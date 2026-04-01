package org.xiaoziyi.crossserver.auth;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AuthService {
	public static final String PROFILE_NAMESPACE = "auth.profile";
	public static final String TICKET_NAMESPACE = "auth.ticket";
	private static final Duration LOGIN_TIMEOUT = Duration.ofSeconds(45);
	private static final Duration TICKET_TTL = Duration.ofMinutes(5);
	private static final int MAX_FAILURES = 5;

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final String serverId;
	private final SecureRandom secureRandom;
	private final Map<UUID, AuthRuntimeSession> runtimeSessions;

	public AuthService(JavaPlugin plugin, Logger logger, CrossServerApi api, String serverId) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.serverId = serverId;
		this.secureRandom = new SecureRandom();
		this.runtimeSessions = new ConcurrentHashMap<>();
	}

	public void initializePlayer(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<PlayerSnapshot> profileSnapshot = api.loadPlayerData(player.getUniqueId(), PROFILE_NAMESPACE);
				Optional<PlayerSnapshot> ticketSnapshot = api.loadPlayerData(player.getUniqueId(), TICKET_NAMESPACE);
				AuthProfile profile = profileSnapshot.map(snapshot -> AuthCodec.decodeProfile(snapshot.payload())).orElse(null);
				AuthTicket ticket = ticketSnapshot.map(snapshot -> AuthCodec.decodeTicket(snapshot.payload())).orElse(null);
				plugin.getServer().getScheduler().runTask(plugin, () -> applyInitialState(player, profile, ticket));
			} catch (Exception exception) {
				logger.warning("初始化登录状态失败: " + player.getUniqueId() + " -> " + exception.getMessage());
				plugin.getServer().getScheduler().runTask(plugin, () -> player.kick(Component.text("登录模块初始化失败，请稍后重试")));
			}
		});
	}

	public void clearPlayer(UUID playerId) {
		AuthRuntimeSession session = runtimeSessions.remove(playerId);
		if (session == null || session.bossBar == null) {
			return;
		}
		Player player = Bukkit.getPlayer(playerId);
		if (player != null) {
			player.hideBossBar(session.bossBar);
		}
	}

	public boolean isAuthenticated(UUID playerId) {
		AuthRuntimeSession session = runtimeSessions.get(playerId);
		return session == null || session.state == AuthSessionState.AUTHENTICATED;
	}

	public boolean shouldBlock(UUID playerId) {
		AuthRuntimeSession session = runtimeSessions.get(playerId);
		if (session == null || session.state == AuthSessionState.AUTHENTICATED) {
			return false;
		}
		return Instant.now().isBefore(session.expiresAt);
	}

	public boolean isAllowedCommand(String message) {
		String normalized = message.trim().toLowerCase(Locale.ROOT);
		return normalized.startsWith("/login") || normalized.startsWith("/l ") || normalized.equals("/l")
				|| normalized.startsWith("/register") || normalized.startsWith("/reg ") || normalized.equals("/reg")
				|| normalized.startsWith("/changepassword");
	}

	public String getBlockedCommandMessage() {
		return "§c请先完成登录，再执行其他命令。";
	}

	public void issueCrossServerTicket(UUID playerId) {
		AuthRuntimeSession session = runtimeSessions.get(playerId);
		long version = session != null && session.profile != null ? session.profile.loginTicketVersion() : 1L;
		saveTicket(playerId, new AuthTicket(randomToken(), Instant.now(), Instant.now().plus(TICKET_TTL), serverId, version));
	}

	public String register(Player player, String password, String confirmPassword) {
		AuthRuntimeSession session = runtimeSessions.get(player.getUniqueId());
		if (session == null || session.state != AuthSessionState.UNREGISTERED) {
			return "§c你已经注册过账号了。";
		}
		if (!password.equals(confirmPassword)) {
			return "§c两次输入的密码不一致。";
		}
		if (password.length() < 6) {
			return "§c密码至少需要 6 位。";
		}
		String salt = randomToken();
		AuthProfile profile = new AuthProfile(hash(password, salt), salt, Instant.now(), Instant.now(), serverId, 1L);
		saveProfile(player.getUniqueId(), profile);
		markAuthenticated(player, profile, false);
		return "§a注册成功，已自动为你登录。";
	}

	public String login(Player player, String password) {
		AuthRuntimeSession session = runtimeSessions.get(player.getUniqueId());
		if (session == null) {
			return "§c登录状态未初始化，请稍后重试。";
		}
		if (session.state == AuthSessionState.AUTHENTICATED) {
			return "§e你已经登录了。";
		}
		if (session.profile == null) {
			return "§c请先注册账号。";
		}
		String expected = hash(password, session.profile.salt());
		if (!expected.equals(session.profile.passwordHash())) {
			session.failures++;
			showFailureReminder(player, session);
			playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
			if (session.failures >= MAX_FAILURES) {
				player.kick(Component.text("登录失败次数过多，请稍后重试"));
			}
			return "§c密码错误，剩余尝试次数: §f" + Math.max(0, MAX_FAILURES - session.failures);
		}
		AuthProfile updated = new AuthProfile(
				session.profile.passwordHash(),
				session.profile.salt(),
				session.profile.registeredAt(),
				Instant.now(),
				serverId,
				session.profile.loginTicketVersion() + 1
		);
		saveProfile(player.getUniqueId(), updated);
		saveTicket(player.getUniqueId(), new AuthTicket(randomToken(), Instant.now(), Instant.now().plus(TICKET_TTL), serverId, updated.loginTicketVersion()));
		markAuthenticated(player, updated, false);
		return "§a登录成功，祝你游玩愉快。";
	}

	public String changePassword(Player player, String oldPassword, String newPassword) {
		AuthRuntimeSession session = runtimeSessions.get(player.getUniqueId());
		if (session == null || session.profile == null) {
			return "§c请先注册并登录。";
		}
		if (!isAuthenticated(player.getUniqueId())) {
			return "§c请先登录后再修改密码。";
		}
		if (!hash(oldPassword, session.profile.salt()).equals(session.profile.passwordHash())) {
			return "§c旧密码错误。";
		}
		if (newPassword.length() < 6) {
			return "§c新密码至少需要 6 位。";
		}
		String salt = randomToken();
		AuthProfile updated = new AuthProfile(hash(newPassword, salt), salt, session.profile.registeredAt(), Instant.now(), serverId, session.profile.loginTicketVersion() + 1);
		session.profile = updated;
		saveProfile(player.getUniqueId(), updated);
		saveTicket(player.getUniqueId(), new AuthTicket(randomToken(), Instant.now(), Instant.now().plus(TICKET_TTL), serverId, updated.loginTicketVersion()));
		player.sendActionBar(Component.text("密码修改成功，请妥善保管你的新密码。"));
		playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.15F);
		return "§a密码修改成功。";
	}

	public AuthAdminInspection inspectAuth(UUID playerId) throws Exception {
		Optional<PlayerSnapshot> profileSnapshot = api.loadPlayerData(playerId, PROFILE_NAMESPACE);
		Optional<PlayerSnapshot> ticketSnapshot = api.loadPlayerData(playerId, TICKET_NAMESPACE);
		AuthProfile profile = profileSnapshot.map(snapshot -> AuthCodec.decodeProfile(snapshot.payload())).orElse(null);
		AuthTicket ticket = ticketSnapshot.map(snapshot -> AuthCodec.decodeTicket(snapshot.payload())).orElse(null);
		AuthRuntimeSession runtime = runtimeSessions.get(playerId);
		return new AuthAdminInspection(playerId, profile, ticket, runtime != null ? runtime.state : null, runtime != null ? runtime.failures : 0);
	}

	public String invalidateTickets(UUID playerId, String actorName) throws Exception {
		AuthAdminInspection inspection = inspectAuth(playerId);
		AuthProfile profile = inspection.profile();
		if (profile == null) {
			return "§e该玩家尚未注册。";
		}
		AuthProfile updated = new AuthProfile(profile.passwordHash(), profile.salt(), profile.registeredAt(), profile.lastLoginAt(), profile.lastLoginServer(), profile.loginTicketVersion() + 1);
		saveProfileSync(playerId, updated);
		saveTicketSync(playerId, new AuthTicket(randomToken(), Instant.now(), Instant.now().minusSeconds(1), actorName, updated.loginTicketVersion()));
		return "§a已作废该玩家现有登录票据。";
	}

	public String forceReauthenticate(UUID playerId, String actorName) throws Exception {
		String result = invalidateTickets(playerId, actorName);
		AuthRuntimeSession session = runtimeSessions.get(playerId);
		if (session != null) {
			session.state = AuthSessionState.PENDING_LOGIN;
			session.failures = 0;
			session.expiresAt = Instant.now().plus(LOGIN_TIMEOUT);
			Player player = Bukkit.getPlayer(playerId);
			if (player != null && player.isOnline()) {
				enterState(player, session, AuthSessionState.PENDING_LOGIN);
			}
		}
		return result;
	}

	public void handleChatInput(Player player, String message) {
		AuthRuntimeSession session = runtimeSessions.get(player.getUniqueId());
		if (session == null || session.state == AuthSessionState.AUTHENTICATED) {
			return;
		}
		String response = session.state == AuthSessionState.UNREGISTERED
				? "§e请使用 /register <密码> <确认密码> 完成注册。"
				: login(player, message.trim());
		player.sendMessage(response);
	}

	public void tickPlayer(Player player) {
		AuthRuntimeSession session = runtimeSessions.get(player.getUniqueId());
		if (session == null || session.state == AuthSessionState.AUTHENTICATED) {
			return;
		}
		if (Instant.now().isAfter(session.expiresAt)) {
			player.kick(Component.text("登录超时，请重新进入服务器"));
			return;
		}
		refreshTimedUi(player, session);
	}

	private void applyInitialState(Player player, AuthProfile profile, AuthTicket ticket) {
		AuthRuntimeSession session = new AuthRuntimeSession();
		session.profile = profile;
		session.expiresAt = Instant.now().plus(LOGIN_TIMEOUT);
		session.bossBar = BossBar.bossBar(Component.text("身份验证中"), 1.0F, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
		runtimeSessions.put(player.getUniqueId(), session);
		player.showBossBar(session.bossBar);
		if (profile == null) {
			enterState(player, session, AuthSessionState.UNREGISTERED);
			playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7F, 1.2F);
			return;
		}
		if (ticket != null && ticket.expiresAt().isAfter(Instant.now()) && ticket.version() >= profile.loginTicketVersion()) {
			markAuthenticated(player, profile, true);
			return;
		}
		enterState(player, session, AuthSessionState.PENDING_LOGIN);
		playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7F, 0.9F);
	}

	private void enterState(Player player, AuthRuntimeSession session, AuthSessionState state) {
		session.state = state;
		session.lastPromptState = state;
		session.failures = 0;
		sendEntryTitle(player, state);
		refreshTimedUi(player, session);
	}

	private void markAuthenticated(Player player, AuthProfile profile, boolean autoLogin) {
		AuthRuntimeSession session = runtimeSessions.computeIfAbsent(player.getUniqueId(), ignored -> new AuthRuntimeSession());
		session.profile = profile;
		session.state = AuthSessionState.AUTHENTICATED;
		session.failures = 0;
		session.lastPromptState = AuthSessionState.AUTHENTICATED;
		if (session.bossBar != null) {
			player.hideBossBar(session.bossBar);
		}
		if (autoLogin) {
			player.sendMessage("§a已通过跨服免重登验证。§7你可以直接开始游戏。");
			player.sendActionBar(Component.text("跨服免重登成功，欢迎回来。"));
			playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.35F);
			return;
		}
		player.sendTitle("§a登录成功", "§7祝你游玩愉快", 10, 50, 10);
		player.sendActionBar(Component.text("认证完成，已解除登录限制。"));
		playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.75F, 1.1F);
	}

	private void refreshTimedUi(Player player, AuthRuntimeSession session) {
		long remainingSeconds = Math.max(1L, Duration.between(Instant.now(), session.expiresAt).toSeconds());
		float progress = Math.max(0.0F, Math.min(1.0F, remainingSeconds / (float) LOGIN_TIMEOUT.toSeconds()));
		if (session.bossBar != null) {
			session.bossBar.progress(progress);
			session.bossBar.name(Component.text(formatBossBarText(session.state, remainingSeconds)));
		}
		if (session.lastActionBarSecond != remainingSeconds) {
			player.sendActionBar(Component.text(formatActionBarText(session, remainingSeconds)));
			session.lastActionBarSecond = remainingSeconds;
		}
	}

	private void sendEntryTitle(Player player, AuthSessionState state) {
		if (state == AuthSessionState.UNREGISTERED) {
			player.sendTitle("§b欢迎来到晴川空岛", "§f请先注册你的账号", 10, 50, 10);
			return;
		}
		if (state == AuthSessionState.PENDING_LOGIN) {
			player.sendTitle("§e身份验证", "§f请输入密码完成登录", 10, 50, 10);
		}
	}

	private void showFailureReminder(Player player, AuthRuntimeSession session) {
		long remainingAttempts = Math.max(0, MAX_FAILURES - session.failures);
		player.sendActionBar(Component.text("密码错误，还可尝试 " + remainingAttempts + " 次。"));
	}

	private String formatBossBarText(AuthSessionState state, long remainingSeconds) {
		if (state == AuthSessionState.UNREGISTERED) {
			return "注册剩余 " + remainingSeconds + " 秒";
		}
		return "登录剩余 " + remainingSeconds + " 秒";
	}

	private String formatActionBarText(AuthRuntimeSession session, long remainingSeconds) {
		if (session.state == AuthSessionState.UNREGISTERED) {
			return "请使用 /register <密码> <确认密码> 完成注册 · 剩余 " + remainingSeconds + " 秒";
		}
		long remainingAttempts = Math.max(0, MAX_FAILURES - session.failures);
		return "可直接在聊天栏输入密码，或使用 /login <密码> · 剩余 " + remainingSeconds + " 秒 · 尝试 " + remainingAttempts + "/" + MAX_FAILURES;
	}

	private void playSound(Player player, Sound sound, float volume, float pitch) {
		player.playSound(player.getLocation(), sound, volume, pitch);
	}

	private void saveProfile(UUID playerId, AuthProfile profile) {
		String payload = AuthCodec.encodeProfile(profile);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(playerId, PROFILE_NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存登录资料失败: " + playerId + " -> " + exception.getMessage());
			}
		});
	}

	private void saveTicket(UUID playerId, AuthTicket ticket) {
		String payload = AuthCodec.encodeTicket(ticket);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(playerId, TICKET_NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存登录票据失败: " + playerId + " -> " + exception.getMessage());
			}
		});
	}

	private void saveProfileSync(UUID playerId, AuthProfile profile) throws Exception {
		api.savePlayerData(playerId, PROFILE_NAMESPACE, AuthCodec.encodeProfile(profile));
	}

	private void saveTicketSync(UUID playerId, AuthTicket ticket) throws Exception {
		api.savePlayerData(playerId, TICKET_NAMESPACE, AuthCodec.encodeTicket(ticket));
	}

	private String randomToken() {
		byte[] bytes = new byte[16];
		secureRandom.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private String hash(String input, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] result = digest.digest((salt + ":" + input).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(result);
		} catch (Exception exception) {
			throw new IllegalStateException("无法计算密码哈希", exception);
		}
	}

	public record AuthAdminInspection(UUID playerId, AuthProfile profile, AuthTicket ticket, AuthSessionState runtimeState, int runtimeFailures) {
	}

	private static final class AuthRuntimeSession {
		private AuthProfile profile;
		private AuthSessionState state;
		private AuthSessionState lastPromptState;
		private Instant expiresAt;
		private int failures;
		private long lastActionBarSecond = Long.MIN_VALUE;
		private BossBar bossBar;
	}
}
