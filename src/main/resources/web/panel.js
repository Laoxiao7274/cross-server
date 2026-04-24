/**
 * CrossServer Web Panel - JavaScript
 * High-contrast minimal dark theme
 */

// ─── Constants ───────────────────────────────────────────────────────────────

const MODULE_KEYS = ["auth", "homes", "warps", "tpa", "routeConfig", "transferAdmin", "economyBridge", "permissions"];

const BOOLEAN_OPTIONS = [
  { value: "true", label: "开启" },
  { value: "false", label: "关闭" }
];

// ─── i18n ────────────────────────────────────────────────────────────────────

const I18N = {
  "zh-CN": {
    brandTitle: "CrossServer",
    brandSubtitle: "运维面板",
    navDashboard: "仪表盘",
    navModules: "模块",
    navRoutes: "路由",
    navDocuments: "配置文档",
    navLogs: "日志中心",
    navNodeConfigs: "节点配置",
    navTransfers: "转服诊断",
    saveAuth: "保存凭证",
    refresh: "刷新概览",
    reloadNode: "重载节点",
    reloadData: "重新读取",
    searchPlayer: "查询玩家",
    saveRoute: "保存路由",
    replaceRoutes: "整体覆盖",
    loadCurrentRoutes: "填入当前路由",
    confirmTitle: "操作确认",
    confirmBody: "请确认本次运维操作的影响范围。",
    confirmContinue: "确认继续",
    dashboardTitle: "仪表盘",
    dashboardDesc: "集群状态、节点心跳、会话统计与面板成员",
    modulesTitle: "共享模块",
    modulesDesc: "查看本地 / 共享 / 生效三层状态，管理共享覆盖",
    routesTitle: "共享路由",
    routesDesc: "管理跨服路由映射表",
    documentsTitle: "配置文档",
    documentsDesc: "查看并编辑 JSON / YAML 配置文档",
    logsTitle: "日志中心",
    logsDesc: "按节点查看最近同步的插件日志",
    nodeConfigsTitle: "节点配置",
    nodeConfigsDesc: "查看节点配置快照与应用结果",
    nodeConfigDetailTitle: "节点详情",
    nodeConfigDetailDesc: "编辑白名单字段并提交到目标节点",
    transfersTitle: "转服诊断",
    transfersDesc: "查看近期转服记录并按玩家名诊断",
    placeholderNodes: "搜索节点 ID / 集群 / 状态",
    placeholderRoutes: "搜索服务器 ID / 路由目标 / 来源",
    placeholderDocs: "搜索 namespace / dataKey / 标题",
    placeholderLogsServer: "筛选节点 serverId",
    placeholderLogsKeyword: "筛选日志关键字 / 级别",
    placeholderNodeConfigs: "搜索节点 ID / 状态 / 应用结果",
    placeholderTransferPlayer: "输入玩家名查询转服诊断",
    placeholderServerId: "服务器 ID",
    placeholderProxyTarget: "代理目标",
    placeholderRoutesJson: '{"routes":{"spawn":"lobby"}}',
    authSavedOk: "凭证已保存，概览加载成功。",
    overviewReloaded: "概览已刷新。",
    logsReloaded: "日志中心已刷新。",
    modulesReloaded: "模块数据已刷新。",
    routesReloaded: "路由数据已刷新。",
    nodeConfigsReloaded: "节点配置已刷新。",
    routeSaved: "已保存共享路由：%s → %s",
    routesReplaced: "已整体覆盖共享路由表。",
    authNeedToken: "请输入 Web 面板 token 后再加载数据。",
    authLoadedFromSaved: "已使用本地保存的凭证加载面板。",
    thNode: "节点",
    thStatus: "状态",
    thLastHeartbeat: "最后心跳",
    thLatency: "耗时",
    thModule: "模块",
    thModuleSummary: "状态概览",
    thModuleControl: "共享覆盖",
    thServerId: "服务器 ID",
    thLocalTarget: "本地目标",
    thSharedTarget: "共享目标",
    thEffective: "生效值",
    thSource: "来源",
    thAction: "操作",
    thOnlineStatus: "在线状态",
    thSnapshot: "配置快照",
    thLatestApply: "最近应用",
    thTime: "时间",
    thPlayer: "玩家",
    thTarget: "目标",
    thEvent: "事件",
    thLevel: "级别",
    thContent: "内容",
    emptyPanelMembers: "暂无已登记的运维面板成员",
    emptyNodes: "暂无节点心跳记录",
    emptyRoutes: "暂无路由数据",
    emptyDocuments: "暂无已注册配置文档。",
    emptyTransferHistory: "暂无近期 transfer 历史",
    emptyLogs: "暂无已同步日志",
    emptyLogEntries: "暂无同步日志记录",
    emptyNodeConfigs: "暂无节点配置数据",
    emptyModuleHistory: "暂无模块变更历史。",
    emptyRouteHistory: "暂无路由变更历史。",
    emptyDocHistory: "暂无历史记录。",
    historyModules: "查看模块变更 (%s)",
    historyRoutes: "查看路由变更 (%s)",
    historyDocuments: "查看文档历史 (%s)",
    historyNodeApply: "查看应用历史 (%s)",
    viewDetails: "查看详情",
    rollback: "回滚",
    collapse: "收起",
    expand: "展开",
    currentNode: "当前节点",
    clusterNode: "集群节点",
    localOnly: "本地：",
    sharedOnly: "共享：",
    effectiveState: "生效：",
    forceEnable: "强制开启",
    forceDisable: "强制关闭",
    followLocal: "跟随本地",
    noDescription: "暂无说明",
    noSummary: "暂无摘要",
    noApplyRecord: "暂无应用记录",
    nodeSelectIntro: "请选择一个节点查看详情并编辑白名单字段。",
    transferNeedPlayer: "请输入玩家名",
    requestFailed: "请求失败%s：%s"
  },
  "en-US": {
    brandTitle: "CrossServer",
    brandSubtitle: "Operations",
    navDashboard: "Dashboard",
    navModules: "Modules",
    navRoutes: "Routes",
    navDocuments: "Documents",
    navLogs: "Logs",
    navNodeConfigs: "Node Configs",
    navTransfers: "Transfers",
    saveAuth: "Save Auth",
    refresh: "Refresh",
    reloadNode: "Reload Node",
    reloadData: "Reload",
    searchPlayer: "Search Player",
    saveRoute: "Save Route",
    replaceRoutes: "Replace All",
    loadCurrentRoutes: "Load Current",
    confirmTitle: "Confirm Action",
    confirmBody: "Please confirm the impact of this operation.",
    confirmContinue: "Continue",
    dashboardTitle: "Dashboard",
    dashboardDesc: "Cluster status, node heartbeats, sessions, and panel members",
    modulesTitle: "Shared Modules",
    modulesDesc: "View local / shared / effective states and manage overrides",
    routesTitle: "Shared Routes",
    routesDesc: "Manage cross-server route mappings",
    documentsTitle: "Config Documents",
    documentsDesc: "View and edit JSON / YAML configuration documents",
    logsTitle: "Log Center",
    logsDesc: "View recent plugin logs synced by node",
    nodeConfigsTitle: "Node Configs",
    nodeConfigsDesc: "View node snapshots and apply results",
    nodeConfigDetailTitle: "Node Detail",
    nodeConfigDetailDesc: "Edit whitelist fields and submit to target node",
    transfersTitle: "Transfer Diagnostics",
    transfersDesc: "Recent transfers and per-player diagnostics",
    placeholderNodes: "Search node ID / cluster / status",
    placeholderRoutes: "Search server ID / target / source",
    placeholderDocs: "Search namespace / dataKey / title",
    placeholderLogsServer: "Filter by serverId",
    placeholderLogsKeyword: "Filter by keyword / level",
    placeholderNodeConfigs: "Search node ID / status / result",
    placeholderTransferPlayer: "Enter player name",
    placeholderServerId: "Server ID",
    placeholderProxyTarget: "Proxy target",
    placeholderRoutesJson: '{"routes":{"spawn":"lobby"}}',
    authSavedOk: "Credentials saved. Overview loaded.",
    overviewReloaded: "Overview refreshed.",
    logsReloaded: "Logs refreshed.",
    modulesReloaded: "Modules refreshed.",
    routesReloaded: "Routes refreshed.",
    nodeConfigsReloaded: "Node configs refreshed.",
    routeSaved: "Saved route: %s → %s",
    routesReplaced: "Route table replaced.",
    authNeedToken: "Enter token before loading data.",
    authLoadedFromSaved: "Loaded with saved credentials.",
    thNode: "Node",
    thStatus: "Status",
    thLastHeartbeat: "Last Heartbeat",
    thLatency: "Latency",
    thModule: "Module",
    thModuleSummary: "Summary",
    thModuleControl: "Override",
    thServerId: "Server ID",
    thLocalTarget: "Local Target",
    thSharedTarget: "Shared Target",
    thEffective: "Effective",
    thSource: "Source",
    thAction: "Action",
    thOnlineStatus: "Status",
    thSnapshot: "Snapshot",
    thLatestApply: "Latest Apply",
    thTime: "Time",
    thPlayer: "Player",
    thTarget: "Target",
    thEvent: "Event",
    thLevel: "Level",
    thContent: "Content",
    emptyPanelMembers: "No registered panel members.",
    emptyNodes: "No node heartbeat records.",
    emptyRoutes: "No route data.",
    emptyDocuments: "No registered config documents.",
    emptyTransferHistory: "No recent transfers.",
    emptyLogs: "No synced logs.",
    emptyLogEntries: "No log entries.",
    emptyNodeConfigs: "No node config data.",
    emptyModuleHistory: "No module change history.",
    emptyRouteHistory: "No route change history.",
    emptyDocHistory: "No history yet.",
    historyModules: "Module changes (%s)",
    historyRoutes: "Route changes (%s)",
    historyDocuments: "Document history (%s)",
    historyNodeApply: "Apply history (%s)",
    viewDetails: "Details",
    rollback: "Rollback",
    collapse: "Collapse",
    expand: "Expand",
    currentNode: "Current node",
    clusterNode: "Cluster node",
    localOnly: "Local: ",
    sharedOnly: "Shared: ",
    effectiveState: "Effective: ",
    forceEnable: "Force On",
    forceDisable: "Force Off",
    followLocal: "Follow Local",
    noDescription: "No description",
    noSummary: "No summary",
    noApplyRecord: "No apply records",
    nodeSelectIntro: "Select a node to view details.",
    transferNeedPlayer: "Enter a player name",
    requestFailed: "Request failed%s: %s"
  }
};

