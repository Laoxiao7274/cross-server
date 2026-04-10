package org.xiaoziyi.crossserver.i18n;

import org.xiaoziyi.crossserver.config.PluginConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Texts {
	private final String locale;
	private final Map<String, String> zh = new HashMap<>();
	private final Map<String, String> en = new HashMap<>();

	public Texts(PluginConfiguration.LanguageSettings settings) {
		this.locale = settings == null || settings.locale() == null ? "zh-CN" : settings.locale().trim();
		seed();
	}

	public String tr(String key) {
		Map<String, String> bundle = isEnglish() ? en : zh;
		return bundle.getOrDefault(key, key);
	}

	public String tr(String key, Object... args) {
		return String.format(tr(key), args);
	}

	public boolean isEnglish() {
		return locale.toLowerCase(Locale.ROOT).startsWith("en");
	}

	private void seed() {
		zh.put("plugin.started", "CrossServer 已启动，节点: %s 集群: %s");
		en.put("plugin.started", "CrossServer started. Node: %s Cluster: %s");
		zh.put("plugin.start_failed", "CrossServer 启动失败: %s");
		en.put("plugin.start_failed", "CrossServer startup failed: %s");
		zh.put("plugin.reload_requested", "收到重载请求，来源: %s 操作者: %s");
		en.put("plugin.reload_requested", "Reload request received. Source: %s Actor: %s");
		zh.put("plugin.reload_completed", "CrossServer 重载完成，来源: %s 操作者: %s");
		en.put("plugin.reload_completed", "CrossServer reload completed. Source: %s Actor: %s");
		zh.put("plugin.reload_failed", "CrossServer 重载失败，来源: %s 操作者: %s 错误: %s");
		en.put("plugin.reload_failed", "CrossServer reload failed. Source: %s Actor: %s Error: %s");
		zh.put("plugin.web.disabled", "Web 面板未启用。如需使用，请在 config.yml 中设置 web-panel.enabled: true");
		en.put("plugin.web.disabled", "Web panel is disabled. Set web-panel.enabled: true in config.yml to enable it.");
		zh.put("command.no_permission", "§c你没有权限执行此命令。");
		en.put("command.no_permission", "§cYou do not have permission to use this command.");
		zh.put("command.help.title", "§aCrossServer 命令帮助 §8(第 %s/%s 页)");
		en.put("command.help.title", "§aCrossServer command help §8(page %s/%s)");
		zh.put("command.help.alias", "§7别名: §f/%s §7或 §f/cs");
		en.put("command.help.alias", "§7Aliases: §f/%s §7or §f/cs");
		zh.put("menu.title", "✦ CrossServer 总菜单 ✦");
		en.put("menu.title", "✦ CrossServer Main Menu ✦");
		zh.put("teleport.home.success", "已传送至家园: %s");
		en.put("teleport.home.success", "Teleported to home: %s");
		zh.put("teleport.warp.success", "已传送至地标: %s");
		en.put("teleport.warp.success", "Teleported to warp: %s");
		zh.put("teleport.tpa.success", "已传送至玩家附近: %s");
		en.put("teleport.tpa.success", "Teleported near player: %s");
		zh.put("teleport.tpahere.success", "已完成玩家传送邀请: %s");
		en.put("teleport.tpahere.success", "Completed player teleport invite: %s");
		zh.put("player.only", "§c该命令只能由玩家执行。");
		en.put("player.only", "§cThis command can only be used by players.");
		zh.put("command.no_permission", "§c你没有权限执行此命令。");
		en.put("command.no_permission", "§cYou do not have permission to use this command.");
		zh.put("homes.loading", "§e家园数据加载中，请稍后重试。");
		en.put("homes.loading", "§eHome data is still loading. Please try again later.");
		zh.put("homes.name_invalid", "§c家园名称只能包含字母、数字、下划线和短横线，且长度不能超过 16。");
		en.put("homes.name_invalid", "§cHome names may only contain letters, numbers, underscores, and hyphens, and must be 16 characters or fewer.");
		zh.put("homes.world_missing", "§c无法读取当前位置世界。");
		en.put("homes.world_missing", "§cUnable to read the current world.");
		zh.put("homes.set", "§a已设置家园: §f%s");
		en.put("homes.set", "§aHome set: §f%s");
		zh.put("homes.not_found", "§c未找到家园: %s");
		en.put("homes.not_found", "§cHome not found: %s");
		zh.put("homes.deleted", "§a已删除家园: §f%s");
		en.put("homes.deleted", "§aDeleted home: §f%s");
		zh.put("homes.default_set", "§a已设置默认家园: §f%s");
		en.put("homes.default_set", "§aDefault home set: §f%s");
		zh.put("homes.default_missing", "§c你还没有设置默认家园。");
		en.put("homes.default_missing", "§cYou do not have a default home yet.");
		zh.put("homes.teleport_local", "§a已传送到家园: §f%s");
		en.put("homes.teleport_local", "§aTeleported to home: §f%s");
		zh.put("warp.name_invalid", "§cWarp 名称只能包含字母、数字、下划线和短横线，且长度不能超过 24。");
		en.put("warp.name_invalid", "§cWarp names may only contain letters, numbers, underscores, and hyphens, and must be 24 characters or fewer.");
		zh.put("warp.world_missing", "§c无法读取当前位置世界。");
		en.put("warp.world_missing", "§cUnable to read the current world.");
		zh.put("warp.set", "§a已设置 Warp: §f%s");
		en.put("warp.set", "§aWarp set: §f%s");
		zh.put("warp.set_failed", "§c设置 Warp 失败，请稍后重试。");
		en.put("warp.set_failed", "§cFailed to set warp. Please try again later.");
		zh.put("warp.invalid_name", "§c无效的 Warp 名称。");
		en.put("warp.invalid_name", "§cInvalid warp name.");
		zh.put("warp.not_found", "§c未找到 Warp: %s");
		en.put("warp.not_found", "§cWarp not found: %s");
		zh.put("warp.deleted", "§a已删除 Warp: §f%s");
		en.put("warp.deleted", "§aDeleted warp: §f%s");
		zh.put("warp.delete_failed", "§c删除 Warp 失败，请稍后重试。");
		en.put("warp.delete_failed", "§cFailed to delete warp. Please try again later.");
		zh.put("warp.none", "§e当前还没有可用的 Warp。");
		en.put("warp.none", "§eThere are no available warps yet.");
		zh.put("warp.list", "§e可用 Warp: §f%s");
		en.put("warp.list", "§eAvailable warps: §f%s");
		zh.put("warp.teleport_local", "§a已传送到 Warp: §f%s");
		en.put("warp.teleport_local", "§aTeleported to warp: §f%s");
		zh.put("auth.usage.login", "§e用法: /login <password>");
		en.put("auth.usage.login", "§eUsage: /login <password>");
		zh.put("auth.usage.register", "§e用法: /register <password> <confirm>");
		en.put("auth.usage.register", "§eUsage: /register <password> <confirm>");
		zh.put("auth.usage.change", "§e用法: /changepassword <old> <new>");
		en.put("auth.usage.change", "§eUsage: /changepassword <old> <new>");
		zh.put("tpa.requester_offline", "§c请求发起者当前不在线，无法跨服传送。");
		en.put("tpa.requester_offline", "§cThe request sender is offline and cross-server teleport cannot continue.");
	}
}
