package org.xiaoziyi.crossserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.configcenter.ConfigCenterObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class WebPanelServer implements AutoCloseable {
	private final Logger logger;
	private final PluginConfiguration.WebPanelSettings settings;
	private final WebPanelDataService dataService;
	private final WebPanelLogService logService;
	private final ObjectMapper objectMapper;
	private HttpServer server;
	private ExecutorService executorService;

	public WebPanelServer(Logger logger, PluginConfiguration.WebPanelSettings settings, WebPanelDataService dataService, WebPanelLogService logService) {
		this.logger = logger;
		this.settings = settings;
		this.dataService = dataService;
		this.logService = logService;
		this.objectMapper = ConfigCenterObjectMapper.create();
	}

	public boolean isRunning() {
		return server != null;
	}

	private void logRequest(HttpExchange exchange) {
		if (logService == null) {
			return;
		}
		String remote = exchange.getRemoteAddress() == null ? "unknown" : String.valueOf(exchange.getRemoteAddress());
		logService.captureAccessLog(exchange.getRequestMethod(), exchange.getRequestURI().toString(), remote);
	}

	private String requestSummary(HttpExchange exchange) {
		String remote = exchange.getRemoteAddress() == null ? "unknown" : String.valueOf(exchange.getRemoteAddress());
		return exchange.getRequestMethod() + " " + exchange.getRequestURI() + " from " + remote;
	}

	private boolean isLoopbackHost(String host) {
		if (host == null) {
			return false;
		}
		String normalized = host.trim().toLowerCase(java.util.Locale.ROOT);
		return "127.0.0.1".equals(normalized) || "localhost".equals(normalized) || "::1".equals(normalized);
	}

	public void start() throws IOException {
		if (!settings.enabled() || isRunning()) {
			return;
		}
		server = HttpServer.create(new InetSocketAddress(settings.host(), settings.port()), 0);
		executorService = Executors.newCachedThreadPool();
		server.setExecutor(executorService);
		server.createContext("/", new StaticResourceHandler("/web/panel.html", "text/html; charset=utf-8"));
		server.createContext("/panel.css", new StaticResourceHandler("/web/panel.css", "text/css; charset=utf-8"));
		server.createContext("/panel.js", new StaticResourceHandler("/web/panel.js", "application/javascript; charset=utf-8"));
		server.createContext("/api/status", exchange -> handleJson(exchange, "GET", () -> dataService.loadStatus()));
		server.createContext("/api/modules", new ModulesHandler());
		server.createContext("/api/routes", new RoutesHandler());
		server.createContext("/api/reload", new ReloadHandler());
		server.createContext("/api/config-documents", new ConfigDocumentsHandler());
		server.createContext("/api/transfers/recent", exchange -> handleJson(exchange, "GET", () -> dataService.loadRecentTransfers()));
		server.createContext("/api/logs", exchange -> handleJson(exchange, "GET", () -> dataService.loadLogs()));
		server.createContext("/api/node-configs", exchange -> handleJson(exchange, "GET", () -> dataService.loadNodeConfigs()));
		server.createContext("/api/node-configs/detail", exchange -> handleJson(exchange, "GET", () -> {
			String serverId = extractQueryValue(exchange.getRequestURI().getRawQuery(), "serverId");
			if (serverId == null || serverId.isBlank()) {
				throw new IllegalArgumentException("missing serverId");
			}
			return dataService.loadNodeConfigDetail(serverId);
		}));
		server.createContext("/api/node-configs/apply", exchange -> handleJson(exchange, "POST", () -> dataService.requestNodeConfigApply(readJsonBody(exchange), actorName(exchange))));
		server.createContext("/api/overview", exchange -> handleJson(exchange, "GET", () -> dataService.loadOverview()));
		server.createContext("/api/transfers/player", new PlayerTransferHandler());
		server.start();
		logger.info("内置 Web 配置面板已启动: http://" + settings.host() + ":" + settings.port());
		if (isLoopbackHost(settings.host())) {
			logger.info("Web 面板当前仅监听本机回环地址。若需外部设备访问，请将 web-panel.host 改为 0.0.0.0 或服务器内网 IP。当前: " + settings.host());
		}
	}

	private void handleJson(HttpExchange exchange, String method, ThrowingSupplier<Object> supplier) throws IOException {
		logRequest(exchange);
		try {
			if (!authenticate(exchange)) {
				return;
			}
			if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
				logger.warning("Web 面板方法不匹配: expected=" + method + " actual=" + exchange.getRequestMethod() + " request=" + requestSummary(exchange));
				sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
				return;
			}
			sendJson(exchange, 200, supplier.get());
		} catch (IllegalArgumentException exception) {
			logger.warning("Web 面板请求参数错误: " + requestSummary(exchange) + " -> " + exception.getMessage());
			sendJson(exchange, 400, errorResponse("bad_request", exception.getMessage()));
		} catch (Exception exception) {
			logger.warning("Web 面板请求处理失败: " + requestSummary(exchange) + " -> " + exception.getMessage());
			sendJson(exchange, 500, errorResponse("internal_error", exception.getMessage()));
		}
	}

	private void handleJson(HttpExchange exchange, ThrowingSupplier<Object> supplier) throws IOException {
		handleJson(exchange, "GET", supplier);
	}

	private boolean authenticate(HttpExchange exchange) throws IOException {
		return authenticate(exchange, true);
	}

	private boolean authenticate(HttpExchange exchange, boolean challenge) throws IOException {
		String configuredToken = settings.token();
		if (configuredToken == null || configuredToken.isBlank()) {
			logger.warning("Web 面板拒绝请求: 未配置 token。request=" + requestSummary(exchange));
			sendJson(exchange, 503, Map.of("error", "web_panel_token_missing"));
			return false;
		}
		String provided = exchange.getRequestHeaders().getFirst("X-CrossServer-Token");
		if (provided == null || !configuredToken.equals(provided)) {
			String reason = provided == null ? "missing_header" : "token_mismatch";
			logger.warning("Web 面板鉴权失败: reason=" + reason + " request=" + requestSummary(exchange));
			if (challenge) {
				Headers headers = exchange.getResponseHeaders();
				headers.set("WWW-Authenticate", "X-CrossServer-Token");
				sendJson(exchange, 401, Map.of("error", "unauthorized"));
			}
			return false;
		}
		return true;
	}

	private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
		byte[] bytes = objectMapper.writeValueAsBytes(body);
		Headers headers = exchange.getResponseHeaders();
		headers.set("Content-Type", "application/json; charset=utf-8");
		headers.set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
		try (InputStream inputStream = exchange.getRequestBody()) {
			byte[] body = inputStream.readAllBytes();
			if (body.length == 0) {
				return Map.of();
			}
			Object value = objectMapper.readValue(body, Object.class);
			if (!(value instanceof Map<?, ?> raw)) {
				throw new IllegalArgumentException("请求体必须是 JSON 对象");
			}
			Map<String, Object> result = new java.util.LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : raw.entrySet()) {
				if (!(entry.getKey() instanceof String key)) {
					throw new IllegalArgumentException("请求体的键必须是字符串");
				}
				result.put(key, entry.getValue());
			}
			return result;
		}
	}

	private String actorName(HttpExchange exchange) {
		String actor = exchange.getRequestHeaders().getFirst("X-CrossServer-Actor");
		if (actor == null || actor.isBlank()) {
			return "web-panel";
		}
		return actor.trim();
	}

	private void sendHtml(HttpExchange exchange, String html) throws IOException {
		byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
		Headers headers = exchange.getResponseHeaders();
		headers.set("Content-Type", "text/html; charset=utf-8");
		headers.set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private Map<String, Object> errorResponse(String code, String message) {
		Map<String, Object> response = new java.util.LinkedHashMap<>();
		response.put("error", code == null || code.isBlank() ? "internal_error" : code);
		response.put("message", message == null ? "" : message);
		return response;
	}

	@Override
	public void close() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}
	}

	private final class StaticResourceHandler implements HttpHandler {
		private final String resourcePath;
		private final String contentType;

		StaticResourceHandler(String resourcePath, String contentType) {
			this.resourcePath = resourcePath;
			this.contentType = contentType;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			logRequest(exchange);
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
				return;
			}
			try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
				if (in == null) {
					sendJson(exchange, 404, Map.of("error", "resource_not_found", "path", resourcePath));
					return;
				}
				byte[] bytes = in.readAllBytes();
				Headers headers = exchange.getResponseHeaders();
				headers.set("Content-Type", contentType);
				headers.set("Cache-Control", "no-store");
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			} catch (Exception e) {
				logger.warning("Web 面板静态资源加载失败: " + resourcePath + " -> " + e.getMessage());
				sendJson(exchange, 500, Map.of("error", "resource_load_failed", "message", e.getMessage()));
			}
		}
	}

	private final class ModulesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				handleJson(exchange, "GET", () -> dataService.loadModules());
				return;
			}
			if ("PUT".equalsIgnoreCase(method)) {
				handleJson(exchange, "PUT", () -> dataService.updateModules(readJsonBody(exchange), actorName(exchange)));
				return;
			}
			if ("POST".equalsIgnoreCase(method)) {
				handleJson(exchange, "POST", () -> dataService.rollbackModules(readJsonBody(exchange), actorName(exchange)));
				return;
			}
			sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
		}
	}

	private final class RoutesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				handleJson(exchange, "GET", () -> dataService.loadRoutes());
				return;
			}
			if ("PUT".equalsIgnoreCase(method)) {
				handleJson(exchange, "PUT", () -> dataService.replaceRoutes(readJsonBody(exchange), actorName(exchange)));
				return;
			}
			if ("POST".equalsIgnoreCase(method)) {
				Map<String, Object> body = readJsonBody(exchange);
				if (body.containsKey("version")) {
					handleJson(exchange, "POST", () -> dataService.rollbackRoutes(body, actorName(exchange)));
					return;
				}
				handleJson(exchange, "POST", () -> dataService.upsertRoute(body, actorName(exchange)));
				return;
			}
			if ("DELETE".equalsIgnoreCase(method)) {
				handleJson(exchange, "DELETE", () -> {
					String serverId = extractQueryValue(exchange.getRequestURI().getRawQuery(), "serverId");
					if (serverId == null || serverId.isBlank()) {
						throw new IllegalArgumentException("missing serverId");
					}
					return dataService.deleteRoute(serverId, actorName(exchange));
				});
				return;
			}
			sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
		}
	}

	private final class ConfigDocumentsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				handleJson(exchange, "GET", () -> dataService.loadConfigDocuments());
				return;
			}
			if ("PUT".equalsIgnoreCase(method)) {
				handleJson(exchange, "PUT", () -> dataService.updateConfigDocument(readJsonBody(exchange), actorName(exchange)));
				return;
			}
			if ("POST".equalsIgnoreCase(method)) {
				handleJson(exchange, "POST", () -> dataService.rollbackConfigDocument(readJsonBody(exchange), actorName(exchange)));
				return;
			}
			sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
		}
	}

	private final class ReloadHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if (!"POST".equalsIgnoreCase(method)) {
				sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
				return;
			}
			try {
				if (!authenticate(exchange)) {
					return;
				}
				Map<String, Object> result = dataService.requestReload(readJsonBody(exchange), actorName(exchange));
				int status = Boolean.TRUE.equals(result.get("accepted")) ? 202 : 409;
				sendJson(exchange, status, result);
			} catch (IllegalArgumentException exception) {
				sendJson(exchange, 400, errorResponse("bad_request", exception.getMessage()));
			} catch (Exception exception) {
				logger.warning("Web 面板重载请求处理失败: " + exception.getMessage());
				sendJson(exchange, 500, errorResponse("internal_error", exception.getMessage()));
			}
		}
	}

	private final class PlayerTransferHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try {
				if (!authenticate(exchange)) {
					return;
				}
				if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
					sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
					return;
				}
				URI uri = exchange.getRequestURI();
				String query = uri.getRawQuery();
				String playerName = extractQueryValue(query, "player");
				if (playerName == null || playerName.isBlank()) {
					sendJson(exchange, 400, Map.of("error", "missing_player"));
					return;
				}
				sendJson(exchange, 200, dataService.loadPlayerTransfer(playerName));
			} catch (Exception exception) {
				logger.warning("Web 面板玩家转服查询失败: " + exception.getMessage());
				sendJson(exchange, 500, errorResponse("internal_error", exception.getMessage()));
			}
		}
	}

	private String extractQueryValue(String query, String key) {
		if (query == null || query.isBlank()) {
			return null;
		}
		String[] entries = query.split("&");
		for (String entry : entries) {
			String[] pair = entry.split("=", 2);
			if (pair.length == 2 && key.equalsIgnoreCase(pair[0])) {
				return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	@FunctionalInterface
	private interface ThrowingSupplier<T> {
		T get() throws Exception;
	}
}