// ─── State ───────────────────────────────────────────────────────────────────

const state = {
  overview: null,
  selectedNodeConfigServerId: null,
  nodeConfigDetail: null,
  confirmResolve: null,
  locale: "zh-CN",
  filters: {
    dashboardNodes: "",
    routes: "",
    documents: "",
    logsServer: "",
    logsKeyword: "",
    nodeConfigs: ""
  }
};

// ─── DOM refs ────────────────────────────────────────────────────────────────

const tokenInput = document.getElementById("tokenInput");
const actorInput = document.getElementById("actorInput");
const localeSelect = document.getElementById("localeSelect");
const authMessage = document.getElementById("authMessage");
const navButtons = Array.from(document.querySelectorAll(".nav-item[data-tab]"));
const confirmModal = document.getElementById("confirmModal");
const confirmModalTitle = document.getElementById("confirmModalTitle");
const confirmModalBody = document.getElementById("confirmModalBody");
const confirmCancelBtn = document.getElementById("confirmCancelBtn");
const confirmAcceptBtn = document.getElementById("confirmAcceptBtn");

// ─── Helpers ─────────────────────────────────────────────────────────────────

function savedToken() { return localStorage.getItem("crossserver.web.token") || ""; }
function savedActor() { return localStorage.getItem("crossserver.web.actor") || "web-panel"; }

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function boolText(value) {
  if (value === null || value === undefined) return state.locale === "zh-CN" ? "未设置" : "unset";
  return value
    ? (state.locale === "zh-CN" ? "开启" : "On")
    : (state.locale === "zh-CN" ? "关闭" : "Off");
}

function moduleName(key) {
  const labels = {
    auth: "登录认证",
    homes: "家园",
    warps: "地标传送",
    tpa: "玩家传送",
    routeConfig: "路由配置",
    transferAdmin: "转服诊断",
    economyBridge: "经济桥接",
    permissions: "权限同步"
  };
  return labels[key] || key;
}

function formatPayloadType(type) {
  const labels = {
    "json-object": "JSON 对象",
    "json-array": "JSON 数组",
    "json-value": "JSON 值",
    "yaml-object": "YAML 对象",
    "yaml-array": "YAML 数组",
    "yaml-value": "YAML 值",
    text: "纯文本",
    empty: "空内容"
  };
  return labels[type] || type;
}

function formatTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString(state.locale, { hour12: false });
}

function includesFilter(value, keyword) {
  const source = String(value ?? "").toLowerCase();
  const search = String(keyword ?? "").trim().toLowerCase();
  return !search || source.includes(search);
}

function t(key) {
  const bundle = I18N[state.locale] || I18N["zh-CN"];
  return bundle[key] || key;
}

function setMessage(el, type, text) {
  el.className = `message show ${type}`;
  el.textContent = text;
}

function clearMessage(el) {
  el.className = "message";
  el.textContent = "";
}

// ─── Tabs ────────────────────────────────────────────────────────────────────

const TAB_IDS = ["dashboard", "modules", "routes", "documents", "logs", "node-configs", "transfers"];

function resolveTab(tab) {
  return TAB_IDS.includes(tab) ? tab : "dashboard";
}

function switchTab(tab, syncHash = true) {
  const resolved = resolveTab(tab);
  navButtons.forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === resolved);
  });
  document.querySelectorAll(".tab-page").forEach(page => {
    page.classList.toggle("active", page.id === `tab-${resolved}`);
  });
  if (syncHash && window.location.hash !== `#${resolved}`) {
    window.location.hash = resolved;
  }
}

navButtons.forEach(btn => {
  btn.addEventListener("click", () => switchTab(btn.dataset.tab));
});

window.addEventListener("hashchange", () => {
  switchTab(window.location.hash.replace("#", ""), false);
});

// ─── Locale ──────────────────────────────────────────────────────────────────

function applyLocale() {
  document.querySelector(".brand .brand-text").textContent = t("brandTitle");
  document.querySelector(".brand small").textContent = t("brandSubtitle");
  navButtons.forEach((btn, i) => {
    const keys = ["navDashboard","navModules","navRoutes","navDocuments","navLogs","navNodeConfigs","navTransfers"];
    btn.querySelector("span").textContent = t(keys[i]);
  });
  document.getElementById("saveAuthBtn").textContent = t("saveAuth");
  document.getElementById("refreshBtn").textContent = t("refresh");
  document.getElementById("reloadBtn").textContent = t("reloadNode");
  document.getElementById("reloadModulesBtn").textContent = t("reloadData");
  document.getElementById("reloadRoutesBtn").textContent = t("reloadData");
  document.getElementById("reloadLogsBtn").textContent = t("reloadData");
  document.getElementById("reloadNodeConfigsBtn").textContent = t("reloadData");
  document.getElementById("saveRouteBtn").textContent = t("saveRoute");
  document.getElementById("replaceRoutesBtn").textContent = t("replaceRoutes");
  document.getElementById("loadRoutesJsonBtn").textContent = t("loadCurrentRoutes");
  document.getElementById("playerSearchBtn").textContent = t("searchPlayer");
  document.getElementById("dashboardNodesSearch").placeholder = t("placeholderNodes");
  document.getElementById("routesSearchInput").placeholder = t("placeholderRoutes");
  document.getElementById("documentsSearchInput").placeholder = t("placeholderDocs");
  document.getElementById("logsServerSearch").placeholder = t("placeholderLogsServer");
  document.getElementById("logsKeywordSearch").placeholder = t("placeholderLogsKeyword");
  document.getElementById("nodeConfigsSearchInput").placeholder = t("placeholderNodeConfigs");
  document.getElementById("playerSearchInput").placeholder = t("placeholderTransferPlayer");
  document.getElementById("routeServerIdInput").placeholder = t("placeholderServerId");
  document.getElementById("routeProxyTargetInput").placeholder = t("placeholderProxyTarget");
  document.getElementById("routesBulkInput").placeholder = t("placeholderRoutesJson");

  const headings = document.querySelectorAll(".tab-page .panel-header h2");
  const descs = [
    "dashboardDesc","modulesDesc","routesDesc","documentsDesc",
    "logsDesc","nodeConfigsDesc","nodeConfigDetailDesc","transfersDesc"
  ];
  const titles = [
    "dashboardTitle","modulesTitle","routesTitle","documentsTitle",
    "logsTitle","nodeConfigsTitle","nodeConfigDetailTitle","transfersTitle"
  ];
  headings.forEach((h, i) => { if (h) { h.textContent = t(titles[i]); } });
  document.querySelectorAll(".tab-page .panel-header .subtitle").forEach((s, i) => {
    if (s) s.textContent = t(descs[i]);
  });

  confirmModalTitle.textContent = t("confirmTitle");
  if (!confirmModalBody.textContent || confirmModalBody.textContent === I18N["zh-CN"].confirmBody || confirmModalBody.textContent === I18N["en-US"].confirmBody) {
    confirmModalBody.textContent = t("confirmBody");
  }
  if (confirmAcceptBtn.textContent === I18N["zh-CN"].confirmContinue || confirmAcceptBtn.textContent === I18N["en-US"].confirmContinue) {
    confirmAcceptBtn.textContent = t("confirmContinue");
  }
}

localeSelect?.addEventListener("change", e => {
  state.locale = e.target.value || "zh-CN";
  localStorage.setItem("crossserver.locale", state.locale);
  applyLocale();
});

// ─── Confirm modal ───────────────────────────────────────────────────────────

function askConfirm(title, body, confirmLabel) {
  return new Promise(resolve => {
    state.confirmResolve = resolve;
    confirmModalTitle.textContent = title || t("confirmTitle");
    confirmModalBody.textContent = body || t("confirmBody");
    confirmAcceptBtn.textContent = confirmLabel || t("confirmContinue");
    confirmModal.classList.add("show");
    confirmModal.setAttribute("aria-hidden", "false");
  });
}

function closeConfirm(result) {
  confirmModal.classList.remove("show");
  confirmModal.setAttribute("aria-hidden", "true");
  const resolver = state.confirmResolve;
  state.confirmResolve = null;
  if (resolver) resolver(!!result);
}

confirmCancelBtn.addEventListener("click", () => closeConfirm(false));
confirmAcceptBtn.addEventListener("click", () => closeConfirm(true));
confirmModal.addEventListener("click", e => {
  if (e.target === confirmModal) closeConfirm(false);
});

function diffText(fromValue, toValue) {
  const fromText = typeof fromValue === "string" ? fromValue : JSON.stringify(fromValue ?? null, null, 2);
  const toText = typeof toValue === "string" ? toValue : JSON.stringify(toValue ?? null, null, 2);
  return `${state.locale === "zh-CN" ? "旧值" : "Old"}:\n${fromText || "-"}\n\n${state.locale === "zh-CN" ? "新值" : "New"}:\n${toText || "-"}`;
}

// ─── API ─────────────────────────────────────────────────────────────────────

async function api(path, options = {}) {
  const token = tokenInput.value.trim();
  if (!token) throw new Error(state.locale === "zh-CN" ? "请先输入访问令牌" : "Enter token first");
  clearMessage(authMessage);
  const headers = new Headers(options.headers || {});
  headers.set("X-CrossServer-Token", token);
  headers.set("X-CrossServer-Actor", (actorInput.value || "web-panel").trim() || "web-panel");
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch (_) { data = text; }
  if (!response.ok) {
    const message = data && typeof data === "object" && data.message ? data.message : (data && data.error ? data.error : response.statusText);
    const error = new Error(message || `HTTP ${response.status}`);
    error.code = data && typeof data === "object" ? data.error : undefined;
    error.status = response.status;
    throw error;
  }
  return data;
}

function reportGlobalError(error) {
  const box = document.getElementById("globalMessage");
  if (!box) return;
  const status = error && error.status ? ` (HTTP ${error.status})` : "";
  setMessage(box, "error", t("requestFailed").replace("%s", status).replace("%s", error.message || "unknown error"));
}

