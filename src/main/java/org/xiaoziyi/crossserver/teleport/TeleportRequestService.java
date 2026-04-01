package org.xiaoziyi.crossserver.teleport;

import org.bukkit.entity.Player;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class TeleportRequestService {
	public static final String NAMESPACE = "teleport.requests";
	public static final String DATA_KEY = "tpa.requests";

	private final CrossServerApi api;
	private final Logger logger;
	private final int expirySeconds;

	public TeleportRequestService(CrossServerApi api, Logger logger, int expirySeconds) {
		this.api = api;
		this.logger = logger;
		this.expirySeconds = Math.max(5, expirySeconds);
	}

	public boolean submitRequest(Player sender, Player receiver, TpaType type) {
		return submitRequest(
				sender.getUniqueId(),
				sender.getName(),
				receiver.getUniqueId(),
				receiver.getName(),
				sender.getServer().getName(),
				type
		);
	}

	public boolean submitRequest(UUID senderId, String senderName, UUID receiverId, String receiverName, String senderServerId, TpaType type) {
		try {
			Map<String, PendingRequest> requests = new LinkedHashMap<>(loadRequests());
			String requestId = senderId + ":" + receiverId;
			requests.remove(requestId);
			PendingRequest request = new PendingRequest(
					requestId,
					senderId,
					senderName,
					receiverId,
					receiverName,
					type,
					senderServerId,
					Instant.now().plusSeconds(expirySeconds)
			);
			requests.put(requestId, request);
			saveRequests(requests);
			return true;
		} catch (Exception exception) {
			logger.warning("保存 TPA 请求失败: " + exception.getMessage());
			return false;
		}
	}

	public Optional<PendingRequest> consumeRequest(UUID receiverId, UUID senderId) {
		try {
			Map<String, PendingRequest> requests = new LinkedHashMap<>(loadRequests(false));
			String requestId = senderId + ":" + receiverId;
			PendingRequest request = requests.remove(requestId);
			if (request == null) {
				saveRequests(pruneExpired(requests));
				return Optional.empty();
			}
			if (request.expiresAt().isBefore(Instant.now())) {
				saveRequests(pruneExpired(requests));
				return Optional.empty();
			}
			saveRequests(pruneExpired(requests));
			return Optional.of(request);
		} catch (Exception exception) {
			logger.warning("消费 TPA 请求失败: " + exception.getMessage());
			return Optional.empty();
		}
	}

	public Optional<PendingRequest> findLatestRequest(UUID receiverId) {
		try {
			return loadRequests().values().stream()
					.filter(r -> r.receiverId().equals(receiverId))
					.filter(r -> r.expiresAt().isAfter(Instant.now()))
					.max((a, b) -> a.expiresAt().compareTo(b.expiresAt()));
		} catch (Exception exception) {
			logger.warning("查询 TPA 请求失败: " + exception.getMessage());
			return Optional.empty();
		}
	}

	public List<PendingRequest> removeRequestsBySender(UUID senderId) {
		try {
			Map<String, PendingRequest> requests = new LinkedHashMap<>(loadRequests());
			List<PendingRequest> removed = requests.values().stream()
					.filter(request -> request.senderId().equals(senderId))
					.toList();
			requests.entrySet().removeIf(e -> e.getValue().senderId().equals(senderId));
			saveRequests(requests);
			return removed;
		} catch (Exception exception) {
			logger.warning("移除发送方 TPA 请求失败: " + exception.getMessage());
			return List.of();
		}
	}

	public void removeRequestsByReceiver(UUID receiverId) {
		try {
			Map<String, PendingRequest> requests = new LinkedHashMap<>(loadRequests());
			requests.entrySet().removeIf(e -> e.getValue().receiverId().equals(receiverId));
			saveRequests(requests);
		} catch (Exception exception) {
			logger.warning("移除接收方 TPA 请求失败: " + exception.getMessage());
		}
	}

	public RequestStatus getRequestStatus(UUID receiverId, UUID senderId) {
		try {
			PendingRequest request = loadRequests(false).get(senderId + ":" + receiverId);
			if (request == null) {
				return RequestStatus.NONE;
			}
			return request.expiresAt().isBefore(Instant.now()) ? RequestStatus.EXPIRED : RequestStatus.ACTIVE;
		} catch (Exception exception) {
			logger.warning("查询 TPA 请求状态失败: " + exception.getMessage());
			return RequestStatus.NONE;
		}
	}

	public int getExpirySeconds() {
		return expirySeconds;
	}

	private Map<String, PendingRequest> pruneExpired(Map<String, PendingRequest> requests) {
		Map<String, PendingRequest> cleaned = new LinkedHashMap<>(requests);
		Instant now = Instant.now();
		cleaned.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
		return cleaned;
	}

	private Map<String, PendingRequest> loadRequests() throws Exception {
		return loadRequests(true);
	}

	private Map<String, PendingRequest> loadRequests(boolean pruneExpired) throws Exception {
		Optional<GlobalSnapshot> snapshot = api.loadGlobalData(NAMESPACE, DATA_KEY);
		if (snapshot.isEmpty() || snapshot.get().payload() == null || snapshot.get().payload().isBlank()) {
			return Map.of();
		}
		Map<String, PendingRequest> decoded = TeleportRequestCodec.decode(snapshot.get().payload());
		return pruneExpired ? pruneExpired(decoded) : new LinkedHashMap<>(decoded);
	}

	private void saveRequests(Map<String, PendingRequest> requests) throws Exception {
		api.saveGlobalData(NAMESPACE, DATA_KEY, TeleportRequestCodec.encode(Map.copyOf(requests)));
	}

	public enum TpaType {
		TPA,
		TPA_HERE
	}

	public enum RequestStatus {
		ACTIVE,
		EXPIRED,
		NONE
	}

	public record PendingRequest(
			String requestId,
			UUID senderId,
			String senderName,
			UUID receiverId,
			String receiverName,
			TpaType type,
			String senderServerId,
			Instant expiresAt
	) {
	}
}
