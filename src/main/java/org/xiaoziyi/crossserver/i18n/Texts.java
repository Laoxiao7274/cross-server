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
	}
}