async function waitForPanelRecovery() {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 60000) {
    try {
      await loadOverview();
      setMessage(authMessage, "success", state.locale === "zh-CN" ? "CrossServer 已重新上线，面板数据已恢复。" : "CrossServer back online.");
      return;
    } catch (_) {
      await new Promise(r => setTimeout(r, 2500));
    }
  }
  setMessage(authMessage, "error", state.locale === "zh-CN" ? "等待重载恢复超时" : "Reload recovery timed out");
}

// ─── Render helpers ──────────────────────────────────────────────────────────

function kvCard(items, accent = false) {
  const cls = accent ? "info-card accent" : "info-card";
  const rows = items.map(([label, value]) =>
    `<div class="kv-row"><span class="kv-label">${escapeHtml(label)}</span><span class="kv-value">${escapeHtml(value ?? "-")}</span></div>`
  ).join("");
  return `<div class="${cls}">${rows}</div>`;
}

function inputHtml(type, name, value, placeholder = "", extra = "") {
  return `<input type="${escapeHtml(type)}" name="${escapeHtml(name)}" value="${escapeHtml(value ?? "")}" placeholder="${escapeHtml(placeholder)}"${extra ? ` ${extra}` : ""}>`;
}

function booleanSelectHtml(name, value) {
  const normalized = value === true ? "true" : value === false ? "false" : "";
  return `<select name="${escapeHtml(name)}">${BOOLEAN_OPTIONS.map(o => `<option value="${escapeHtml(o.value)}"${normalized === o.value ? " selected" : ""}>${escapeHtml(o.label)}</option>`).join("")}</select>`;
}

function fieldGroup(label, control, hint = "") {
  return `<div class="field-group"><label>${escapeHtml(label)}</label>${control}${hint ? `<div class="hint">${escapeHtml(hint)}</div>` : ""}</div>`;
}

function applyStatusText(status) {
  if (!status) return state.locale === "zh-CN" ? "未提交" : "Not submitted";
  const labels = state.locale === "zh-CN"
    ? { pending: "等待应用", applying: "正在写入", applied: "已应用", failed: "应用失败" }
    : { pending: "Pending", applying: "Applying", applied: "Applied", failed: "Failed" };
  return labels[String(status).toLowerCase()] || String(status);
}

function applyStatusClass(status) {
  const n = String(status || "").toLowerCase();
  if (n === "applied") return "success";
  if (n === "failed") return "danger";
  return "";
}

function nodeStatusText(ns) {
  const n = String(ns?.status || "").toLowerCase();
  if (n === "online") return state.locale === "zh-CN" ? "在线" : "Online";
  if (n === "offline") return state.locale === "zh-CN" ? "离线" : "Offline";
  return state.locale === "zh-CN" ? "未知" : "Unknown";
}

function nodeStatusClass(ns) {
  const n = String(ns?.status || "").toLowerCase();
  if (n === "online") return "success";
  if (n === "offline") return "danger";
  return "";
}

function sameServerId(a, b) {
  return String(a || "").toLowerCase() === String(b || "").toLowerCase();
}

// ─── Render: Dashboard ───────────────────────────────────────────────────────

function renderHero(status) {
  const stats = [
    ["当前节点", status.serverId || "-"],
    ["集群", status.cluster || "-"],
    ["本地会话", status.localSessionCount ?? 0],
    ["预备传送", status.preparedTransferCount ?? 0],
    ["待处理失效", status.pendingInvalidationCount ?? 0],
    ["Redis", status.redisEnabled ? (state.locale === "zh-CN" ? "已启用" : "Enabled") : (state.locale === "zh-CN" ? "未启用" : "Disabled")]
  ];
  document.getElementById("heroStats").innerHTML = stats.map(([label, value]) =>
    `<div class="stat-card"><div class="stat-label">${escapeHtml(label)}</div><div class="stat-value">${escapeHtml(value)}</div></div>`
  ).join("");
}

function renderStatus(status) {
  renderHero(status);

  const summary = [
    ["命名空间数量", status.namespaceCount],
    ["远端会话感知", status.remoteSessionCount],
    ["传送网关类型", status.teleportGatewayType],
    ["Handoff 有效秒数", status.handoffSeconds]
  ];
  document.getElementById("statusSummary").innerHTML = summary.map(([label, value]) =>
    `<div class="stat-card"><div class="stat-label">${escapeHtml(label)}</div><div class="stat-value">${escapeHtml(value ?? "-")}</div></div>`
  ).join("");

  const cluster = status.webPanelCluster || {};
  const localServerId = cluster.localServerId || status.serverId || "-";
  const members = Array.isArray(cluster.members) ? [...cluster.members] : [];
  const localRole = !cluster.enabled
    ? (state.locale === "zh-CN" ? "未加入" : "Not joined")
    : cluster.localIsMaster
      ? (state.locale === "zh-CN" ? "主控节点" : "Master")
      : (state.locale === "zh-CN" ? "受管节点" : "Member");
  const localRegistered = members.some(m => sameServerId(m.serverId, localServerId));

  members.sort((a, b) => {
    if (!!a.leader !== !!b.leader) return a.leader ? -1 : 1;
    const al = sameServerId(a.serverId, localServerId);
    const bl = sameServerId(b.serverId, localServerId);
    if (al !== bl) return al ? -1 : 1;
    return String(a.serverId || "").localeCompare(String(b.serverId || ""), state.locale);
  });

  const cStatus = cluster.enabled
    ? (state.locale === "zh-CN" ? "已启用" : "Enabled")
    : (state.locale === "zh-CN" ? "未启用" : "Disabled");

  document.getElementById("clusterSummary").innerHTML = [
    `<span class="chip">${state.locale === "zh-CN" ? "运维面板" : "Panel"}: ${escapeHtml(cStatus)}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "主控" : "Leader"}: ${escapeHtml(cluster.leaderServerId || "-")}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "当前" : "Current"}: ${escapeHtml(localServerId)}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "角色" : "Role"}: ${escapeHtml(localRole)}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "心跳" : "Heartbeat"}: ${escapeHtml(cluster.heartbeatSeconds ?? "-")}s</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "成员" : "Members"}: ${members.length}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "更新" : "Updated"}: ${escapeHtml(formatTime(cluster.updatedAt))}</span>`
  ].join("");

  document.getElementById("clusterMembers").innerHTML = members.map(member => {
    const isLocal = sameServerId(member.serverId, localServerId);
    const roleLabel = member.leader
      ? (state.locale === "zh-CN" ? "主控节点" : "Master")
      : (state.locale === "zh-CN" ? "受管节点" : "Member");
    const roleCls = member.leader ? "accent" : "";
    const localCls = isLocal ? "accent" : "";
    return `
      <div class="info-card ${member.leader ? "accent" : ""}">
        <div class="panel-header" style="margin-bottom:8px">
          <div><strong>${escapeHtml(member.serverId || "-")}</strong><div class="text-muted" style="font-size:12px">${isLocal ? (state.locale === "zh-CN" ? "当前访问节点" : "Current node") : (state.locale === "zh-CN" ? "集群成员" : "Cluster member")}</div></div>
          <div class="chips-row"><span class="chip ${roleCls}">${escapeHtml(roleLabel)}</span>${isLocal ? `<span class="chip ${localCls}">${state.locale === "zh-CN" ? "当前" : "Current"}</span>` : ""}</div>
        </div>
        <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "面板角色" : "Panel role"}</span><span class="kv-value">${escapeHtml(roleLabel)}</span></div>
        <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "监听地址" : "Listen"}</span><span class="kv-value">${escapeHtml(member.host || "-")}:${escapeHtml(member.port ?? "-")}</span></div>
        <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "最近上报" : "Last seen"}</span><span class="kv-value">${escapeHtml(formatTime(member.advertisedAt))}</span></div>
      </div>`;
  }).join("") || `<div class="empty-state">${escapeHtml(t("emptyPanelMembers"))}</div>`;

  const nodes = (Array.isArray(status.nodes) ? status.nodes : []).filter(node =>
    includesFilter(node.serverId, state.filters.dashboardNodes) ||
    includesFilter(node.cluster, state.filters.dashboardNodes) ||
    includesFilter(node.status, state.filters.dashboardNodes)
  );
  document.getElementById("nodesBody").innerHTML = nodes.map(node => {
    const cls = node.status === "online" ? "text-success" : "text-danger";
    return `<tr><td><strong>${escapeHtml(node.serverId)}</strong><div class="text-muted" style="font-size:12px">${escapeHtml(node.cluster || "-")}</div></td><td class="${cls}">${escapeHtml(node.status || "-")}</td><td>${escapeHtml(formatTime(node.lastSeen || node.updatedAt))}</td><td>${escapeHtml(node.latencyMillis ?? "-")} ms</td></tr>`;
  }).join("") || `<tr><td colspan="4" class="text-muted" style="text-align:center">${escapeHtml(t("emptyNodes"))}</td></tr>`;
}

// ─── Render: Modules ─────────────────────────────────────────────────────────

function renderModules(modules) {
  const history = Array.isArray(modules.history) ? modules.history : [];
  document.getElementById("modulesBody").innerHTML = MODULE_KEYS.map(key => {
    const local = modules.local?.[key];
    const shared = modules.shared?.[key];
    const effective = modules.effective?.[key];
    return `<tr>
      <td><strong>${escapeHtml(moduleName(key))}</strong><div class="text-muted" style="font-size:12px">${escapeHtml(key)}</div></td>
      <td><div class="chips-row">
        <span class="chip">${escapeHtml(t("localOnly") + boolText(local))}</span>
        <span class="chip">${escapeHtml(t("sharedOnly") + boolText(shared))}</span>
        <span class="chip ${effective ? "success" : "danger"}">${escapeHtml(t("effectiveState") + boolText(effective))}</span>
      </div></td>
      <td><div class="module-toggle">
        <button class="${shared === true ? "active enable" : ""}" data-module-key="${escapeHtml(key)}" data-module-mode="true">${escapeHtml(t("forceEnable"))}</button>
        <button class="${shared === false ? "active disable" : ""}" data-module-key="${escapeHtml(key)}" data-module-mode="false">${escapeHtml(t("forceDisable"))}</button>
        <button class="${shared === null || shared === undefined ? "active" : ""}" data-module-key="${escapeHtml(key)}" data-module-mode="clear">${escapeHtml(t("followLocal"))}</button>
      </div></td>
    </tr>`;
  }).join("");

  const historyBox = document.getElementById("modulesHistory");
  if (historyBox) {
    historyBox.innerHTML = history.length
      ? `<details><summary>${escapeHtml(t("historyModules").replace("%s", String(history.length)))}</summary><div class="details-body"><table><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thPlayer"))}</th><th>${escapeHtml(t("thContent"))}</th><th>${escapeHtml(t("thAction"))}</th></tr></thead><tbody>${history.map(item => `<tr><td>${escapeHtml(formatTime(item.updatedAt))}</td><td>${escapeHtml(item.updatedBy || "-")}</td><td>${escapeHtml(item.summary || "-")}</td><td><div class="input-group"><button class="btn-secondary btn-sm" data-history-view="modules" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("viewDetails"))}</button><button class="btn-danger btn-sm" data-history-rollback="modules" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("rollback"))}</button></div></td></tr>`).join("")}</tbody></table></div></details>`
      : `<div class="empty-state">${escapeHtml(t("emptyModuleHistory"))}</div>`;
  }
}

// ─── Render: Routes ──────────────────────────────────────────────────────────

function renderRoutes(routes) {
  const entries = (Array.isArray(routes.entries) ? routes.entries : []).filter(entry =>
    includesFilter(entry.serverId, state.filters.routes) ||
    includesFilter(entry.localTarget, state.filters.routes) ||
    includesFilter(entry.sharedTarget, state.filters.routes) ||
    includesFilter(entry.effectiveTarget, state.filters.routes) ||
    includesFilter(entry.source, state.filters.routes)
  );
  document.getElementById("routesBody").innerHTML = entries.map(entry => {
    const sid = escapeHtml(entry.serverId || "");
    const actions = entry.sharedTarget
      ? `<button class="btn-danger btn-sm" data-route-delete="${sid}">${state.locale === "zh-CN" ? "删除" : "Delete"}</button>`
      : "";
    const editBtn = `<button class="btn-secondary btn-sm" data-route-edit="${sid}" data-route-target="${escapeHtml(entry.sharedTarget || entry.effectiveTarget || "")}">${state.locale === "zh-CN" ? "编辑" : "Edit"}</button>`;
    return `<tr><td><strong>${sid}</strong></td><td>${escapeHtml(entry.localTarget || "-")}</td><td>${escapeHtml(entry.sharedTarget || "-")}</td><td>${escapeHtml(entry.effectiveTarget || "-")}</td><td>${escapeHtml(entry.source || "-")}</td><td><div class="input-group">${editBtn}${actions}</div></td></tr>`;
  }).join("") || `<tr><td colspan="6" class="text-muted" style="text-align:center">${escapeHtml(t("emptyRoutes"))}</td></tr>`;

  document.getElementById("routesBulkInput").value = JSON.stringify({ routes: routes.shared || {} }, null, 2);

  const history = Array.isArray(routes.history) ? routes.history : [];
  const historyBox = document.getElementById("routesHistory");
  if (historyBox) {
    historyBox.innerHTML = history.length
      ? `<details><summary>${escapeHtml(t("historyRoutes").replace("%s", String(history.length)))}</summary><div class="details-body"><table><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thPlayer"))}</th><th>${escapeHtml(t("thContent"))}</th><th>${escapeHtml(t("thAction"))}</th></tr></thead><tbody>${history.map(item => `<tr><td>${escapeHtml(formatTime(item.updatedAt))}</td><td>${escapeHtml(item.updatedBy || "-")}</td><td>${escapeHtml(item.summary || "-")}</td><td><div class="input-group"><button class="btn-secondary btn-sm" data-history-view="routes" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("viewDetails"))}</button><button class="btn-danger btn-sm" data-history-rollback="routes" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("rollback"))}</button></div></td></tr>`).join("")}</tbody></table></div></details>`
      : `<div class="empty-state">${escapeHtml(t("emptyRouteHistory"))}</div>`;
  }
}

// ─── Render: Documents ───────────────────────────────────────────────────────

function renderDocuments(documents) {
  document.getElementById("documentsList").innerHTML = (documents || []).filter(doc =>
    includesFilter(doc.namespace, state.filters.documents) ||
    includesFilter(doc.dataKey, state.filters.documents) ||
    includesFilter(doc.title, state.filters.documents) ||
    includesFilter(doc.summary, state.filters.documents)
  ).map((doc, index) => {
    const history = Array.isArray(doc.history) ? doc.history : [];
    const schema = doc.schemaDefinition || null;
    return `
    <div class="doc-card">
      <div class="doc-header">
        <div><div class="doc-title">${escapeHtml(doc.title || `${doc.namespace}/${doc.dataKey}`)}</div><div class="text-muted" style="font-size:12px">${escapeHtml(doc.namespace)}/${escapeHtml(doc.dataKey)}</div></div>
        <button class="btn-secondary btn-sm" data-doc-toggle="${index}">${state.locale === "zh-CN" ? "展开" : "Expand"}</button>
      </div>
      <div style="color:var(--text-secondary);font-size:13px;line-height:1.6">${escapeHtml(doc.purpose || t("noDescription"))}</div>
      ${schema
        ? `<div class="banner warning mt-sm"><strong>${escapeHtml(schema.name || "Schema")}</strong> v${escapeHtml(schema.version ?? "-")}<br>${escapeHtml(schema.description || "")}</div>`
        : `<div class="banner mt-sm">${state.locale === "zh-CN" ? "当前文档未注册 schema" : "No schema registered"}</div>`}
      <div class="doc-meta">
        <div class="info-card"><div class="kv-label">${state.locale === "zh-CN" ? "状态" : "Status"}</div><div class="kv-value">${doc.present ? (state.locale === "zh-CN" ? "已存在" : "Present") : (state.locale === "zh-CN" ? "未写入" : "Missing")}</div></div>
        <div class="info-card"><div class="kv-label">${state.locale === "zh-CN" ? "版本" : "Version"}</div><div class="kv-value">${escapeHtml(doc.version ?? "-")}</div></div>
        <div class="info-card"><div class="kv-label">${state.locale === "zh-CN" ? "更新" : "Updated"}</div><div class="kv-value">${escapeHtml(formatTime(doc.updatedAt))}</div></div>
      </div>
      <div class="chips-row mt-sm">
        <span class="chip">${state.locale === "zh-CN" ? "更新" : "By"}: ${escapeHtml(doc.updatedBy || "-")}</span>
        <span class="chip">${state.locale === "zh-CN" ? "格式" : "Format"}: ${escapeHtml(formatPayloadType(doc.payloadType))}</span>
      </div>
      <div class="text-muted mt-sm" style="font-size:12px">${escapeHtml(doc.summary || t("noSummary"))}</div>
      <div class="toolbar mt-sm">
        <span style="font-size:12px;color:var(--text-muted)">${state.locale === "zh-CN" ? "编辑 payload 并保存到配置中心" : "Edit payload and save to config center"}</span>
        <button class="btn-secondary btn-sm" data-doc-fill="${index}">${state.locale === "zh-CN" ? "填入当前" : "Fill current"}</button>
      </div>
      <div id="payload-${index}" class="payload-block" style="display:none;margin-top:12px">
        <form class="stack" data-doc-form="${index}" data-doc-namespace="${escapeHtml(doc.namespace)}" data-doc-key="${escapeHtml(doc.dataKey)}">
          <div class="field-grid">
            ${fieldGroup("schemaVersion", inputHtml("number", `schemaVersion-${index}`, doc.schemaVersion ?? 1, "1", 'min="1" step="1"'), "Schema version")}
            ${fieldGroup("source", inputHtml("text", `source-${index}`, doc.source || "web-panel", "web-panel"), "Source")}
          </div>
          ${fieldGroup("summary", inputHtml("text", `summary-${index}`, doc.summary || "", state.locale === "zh-CN" ? "本次变更摘要" : "Change summary"))}
          <div>
            <label style="font-size:12px;font-weight:600;color:var(--text-muted)">payload (${escapeHtml(formatPayloadType(doc.payloadType))})</label>
            <textarea name="payload-${index}" data-doc-payload="${index}" style="margin-top:4px">${escapeHtml(doc.payloadPretty || doc.payload || "")}</textarea>
          </div>
          ${schema ? `<div class="info-card"><strong style="font-size:13px">Schema</strong><div class="text-muted" style="margin-top:6px;font-size:12px">${state.locale === "zh-CN" ? "必填" : "Required"}: ${escapeHtml((schema.requiredPaths || []).join(", ") || "-")}</div><div class="text-muted" style="margin-top:4px;font-size:12px">${state.locale === "zh-CN" ? "类型" : "Types"}: ${escapeHtml(Object.entries(schema.fieldTypes || {}).map(([k,v]) => `${k}: ${v}`).join("; ") || "-")}</div></div>` : ""}
          <div class="banner danger">${state.locale === "zh-CN" ? "风险提示：保存配置文档会立即写入共享配置中心" : "Warning: saving writes to shared config center"}</div>
          <div class="input-group"><button type="submit" class="btn-warning">${state.locale === "zh-CN" ? "保存文档" : "Save Document"}</button></div>
        </form>
        <details class="mt-sm"><summary>${escapeHtml(t("historyDocuments").replace("%s", String(history.length)))}</summary><div class="details-body">${history.length ? `<table><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thPlayer"))}</th><th>${escapeHtml(t("thContent"))}</th><th>${escapeHtml(t("thAction"))}</th></tr></thead><tbody>${history.map(item => `<tr><td>${escapeHtml(formatTime(item.updatedAt))}</td><td>${escapeHtml(item.updatedBy || "-")}</td><td>${escapeHtml(item.summary || "-")}</td><td><div class="input-group"><button class="btn-secondary btn-sm" data-history-view="document" data-doc-namespace="${escapeHtml(doc.namespace)}" data-doc-key="${escapeHtml(doc.dataKey)}" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("viewDetails"))}</button><button class="btn-danger btn-sm" data-history-rollback="document" data-doc-namespace="${escapeHtml(doc.namespace)}" data-doc-key="${escapeHtml(doc.dataKey)}" data-history-version="${escapeHtml(item.version ?? "")}">${escapeHtml(t("rollback"))}</button></div></td></tr>`).join("")}</tbody></table>` : `<div class="empty-state">${escapeHtml(t("emptyDocHistory"))}</div>`}</div></details>
      </div>
    </div>`;
  }).join("") || `<div class="empty-state">${escapeHtml(t("emptyDocuments"))}</div>`;
}

// ─── Render: Logs ────────────────────────────────────────────────────────────

function renderLogs(logs) {
  const servers = (Array.isArray(logs.servers) ? logs.servers : []).filter(s => includesFilter(s.serverId, state.filters.logsServer));
  document.getElementById("logsSummary").innerHTML = [
    `<span class="chip">${state.locale === "zh-CN" ? "本地" : "Local"}: ${escapeHtml(logs.localServerId || "-")}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "节点" : "Nodes"}: ${servers.length}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "刷新" : "Refreshed"}: ${escapeHtml(formatTime(logs.updatedAt))}</span>`
  ].join("");
  document.getElementById("logsList").innerHTML = servers.map(server => {
    const entries = (Array.isArray(server.entries) ? server.entries : []).filter(e =>
      includesFilter(e.level, state.filters.logsKeyword) || includesFilter(e.message, state.filters.logsKeyword)
    );
    const blocks = entries.map(e => `<tr><td style="white-space:nowrap">${escapeHtml(formatTime(e.occurredAt))}</td><td style="white-space:nowrap"><span class="chip ${e.level === "SEVERE" || e.level === "ERROR" ? "danger" : e.level === "WARNING" ? "warning" : ""}">${escapeHtml(e.level || "-")}</span></td><td style="font-family:monospace;font-size:12px">${escapeHtml(e.message || "-")}</td></tr>`).join("") || `<tr><td colspan="3" class="text-muted" style="text-align:center">${escapeHtml(t("emptyLogEntries"))}</td></tr>`;
    return `<div class="info-card ${server.local ? "accent" : ""}">
      <div class="panel-header" style="margin-bottom:8px">
        <div><strong>${escapeHtml(server.serverId || "-")}</strong><div class="text-muted" style="font-size:12px">${server.local ? (state.locale === "zh-CN" ? "当前节点" : "Current node") : (state.locale === "zh-CN" ? "集群节点" : "Cluster node")}</div></div>
        <div class="chips-row"><span class="chip">${escapeHtml(server.entryCount ?? 0)} entries</span><span class="chip">${escapeHtml(formatTime(server.updatedAt))}</span></div>
      </div>
      <table><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thLevel"))}</th><th>${escapeHtml(t("thContent"))}</th></tr></thead><tbody>${blocks}</tbody></table>
    </div>`;
  }).join("") || `<div class="empty-state">${escapeHtml(t("emptyLogs"))}</div>`;
}

// ─── Render: Node configs ────────────────────────────────────────────────────

function renderNodeConfigs(nodeConfigs) {
  const nodes = (Array.isArray(nodeConfigs.nodes) ? nodeConfigs.nodes : []).filter(node =>
    includesFilter(node.serverId, state.filters.nodeConfigs) ||
    includesFilter(node.nodeStatus?.status, state.filters.nodeConfigs) ||
    includesFilter(node.latestApply?.resultMessage, state.filters.nodeConfigs) ||
    includesFilter(node.latestApply?.status, state.filters.nodeConfigs)
  );
  const onlineCount = nodes.filter(n => String(n.nodeStatus?.status || "").toLowerCase() === "online").length;
  const snapshotCount = nodes.filter(n => n.hasSnapshot).length;

  document.getElementById("nodeConfigsSummary").innerHTML = [
    `<span class="chip">${state.locale === "zh-CN" ? "本地" : "Local"}: ${escapeHtml(nodeConfigs.localServerId || "-")}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "节点" : "Nodes"}: ${nodes.length}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "在线" : "Online"}: ${onlineCount}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "快照" : "Snapshots"}: ${snapshotCount}</span>`,
    `<span class="chip">${state.locale === "zh-CN" ? "刷新" : "Refreshed"}: ${escapeHtml(formatTime(nodeConfigs.updatedAt))}</span>`
  ].join("");

  document.getElementById("nodeConfigsBody").innerHTML = nodes.map(node => {
    const selected = sameServerId(state.selectedNodeConfigServerId, node.serverId);
    const latestApply = node.latestApply || null;
    const snapshot = node.snapshot || null;
    return `<tr>
      <td><strong>${escapeHtml(node.serverId || "-")}</strong><div class="text-muted" style="font-size:12px">${node.local ? (state.locale === "zh-CN" ? "当前节点" : "Current node") : escapeHtml(node.nodeStatus?.cluster || (state.locale === "zh-CN" ? "集群节点" : "Cluster node"))}</div></td>
      <td><span class="chip ${nodeStatusClass(node.nodeStatus)}">${escapeHtml(nodeStatusText(node.nodeStatus))}</span><div class="text-muted" style="font-size:12px">${escapeHtml(formatTime(node.nodeStatus?.lastSeen || node.nodeStatus?.updatedAt))}</div></td>
      <td><span class="chip ${node.hasSnapshot ? "success" : ""}">${node.hasSnapshot ? (state.locale === "zh-CN" ? "已上报" : "Uploaded") : (state.locale === "zh-CN" ? "未上报" : "Missing")}</span><div class="text-muted" style="font-size:12px">${escapeHtml(snapshot?.capturedAt ? formatTime(snapshot.capturedAt) : (state.locale === "zh-CN" ? "等待同步" : "Waiting"))}</div></td>
      <td><span class="chip ${applyStatusClass(latestApply?.status)}">${escapeHtml(applyStatusText(latestApply?.status))}</span><div class="text-muted" style="font-size:12px">${escapeHtml(latestApply?.resultMessage || t("noApplyRecord"))}</div></td>
      <td><button class="${selected ? "btn-primary" : "btn-secondary"} btn-sm" data-node-config-open="${escapeHtml(node.serverId || "")}">${selected ? (state.locale === "zh-CN" ? "当前" : "Viewing") : (state.locale === "zh-CN" ? "查看" : "View")}</button></td>
    </tr>`;
  }).join("") || `<tr><td colspan="5" class="text-muted" style="text-align:center">${escapeHtml(t("emptyNodeConfigs"))}</td></tr>`;
}

function renderNodeConfigDetail(detail) {
  const intro = document.getElementById("nodeConfigDetailIntro");
  const meta = document.getElementById("nodeConfigDetailMeta");
  const statusEl = document.getElementById("nodeConfigDetailStatus");
  const form = document.getElementById("nodeConfigDetailForm");
  const raw = document.getElementById("nodeConfigDetailRaw");

  if (!detail || !detail.serverId) {
    state.nodeConfigDetail = null;
    intro.innerHTML = t("nodeSelectIntro");
    meta.innerHTML = "";
    statusEl.innerHTML = "";
    form.innerHTML = "";
    raw.innerHTML = "";
    return;
  }

  state.nodeConfigDetail = detail;
  const snapshot = detail.snapshot || null;
  const latestApply = detail.latestApply || null;
  const applyHistory = Array.isArray(detail.applyHistory) ? detail.applyHistory : [];
  const editableConfig = snapshot?.editableConfig || {};
  const messaging = editableConfig.messaging || {};
  const webPanel = editableConfig.webPanel || {};
  const modules = editableConfig.modules || {};

  intro.innerHTML = `${state.locale === "zh-CN" ? "当前查看" : "Viewing"} <strong>${escapeHtml(detail.serverId)}</strong>${snapshot ? (state.locale === "zh-CN" ? "，修改后会写回目标节点 config.yml" : ", changes write back to target node") : (state.locale === "zh-CN" ? "，暂未上报快照" : ", no snapshot yet")}`;

  meta.innerHTML = [
    `<span class="chip">${escapeHtml(detail.serverId)}</span>`,
    `<span class="chip ${nodeStatusClass(detail.nodeStatus)}">${escapeHtml(nodeStatusText(detail.nodeStatus))}</span>`,
    `<span class="chip">${detail.local ? (state.locale === "zh-CN" ? "本地" : "Local") : (state.locale === "zh-CN" ? "远程" : "Remote")}</span>`,
    `<span class="chip ${snapshot ? "success" : ""}">${snapshot ? (state.locale === "zh-CN" ? "已上报" : "Uploaded") : (state.locale === "zh-CN" ? "未上报" : "Missing")}</span>`,
    `<span class="chip ${applyStatusClass(latestApply?.status)}">${escapeHtml(applyStatusText(latestApply?.status))}</span>`
  ].join("");

  statusEl.innerHTML = [
    kvCard([
      [state.locale === "zh-CN" ? "节点状态" : "Status", nodeStatusText(detail.nodeStatus)],
      [state.locale === "zh-CN" ? "集群" : "Cluster", detail.nodeStatus?.cluster || "-"],
      [state.locale === "zh-CN" ? "最后心跳" : "Last seen", formatTime(detail.nodeStatus?.lastSeen || detail.nodeStatus?.updatedAt)],
      [state.locale === "zh-CN" ? "延迟" : "Latency", detail.nodeStatus?.latencyMillis !== undefined ? `${detail.nodeStatus.latencyMillis} ms` : "-"]
    ], detail.local),
    kvCard([
      [state.locale === "zh-CN" ? "应用状态" : "Apply status", applyStatusText(latestApply?.status)],
      [state.locale === "zh-CN" ? "申请人" : "Requested by", latestApply?.requestedBy || "-"],
      [state.locale === "zh-CN" ? "申请时间" : "Requested at", formatTime(latestApply?.requestedAt)],
      [state.locale === "zh-CN" ? "结果" : "Result", latestApply?.resultMessage || "-"]
    ]),
    kvCard([
      [state.locale === "zh-CN" ? "快照时间" : "Snapshot", formatTime(snapshot?.capturedAt)],
      [state.locale === "zh-CN" ? "快照更新人" : "Updated by", snapshot?.updatedBy || "-"],
      [state.locale === "zh-CN" ? "Token" : "Token", webPanel.tokenConfigured ? (state.locale === "zh-CN" ? "已配置" : "Configured") : (state.locale === "zh-CN" ? "未配置" : "Not set")]
    ])
  ].join("");

  if (!snapshot) {
    form.innerHTML = `<div class="empty-state">${state.locale === "zh-CN" ? "暂无配置快照，请等待目标节点同步" : "No snapshot, waiting for target node"}</div>`;
    raw.innerHTML = `<details><summary>${state.locale === "zh-CN" ? "查看原始数据" : "Raw data"}</summary><div class="details-body"><pre>${escapeHtml(JSON.stringify(detail, null, 2))}</pre></div></details>`;
    return;
  }

  form.innerHTML = `
    <form id="nodeConfigApplyForm" data-server-id="${escapeHtml(detail.serverId)}" class="stack">
      <div class="panel-header" style="margin-bottom:0">
        <div><strong>${state.locale === "zh-CN" ? "编辑白名单" : "Edit whitelist"}</strong><div class="subtitle">${state.locale === "zh-CN" ? "token 留空表示保持原值" : "Leave token blank to keep current"}</div></div>
        <button type="submit" class="btn-primary">${state.locale === "zh-CN" ? "提交" : "Submit"}</button>
      </div>
      <div class="field-grid">
        ${fieldGroup("messaging.enabled", booleanSelectHtml("messaging.enabled", messaging.enabled), boolText(messaging.enabled))}
        ${fieldGroup("messaging.redisUri", inputHtml("text", "messaging.redisUri", messaging.redisUri, "redis://127.0.0.1:6379/0"), "Redis URI")}
        ${fieldGroup("messaging.channel", inputHtml("text", "messaging.channel", messaging.channel, "crossserver"), "Channel")}
        ${fieldGroup("webPanel.enabled", booleanSelectHtml("webPanel.enabled", webPanel.enabled), boolText(webPanel.enabled))}
        ${fieldGroup("webPanel.host", inputHtml("text", "webPanel.host", webPanel.host, "127.0.0.1"), "Host")}
        ${fieldGroup("webPanel.port", inputHtml("number", "webPanel.port", webPanel.port, "8765", 'min="1" max="65535"'), "Port")}
        ${fieldGroup("webPanel.masterServerId", inputHtml("text", "webPanel.masterServerId", webPanel.masterServerId, "lobby"), "Master serverId")}
        ${fieldGroup("webPanel.token", inputHtml("password", "webPanel.token", "", state.locale === "zh-CN" ? "留空表示不修改" : "Leave blank", 'autocomplete="new-password"'), webPanel.tokenConfigured ? (state.locale === "zh-CN" ? "已配置，留空保持" : "Configured, leave blank") : (state.locale === "zh-CN" ? "未配置" : "Not set"))}
      </div>
      <div class="field-grid">
        ${MODULE_KEYS.map(key => fieldGroup(`modules.${key}`, booleanSelectHtml(`modules.${key}`, modules[key]), moduleName(key))).join("")}
      </div>
    </form>`;

  raw.innerHTML = `${applyHistory.length ? `<details><summary>${escapeHtml(t("historyNodeApply").replace("%s", String(applyHistory.length)))}</summary><div class="details-body"><table><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thPlayer"))}</th><th>${escapeHtml(t("thStatus"))}</th><th>${escapeHtml(t("thContent"))}</th></tr></thead><tbody>${applyHistory.map(item => `<tr><td>${escapeHtml(formatTime(item.updatedAt || item.requestedAt))}</td><td>${escapeHtml(item.updatedBy || item.requestedBy || "-")}</td><td>${escapeHtml(applyStatusText(item.status))}</td><td>${escapeHtml(item.summary || item.resultMessage || "-")}</td></tr>`).join("")}</tbody></table></div></details>` : `<div class="empty-state">${escapeHtml(t("emptyModuleHistory"))}</div>`}<details class="mt-sm"><summary>${state.locale === "zh-CN" ? "查看原始数据" : "Raw data"}</summary><div class="details-body"><pre>${escapeHtml(JSON.stringify(detail, null, 2))}</pre></div></details>`;
}

function renderRecentTransfers(entries) {
  document.getElementById("recentTransfersBody").innerHTML = (entries || []).map(entry =>
    `<tr><td>${escapeHtml(formatTime(entry.createdAt))}</td><td><strong>${escapeHtml(entry.playerName || "-")}</strong></td><td>${escapeHtml(entry.sourceServerId || "-")}</td><td>${escapeHtml(entry.targetServerId || "-")}</td><td>${escapeHtml(entry.eventType || "-")}</td><td>${escapeHtml(entry.status || "-")}</td></tr>`
  ).join("") || `<tr><td colspan="6" class="text-muted" style="text-align:center">${escapeHtml(t("emptyTransferHistory"))}</td></tr>`;
}

// ─── Forms ───────────────────────────────────────────────────────────────────

function formValue(form, name) {
  return String(form.elements.namedItem(name)?.value ?? "").trim();
}

function requiredFormValue(form, name) {
  const value = formValue(form, name);
  if (!value) throw new Error(`${name} ${state.locale === "zh-CN" ? "不能为空" : "is required"}`);
  return value;
}

function formBooleanValue(form, name) {
  return requiredFormValue(form, name) === "true";
}

function formPortValue(form, name) {
  const raw = requiredFormValue(form, name);
  const value = Number(raw);
  if (!Number.isInteger(value) || value < 1 || value > 65535) {
    throw new Error(`${name} ${state.locale === "zh-CN" ? "必须在 1-65535 之间" : "must be 1-65535"}`);
  }
  return value;
}

function collectNodeConfigChanges(form) {
  const changes = {
    messaging: {
      enabled: formBooleanValue(form, "messaging.enabled"),
      redisUri: requiredFormValue(form, "messaging.redisUri"),
      channel: requiredFormValue(form, "messaging.channel")
    },
    webPanel: {
      enabled: formBooleanValue(form, "webPanel.enabled"),
      host: requiredFormValue(form, "webPanel.host"),
      port: formPortValue(form, "webPanel.port"),
      masterServerId: requiredFormValue(form, "webPanel.masterServerId")
    },
    modules: {}
  };
  const token = formValue(form, "webPanel.token");
  if (token) changes.webPanel.token = token;
  MODULE_KEYS.forEach(key => { changes.modules[key] = formBooleanValue(form, `modules.${key}`); });
  return changes;
}

// ─── Data loading ────────────────────────────────────────────────────────────

async function loadOverview() {
  clearMessage(authMessage);
  const overview = await api("/api/overview");
  state.overview = overview;
  renderStatus(overview.status || {});
  renderModules(overview.modules || {});
  renderRoutes(overview.routes || {});
  renderDocuments(overview.documents || []);
  renderLogs(overview.logs || {});
  renderRecentTransfers(overview.recentTransfers || []);
  const nodeConfigs = overview.nodeConfigs || {};
  renderNodeConfigs(nodeConfigs);
  const nodes = Array.isArray(nodeConfigs.nodes) ? nodeConfigs.nodes : [];
  const selectedNode = nodes.find(n => sameServerId(n.serverId, state.selectedNodeConfigServerId))
    || nodes.find(n => sameServerId(n.serverId, nodeConfigs.localServerId))
    || nodes[0]
    || null;
  state.selectedNodeConfigServerId = selectedNode?.serverId || null;
  renderNodeConfigDetail(selectedNode);
  return overview;
}

async function reloadModules() {
  const data = await api("/api/modules");
  if (state.overview) state.overview.modules = data;
  renderModules(data);
}

async function reloadRoutes() {
  const data = await api("/api/routes");
  if (state.overview) state.overview.routes = data;
  renderRoutes(data);
}

async function reloadLogs() {
  const data = await api("/api/logs");
  if (state.overview) state.overview.logs = data;
  renderLogs(data);
}

async function reloadDocuments() {
  const data = await api("/api/config-documents");
  if (state.overview) state.overview.documents = data;
  renderDocuments(data);
  return data;
}

async function reloadNodeConfigs(preserveSelection = true) {
  const data = await api("/api/node-configs");
  if (state.overview) state.overview.nodeConfigs = data;
  renderNodeConfigs(data);
  const nodes = Array.isArray(data.nodes) ? data.nodes : [];
  const preferred = preserveSelection ? state.selectedNodeConfigServerId : null;
  const selected = nodes.find(n => sameServerId(n.serverId, preferred))
    || nodes.find(n => sameServerId(n.serverId, data.localServerId))
    || nodes[0]
    || null;
  state.selectedNodeConfigServerId = selected?.serverId || null;
  renderNodeConfigDetail(selected);
  return data;
}

async function openNodeConfig(serverId) {
  if (!serverId) return;
  state.selectedNodeConfigServerId = serverId;
  if (state.overview?.nodeConfigs) renderNodeConfigs(state.overview.nodeConfigs);
  const detail = await api(`/api/node-configs/detail?serverId=${encodeURIComponent(serverId)}`);
  renderNodeConfigDetail(detail);
}

// ─── Global actions ──────────────────────────────────────────────────────────

window.setModuleState = async function(key, mode) {
  const box = document.getElementById("modulesMessage");
  try {
    const value = mode === "clear" ? null : mode === "true";
    const data = await api("/api/modules", { method: "PUT", body: JSON.stringify({ [key]: value }) });
    if (state.overview) state.overview.modules = data;
    renderModules(data);
    const label = mode === "clear" ? t("followLocal") : mode === "true" ? t("forceEnable") : t("forceDisable");
    setMessage(box, "success", `${key} → ${label}`);
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
};

window.prefillRoute = function(serverId, target) {
  document.getElementById("routeServerIdInput").value = serverId;
  document.getElementById("routeProxyTargetInput").value = target;
};

window.deleteRoute = async function(serverId) {
  const box = document.getElementById("routesMessage");
  const ok = await askConfirm(
    state.locale === "zh-CN" ? "删除共享路由" : "Delete route",
    `${state.locale === "zh-CN" ? "确认删除" : "Confirm delete"} ${serverId}?`,
    state.locale === "zh-CN" ? "确认删除" : "Delete"
  );
  if (!ok) return;
  try {
    const data = await api(`/api/routes?serverId=${encodeURIComponent(serverId)}`, { method: "DELETE" });
    await reloadRoutes();
    setMessage(box, "success", data.removed
      ? `${serverId} ${state.locale === "zh-CN" ? "已删除" : "deleted"}`
      : `${serverId} ${state.locale === "zh-CN" ? "不存在" : "not found"}`
    );
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
};

window.togglePayload = function(index) {
  const box = document.getElementById(`payload-${index}`);
  if (!box) return;
  const isShown = box.style.display === "block";
  box.style.display = isShown ? "none" : "block";
  const btn = document.querySelector(`button[data-doc-toggle="${index}"]`);
  if (btn) btn.textContent = isShown ? (state.locale === "zh-CN" ? "展开" : "Expand") : (state.locale === "zh-CN" ? "收起" : "Collapse");
};

// ─── Event delegation ────────────────────────────────────────────────────────

document.getElementById("modulesBody").addEventListener("click", e => {
  const btn = e.target.closest("button[data-module-key][data-module-mode]");
  if (!btn) return;
  setModuleState(btn.dataset.moduleKey || "", btn.dataset.moduleMode || "clear");
});

document.getElementById("routesBody").addEventListener("click", e => {
  const editBtn = e.target.closest("button[data-route-edit]");
  if (editBtn) { prefillRoute(editBtn.dataset.routeEdit || "", editBtn.dataset.routeTarget || ""); return; }
  const delBtn = e.target.closest("button[data-route-delete]");
  if (delBtn) { deleteRoute(delBtn.dataset.routeDelete || ""); }
});

document.getElementById("documentsList").addEventListener("click", e => {
  const toggleBtn = e.target.closest("button[data-doc-toggle]");
  if (toggleBtn) { togglePayload(toggleBtn.dataset.docToggle); return; }
  const fillBtn = e.target.closest("button[data-doc-fill]");
  if (fillBtn) {
    const index = fillBtn.dataset.docFill;
    const doc = Array.isArray(state.overview?.documents) ? state.overview.documents[Number(index)] : null;
    const textarea = document.querySelector(`textarea[data-doc-payload="${index}"]`);
    if (!doc || !textarea) return;
    textarea.value = doc.payloadPretty || doc.payload || "";
    return;
  }
  const viewBtn = e.target.closest("button[data-history-view]");
  if (viewBtn) {
    const type = viewBtn.dataset.historyView;
    const version = Number(viewBtn.dataset.historyVersion || "0");
    const namespace = viewBtn.dataset.docNamespace || "";
    const dataKey = viewBtn.dataset.docKey || "";
    let item = null;
    if (type === "document") {
      const doc = (state.overview?.documents || []).find(d => d.namespace === namespace && d.dataKey === dataKey);
      item = (doc?.history || []).find(h => Number(h.version) === version) || null;
    } else if (type === "modules") {
      item = (state.overview?.modules?.history || []).find(h => Number(h.version) === version) || null;
    } else if (type === "routes") {
      item = (state.overview?.routes?.history || []).find(h => Number(h.version) === version) || null;
    }
    if (!item) return;
    askConfirm(`${type} v${version}`, diffText(item.payload || "-", item.payload || "-"), state.locale === "zh-CN" ? "关闭" : "Close").then(() => {});
    return;
  }
  const rollbackBtn = e.target.closest("button[data-history-rollback]");
  if (!rollbackBtn) return;
  const type = rollbackBtn.dataset.historyRollback;
  const version = Number(rollbackBtn.dataset.historyVersion || "0");
  const namespace = rollbackBtn.dataset.docNamespace || "";
  const dataKey = rollbackBtn.dataset.docKey || "";
  (async () => {
    const box = document.getElementById("documentsMessage");
    const ok = await askConfirm(
      state.locale === "zh-CN" ? "确认回滚" : "Confirm rollback",
      `${state.locale === "zh-CN" ? "确认回滚" : "Rollback"} ${type} v${version}?`,
      state.locale === "zh-CN" ? "确认回滚" : "Rollback"
    );
    if (!ok) return;
    try {
      if (type === "document") {
        await api("/api/config-documents", { method: "POST", body: JSON.stringify({ namespace, dataKey, version }) });
        await reloadDocuments();
      } else if (type === "modules") {
        await api("/api/modules", { method: "POST", body: JSON.stringify({ version }) });
        await reloadModules();
      } else if (type === "routes") {
        await api("/api/routes", { method: "POST", body: JSON.stringify({ version }) });
        await reloadRoutes();
      }
      setMessage(box, "success", `${type} v${version} ${state.locale === "zh-CN" ? "已回滚" : "rolled back"}`);
    } catch (error) {
      setMessage(box, "error", error.message);
      reportGlobalError(error);
    }
  })();
});

document.getElementById("documentsList").addEventListener("submit", async e => {
  const form = e.target.closest("form[data-doc-form]");
  if (!form) return;
  e.preventDefault();
  const box = document.getElementById("documentsMessage");
  try {
    const index = form.dataset.docForm;
    const payloadField = form.querySelector(`textarea[data-doc-payload="${index}"]`);
    const schemaField = form.elements.namedItem(`schemaVersion-${index}`);
    const sourceField = form.elements.namedItem(`source-${index}`);
    const summaryField = form.elements.namedItem(`summary-${index}`);
    const namespace = form.dataset.docNamespace || "";
    const dataKey = form.dataset.docKey || "";
    const payload = String(payloadField?.value || "").trim();
    const schemaVersion = Number(String(schemaField?.value || "1").trim());
    const source = String(sourceField?.value || "").trim();
    const summary = String(summaryField?.value || "").trim();
    if (!namespace || !dataKey) throw new Error(state.locale === "zh-CN" ? "缺少配置文档标识" : "Missing document identifier");
    if (!payload) throw new Error(state.locale === "zh-CN" ? "payload 不能为空" : "Payload is required");
    if (!Number.isInteger(schemaVersion) || schemaVersion < 1) throw new Error(state.locale === "zh-CN" ? "schemaVersion 必须 >= 1" : "schemaVersion must be >= 1");
    const ok = await askConfirm(
      state.locale === "zh-CN" ? "保存配置文档" : "Save document",
      `${state.locale === "zh-CN" ? "确认保存" : "Save"} ${namespace}/${dataKey}?`,
      state.locale === "zh-CN" ? "确认保存" : "Save"
    );
    if (!ok) return;
    await api("/api/config-documents", { method: "PUT", body: JSON.stringify({ namespace, dataKey, payload, schemaVersion, source, summary }) });
    await reloadDocuments();
    setMessage(box, "success", `${namespace}/${dataKey} ${state.locale === "zh-CN" ? "已保存" : "saved"}`);
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("nodeConfigsBody").addEventListener("click", async e => {
  const btn = e.target.closest("button[data-node-config-open]");
  if (!btn) return;
  const box = document.getElementById("nodeConfigsMessage");
  try {
    clearMessage(box);
    await openNodeConfig(btn.dataset.nodeConfigOpen || "");
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("nodeConfigDetailForm").addEventListener("submit", async e => {
  const form = e.target.closest("form#nodeConfigApplyForm");
  if (!form) return;
  e.preventDefault();
  const box = document.getElementById("nodeConfigsMessage");
  try {
    const serverId = form.dataset.serverId || state.selectedNodeConfigServerId || "";
    if (!serverId) throw new Error(state.locale === "zh-CN" ? "缺少目标节点" : "Missing target serverId");
    const changes = collectNodeConfigChanges(form);
    const ok = await askConfirm(
      state.locale === "zh-CN" ? "提交节点配置" : "Submit node config",
      `${state.locale === "zh-CN" ? "确认向" : "Submit to"} ${serverId}?`,
      state.locale === "zh-CN" ? "确认提交" : "Submit"
    );
    if (!ok) return;
    const result = await api("/api/node-configs/apply", { method: "POST", body: JSON.stringify({ serverId, changes }) });
    setMessage(box, "success", `${serverId}: ${applyStatusText(result.status)} #${result.requestId || "-"}`);
    await reloadNodeConfigs(true);
    await openNodeConfig(serverId);
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
});

// ─── Direct button handlers ──────────────────────────────────────────────────

document.getElementById("saveAuthBtn").addEventListener("click", async () => {
  localStorage.setItem("crossserver.web.token", tokenInput.value.trim());
  localStorage.setItem("crossserver.web.actor", (actorInput.value || "web-panel").trim() || "web-panel");
  try {
    await loadOverview();
    setMessage(authMessage, "success", t("authSavedOk"));
  } catch (error) {
    setMessage(authMessage, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("refreshBtn").addEventListener("click", async () => {
  try {
    await loadOverview();
    setMessage(authMessage, "success", t("overviewReloaded"));
  } catch (error) {
    setMessage(authMessage, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("reloadBtn").addEventListener("click", async () => {
  if (!(await askConfirm(
    state.locale === "zh-CN" ? "重载节点" : "Reload node",
    state.locale === "zh-CN" ? "确认重载当前节点?" : "Confirm reload?",
    state.locale === "zh-CN" ? "确认重载" : "Reload"
  ))) return;
  try {
    const result = await api("/api/reload", { method: "POST", body: JSON.stringify({ confirm: "reload" }) });
    if (!result.accepted) {
      setMessage(authMessage, "error", state.locale === "zh-CN" ? "重载进行中" : "Reload in progress");
      return;
    }
    setMessage(authMessage, "success", state.locale === "zh-CN" ? "已接受重载请求..." : "Reload accepted...");
    waitForPanelRecovery();
  } catch (error) {
    if (error.status === 409) {
      setMessage(authMessage, "error", state.locale === "zh-CN" ? "重载进行中" : "Reload in progress");
      reportGlobalError(error);
      return;
    }
    setMessage(authMessage, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("reloadModulesBtn").addEventListener("click", async () => {
  const box = document.getElementById("modulesMessage");
  try { await reloadModules(); setMessage(box, "success", t("modulesReloaded")); }
  catch (error) { setMessage(box, "error", error.message); reportGlobalError(error); }
});

document.getElementById("reloadRoutesBtn").addEventListener("click", async () => {
  const box = document.getElementById("routesMessage");
  try { await reloadRoutes(); setMessage(box, "success", t("routesReloaded")); }
  catch (error) { setMessage(box, "error", error.message); reportGlobalError(error); }
});

document.getElementById("reloadLogsBtn").addEventListener("click", async () => {
  try { await reloadLogs(); setMessage(authMessage, "success", t("logsReloaded")); }
  catch (error) { setMessage(authMessage, "error", error.message); reportGlobalError(error); }
});

document.getElementById("reloadNodeConfigsBtn").addEventListener("click", async () => {
  const box = document.getElementById("nodeConfigsMessage");
  try { await reloadNodeConfigs(true); setMessage(box, "success", t("nodeConfigsReloaded")); }
  catch (error) { setMessage(box, "error", error.message); reportGlobalError(error); }
});

document.getElementById("dashboardNodesSearch")?.addEventListener("input", e => {
  state.filters.dashboardNodes = e.target.value || "";
  renderStatus(state.overview?.status || {});
});

document.getElementById("routesSearchInput")?.addEventListener("input", e => {
  state.filters.routes = e.target.value || "";
  renderRoutes(state.overview?.routes || {});
});

document.getElementById("documentsSearchInput")?.addEventListener("input", e => {
  state.filters.documents = e.target.value || "";
  renderDocuments(state.overview?.documents || []);
});

document.getElementById("logsServerSearch")?.addEventListener("input", e => {
  state.filters.logsServer = e.target.value || "";
  renderLogs(state.overview?.logs || {});
});

document.getElementById("logsKeywordSearch")?.addEventListener("input", e => {
  state.filters.logsKeyword = e.target.value || "";
  renderLogs(state.overview?.logs || {});
});

document.getElementById("nodeConfigsSearchInput")?.addEventListener("input", e => {
  state.filters.nodeConfigs = e.target.value || "";
  renderNodeConfigs(state.overview?.nodeConfigs || {});
});

document.getElementById("saveRouteBtn").addEventListener("click", async () => {
  const box = document.getElementById("routesMessage");
  try {
    const serverId = document.getElementById("routeServerIdInput").value.trim();
    const proxyTarget = document.getElementById("routeProxyTargetInput").value.trim();
    if (!serverId || !proxyTarget) throw new Error(state.locale === "zh-CN" ? "服务器 ID 和代理目标不能为空" : "Server ID and target required");
    await api("/api/routes", { method: "POST", body: JSON.stringify({ serverId, proxyTarget }) });
    await reloadRoutes();
    setMessage(box, "success", `Saved: ${serverId} → ${proxyTarget}`);
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("replaceRoutesBtn").addEventListener("click", async () => {
  const box = document.getElementById("routesMessage");
  try {
    const raw = document.getElementById("routesBulkInput").value.trim();
    if (!raw) throw new Error(state.locale === "zh-CN" ? "请输入 JSON" : "Enter JSON");
    await api("/api/routes", { method: "PUT", body: raw });
    await reloadRoutes();
    setMessage(box, "success", t("routesReplaced"));
  } catch (error) {
    setMessage(box, "error", error.message);
    reportGlobalError(error);
  }
});

document.getElementById("loadRoutesJsonBtn").addEventListener("click", () => {
  const shared = state.overview?.routes?.shared || {};
  document.getElementById("routesBulkInput").value = JSON.stringify({ routes: shared }, null, 2);
});

document.getElementById("playerSearchBtn").addEventListener("click", searchPlayerTransfer);

// ─── Player transfer search ──────────────────────────────────────────────────

async function searchPlayerTransfer() {
  const name = document.getElementById("playerSearchInput").value.trim();
  const target = document.getElementById("playerTransferResult");
  if (!name) {
    target.innerHTML = `<div class="message show error">${escapeHtml(t("transferNeedPlayer"))}</div>`;
    return;
  }
  try {
    const data = await api(`/api/transfers/player?player=${encodeURIComponent(name)}`);
    if (data.enabled === false) {
      target.innerHTML = `<div class="message show error">${state.locale === "zh-CN" ? "转服诊断模块未启用" : "Transfer diagnostics disabled"}</div>`;
      return;
    }
    if (!data.found) {
      target.innerHTML = `<div class="message show error">${state.locale === "zh-CN" ? "未找到: " : "Not found: "}${escapeHtml(name)}</div>`;
      return;
    }
    const inspection = data.inspection || {};
    const handoff = inspection.handoff || null;
    const session = inspection.sessionTransferState || null;
    const history = Array.isArray(inspection.recentHistory) ? inspection.recentHistory : [];
    const actions = Array.isArray(inspection.suggestedActions) ? inspection.suggestedActions : [];
    const recovery = inspection.recoveryStatus || "-";
    const recoveryClass = String(recovery).includes("FAILED") || String(recovery).includes("EXPIRED") ? "danger" : (String(recovery).includes("COMPLETED") || String(recovery).includes("RECOVERED") ? "success" : "");
    target.innerHTML = `
      <div class="panel" style="margin-top:12px">
        <div class="panel-header">
          <div><h3 style="margin:0;font-size:16px">${state.locale === "zh-CN" ? "玩家转服诊断" : "Player Transfer Diagnostics"}</h3><div class="subtitle">${escapeHtml(inspection.playerName || name)} · ${escapeHtml(inspection.playerId || "-")}</div></div>
          <div class="chips-row"><span class="chip ${recoveryClass}">${escapeHtml(recovery)}</span><span class="chip">${state.locale === "zh-CN" ? "预备" : "Prepared"}: ${escapeHtml(inspection.localPreparedTransfer)}</span></div>
        </div>
        <div class="grid-auto">
          ${kvCard([
            [state.locale === "zh-CN" ? "玩家" : "Player", inspection.playerName || name],
            [state.locale === "zh-CN" ? "UUID" : "UUID", inspection.playerId || "-"],
            [state.locale === "zh-CN" ? "恢复" : "Recovery", recovery],
            [state.locale === "zh-CN" ? "预备" : "Prepared", inspection.localPreparedTransfer]
          ], true)}
          ${kvCard([
            ["Handoff", handoff?.status || "-"],
            [state.locale === "zh-CN" ? "来源" : "Source", handoff?.sourceServerId || "-"],
            [state.locale === "zh-CN" ? "目标" : "Target", handoff?.targetServerId || "-"],
            [state.locale === "zh-CN" ? "过期" : "Expires", formatTime(handoff?.expiresAt)]
          ])}
          ${kvCard([
            [state.locale === "zh-CN" ? "令牌" : "Token", session?.transferToken || "-"],
            [state.locale === "zh-CN" ? "会话来源" : "Session src", session?.sourceServerId || "-"],
            [state.locale === "zh-CN" ? "会话目标" : "Session tgt", session?.targetServerId || "-"],
            [state.locale === "zh-CN" ? "已预备" : "Has prepared", session?.hasPreparedTransfer]
          ])}
        </div>
        <div class="grid-2 mt-sm">
          <div class="info-card">
            <strong style="font-size:13px">${state.locale === "zh-CN" ? "建议动作" : "Suggested Actions"}</strong>
            ${actions.length ? `<ul style="margin:8px 0 0 18px;padding:0;line-height:1.7;font-size:13px">${actions.map(a => `<li>${escapeHtml(a)}</li>`).join("")}</ul>` : `<div class="text-muted" style="margin-top:8px;font-size:13px">${state.locale === "zh-CN" ? "暂无建议" : "No suggestions"}</div>`}
          </div>
          <div class="info-card">
            <strong style="font-size:13px">Handoff ${state.locale === "zh-CN" ? "详情" : "Details"}</strong>
            <div class="stack" style="margin-top:8px">
              <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "请求 ID" : "Request ID"}</span><span class="kv-value">${escapeHtml(handoff?.requestId || "-")}</span></div>
              <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "世界" : "World"}</span><span class="kv-value">${escapeHtml(handoff?.targetWorld || "-")}</span></div>
              <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "坐标" : "Coords"}</span><span class="kv-value">${escapeHtml(handoff ? `${handoff.x}, ${handoff.y}, ${handoff.z}` : "-")}</span></div>
              <div class="kv-row"><span class="kv-label">${state.locale === "zh-CN" ? "失败原因" : "Failure"}</span><span class="kv-value">${escapeHtml(handoff?.failureReason || "-")}</span></div>
            </div>
          </div>
        </div>
        <table class="mt-sm"><thead><tr><th>${escapeHtml(t("thTime"))}</th><th>${escapeHtml(t("thEvent"))}</th><th>${escapeHtml(t("thStatus"))}</th><th>${escapeHtml(t("thPlayer"))}</th><th>${escapeHtml(t("thTarget"))}</th><th>${escapeHtml(t("thContent"))}</th></tr></thead><tbody>${history.length ? history.map(e => `<tr><td style="white-space:nowrap">${escapeHtml(formatTime(e.createdAt))}</td><td>${escapeHtml(e.eventType || "-")}</td><td>${escapeHtml(e.status || "-")}</td><td>${escapeHtml(e.sourceServerId || "-")}</td><td>${escapeHtml(e.targetServerId || "-")}</td><td>${escapeHtml(e.detail || "-")}</td></tr>`).join("") : `<tr><td colspan="6" class="text-muted" style="text-align:center">${escapeHtml(t("emptyTransferHistory"))}</td></tr>`}</tbody></table>
      </div>`;
  } catch (error) {
    target.innerHTML = `<div class="message show error">${escapeHtml(error.message)}</div>`;
    reportGlobalError(error);
  }
}

// ─── Mobile menu ─────────────────────────────────────────────────────────────

document.getElementById("menuToggle")?.addEventListener("click", () => {
  document.querySelector(".sidebar").classList.toggle("open");
});

// ─── Init ────────────────────────────────────────────────────────────────────

switchTab(window.location.hash.replace("#", "") || "dashboard", false);

state.locale = localStorage.getItem("crossserver.locale") || "zh-CN";
if (localeSelect) localeSelect.value = state.locale;
applyLocale();

tokenInput.value = savedToken();
actorInput.value = savedActor();

if (tokenInput.value) {
  loadOverview()
    .then(() => setMessage(authMessage, "success", t("authLoadedFromSaved")))
    .catch(error => {
      setMessage(authMessage, "error", error.message);
      reportGlobalError(error);
    });
} else {
  setMessage(authMessage, "error", t("authNeedToken"));
}
