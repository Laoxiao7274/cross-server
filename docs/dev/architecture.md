# 架构概览

## 整体架构

CrossServer 采用分层架构设计：

```
┌──────────────────────────────────────────────────────┐
│                     Web 面板层                       │
│   WebPanelServer (HTTP) / WebPanelDataService        │
│   WebPanelClusterService / WebPanelLogService        │
├──────────────────────────────────────────────────────┤
│                     配置中心层                        │
│   ConfigCenterService / NodeConfigSyncService        │
│   SharedModuleConfigService / RouteTableService      │
├──────────────────────────────────────────────────────┤
│                     命令 & UI 层                      │
│   Commands (crossserver/home/warp/tpa/economy/login) │
│   Chest GUI Menus (Homes/Warp/Transfer/Route)        │
├──────────────────────────────────────────────────────┤
│                     业务服务层                        │
│   EconomyService / HomesSyncService / WarpService    │
│   TeleportRequestService / PlayerLocationService     │
│   AuthService / PlayerInventorySyncService           │
│   PlayerStateSyncService / PlayerPermissionSyncService│
│   CrossServerTeleportService                         │
├──────────────────────────────────────────────────────┤
│                     核心引擎层                        │
│   SyncService（同步 & 无效化广播）                    │
│   SessionService（分布式会话锁）                      │
├──────────────────────────────────────────────────────┤
│                     基础设施层                        │
│   StorageProvider (MySQL + HikariCP)                │
│   MessagingProvider (Redis PubSub / Noop)           │
├──────────────────────────────────────────────────────┤
│                     启动层                           │
│   CrossServerPlugin (JavaPlugin 生命周期)            │
└──────────────────────────────────────────────────────┘
```

## 核心组件

### SyncService — 同步引擎

数据同步的核心，负责：

- 命名空间注册与校验
- 玩家数据和全局数据的读写
- 变更时通过 MessagingProvider 广播无效化消息
- 追踪本地无效化状态（`scope|targetId|namespace|version`）
- 分发到注册的 `SyncListener` 回调

### SessionService — 分布式会话锁

防止同一玩家在两个服务器上同时被写入：

1. `tryAcquireSession()` — 使用 `SELECT FOR UPDATE` 获取锁
2. `prepareTransfer()` — 生成 transfer token，准备跨服移交
3. `tryClaimTransferredSession()` — 目标服务器用 token 原子获取会话
4. `refreshLocalSessions()` — 定时心跳延长锁 TTL

### StorageProvider — 存储层

MySQL 实现，使用 HikariCP 连接池。关键设计：

- `updateEconomyBalance()` 使用事务 + SELECT FOR UPDATE 保证原子性
- 乐观重试处理约束冲突
- 自动建表，零人工干预

### MessagingProvider — 消息层

Redis PubSub 实现跨服实时通信。消息格式为 JSON（Jackson 序列化 `SyncMessage`），包含：

- `source` — 发送方服务器 ID
- `targetType` / `targetId` — 目标（ALL / PLAYER / SERVER）
- `namespace` — 数据命名空间
- `action` — 操作类型（INVALIDATE / SESSION_*）

`NoopMessagingProvider` 是单服模式的空实现，不产生任何网络通信。

### WebPanelServer — Web 面板 HTTP 服务

基于 `com.sun.net.httpserver` 的轻量 HTTP 服务器：

- 只有 `web-panel.master-server-id` 指定的节点会绑定 HTTP 端口
- 其他开启 `web-panel.enabled` 的节点作为受管节点，只上报成员状态
- 内置 token 鉴权（`X-CrossServer-Token` 请求头）
- 面板页面为单文件 HTML，直接返回前端渲染
- 支持安全重载：`requestReload()` 使用 `AtomicBoolean` 防止并发重载，异步排队执行

### ConfigCenterService — 配置中心

高层配置文档管理，供第三方插件接入共享配置：

- `registerDocument(namespace, dataKey)` — 注册配置文档
- `loadDocument(namespace, dataKey)` — 加载配置文档（含 payload + 元信息）
- `loadEntry(namespace, dataKey)` — 加载配置条目（含 schemaVersion / source / summary）
- `saveDocument(namespace, dataKey, update)` — 保存配置文档（自动补全元信息）
- `registerDocumentListener(namespace, dataKey, listener)` — 监听文档变更

## 数据表

| 表名 | 用途 |
|------|------|
| `player_session` | 分布式会话锁，含 transfer token |
| `player_snapshot` | 玩家数据（按 UUID + namespace 存储 JSON） |
| `global_snapshot` | 全局数据（按 namespace + data_key 存储 JSON） |
| `player_identity` | UUID 与玩家名称映射 |
| `economy_transaction` | 经济交易流水日志 |
| `transfer_history` | 跨服传送事件日志 |
| `node_status` | 集群节点心跳记录 |

## 命名空间

玩家数据按命名空间隔离存储在 `player_snapshot` 表中：

| 命名空间 | 数据格式 | 用途 |
|----------|----------|------|
| `economy` | JSON | 余额 |
| `inventory` | Base64 | 背包内容（ItemStack[]） |
| `enderchest` | Base64 | 末影箱内容 |
| `player-state` | JSON | 血量、饥饿、经验、等级、游戏模式 |
| `player-permissions` | JSON | 玩家权限快照（当前仅 `crossserver.*`） |
| `player-location` | JSON | 玩家位置快照（跨服 TPA） |
| `homes` | JSON | 家园位置列表 |
| `auth.profile` | JSON | 认证账号信息 |
| `auth.ticket` | JSON | 跨服免重登票据 |
| `teleport.handoff` | JSON | 跨服传送交接数据 |
| `teleport.rollback.inventory` | Base64 | 失败回滚用背包快照 |
| `teleport.rollback.enderchest` | Base64 | 失败回滚用末影箱快照 |
| `teleport.rollback.state` | JSON | 失败回滚用玩家状态 |

全局数据保存在 `global_snapshot`：

| 命名空间 | data_key | 用途 |
|----------|----------|------|
| `warps` | `teleport.warps` | 全局 Warp 列表 |
| `teleport.requests` | `tpa.requests` | 跨服 TPA 请求 |
| `route-table` | 路由相关 key | 服务器路由配置 |
| `cluster.config` | `modules.toggles` | 集群共享模块开关 |
| `node.config` | 各节点 `serverId` | 节点配置快照（messaging / webPanel / modules 白名单字段） |
| `web.panel.log` | 各节点 `serverId` | 节点插件日志同步 |

## 配置中心关键服务

### NodeConfigSyncService — 节点配置同步

管理跨节点的配置快照发布与变更申请：

- `publishLocalSnapshot(configuration)` — 将本地可编辑配置快照发布到 `node.config` 命名空间
- `loadClusterNodeConfigs()` — 加载所有节点的配置快照与在线状态
- `loadNodeConfigDetail(serverId)` — 加载指定节点的配置详情
- `requestApply(serverId, changes, actor)` — 主控节点提交变更申请，目标节点收到后写入 config.yml 并排队重载

配置快照通过 `NodeLocalConfigService` 从当前 `PluginConfiguration` 导出，仅包含白名单字段（messaging / webPanel / modules），不暴露数据库、代理等敏感配置。

### WebPanelClusterService — 面板集群管理

管理 Web 面板主控/受管节点的集群登记与选举：

- 主控节点在启动时注册到共享配置
- 受管节点只上报成员状态
- 心跳 + 租约机制确保过期节点自动剔除

### WebPanelLogService — 日志同步

各节点通过共享配置中心的 `web.panel.log` 命名空间同步插件日志，主控节点在 Web 面板的日志中心页面展示各节点日志，便于集群排障。

## 生命周期装配

`CrossServerPlugin` 在启动时完成以下装配：

- 初始化 `StorageProvider`、`MessagingProvider`、`SyncService`、`SessionService`
- 读取本地配置并与共享路由、共享模块配置合并
- 注册全部 namespace
- 按模块开关选择性装配 homes / warps / tpa / auth / permissions 等服务
- 创建 `NodeConfigSyncService` 并发布本地配置快照
- 创建 `WebPanelClusterService`、`WebPanelLogService`
- 创建 `WebPanelDataService`（注入所有上述服务）并启动 `WebPanelServer`
- 注册 listener、命令、GUI、自动保存和恢复任务
- 启动 Web 面板心跳定时任务，周期性执行主控选举、日志发布、配置快照刷新

### 安全重载

`/crossserver reload`、路由菜单的"重载本节点"、Web 面板的"重载本节点"都走同一套安全排队逻辑：

1. `requestReload(actor, source)` 使用 `AtomicBoolean.compareAndSet` 防止并发重载
2. 重载任务在 Bukkit 主线程异步排队执行
3. 重载期间：取消心跳 → 注销面板成员 → 停止面板服务 → 关闭日志/配置同步 → 清理节点状态 → 关闭其他服务 → 重新初始化

### 自动保存

定时任务按 `session.heartbeatSeconds` 间隔自动保存在线玩家数据：

- `inventorySyncService.savePlayerData(player)` — 背包
- `playerStateSyncService.savePlayerState(player)` — 玩家状态（血量、饥饿、经验、等级、游戏模式）
- `playerPermissionSyncService.savePermissions(player)` — 权限快照
- `homesSyncService.savePlayerHomes(player)` — 家园位置

其中权限同步模块当前实现为：

- `PlayerPermissionSyncService` 负责保存、读取、应用权限快照
- `PermissionSyncAdapter` 抽象“如何捕获/应用权限”
- `NoopPermissionSyncAdapter` 作为默认实现，使用 `PermissionAttachment`
- 当前仅管理 `crossserver.*` 权限，避免覆盖外部权限系统的全局状态

## 跨服传送入口

当前已接入的跨服入口：

- `/home`
- `/warp`
- `/tpa`
- `/tpahere`
- 其他通过 API 调用 `requestTeleport(...)` 的入口

### 统一主链路

1. 发起入口调用 `CrossServerTeleportService.requestTeleport()`
2. 保存 inventory / ender chest / player-state，并生成 rollback 快照
3. 在切服前 flush 当前玩家数据（含可选权限快照）
4. 写入 session transfer 与 `teleport.handoff`
5. 通过代理切服
6. 目标服由 `TeleportArrivalListener` + `tryConsumeArrival()` 消费 handoff
7. 落点成功后清理 rollback；失败时标记 handoff 并尝试回滚

### 稳定性补强

- 插件关闭时，`protectOnShutdown()` 会收口 `PENDING/PREPARING` handoff
- 插件关闭时，`clearLocalNodeStatus()` 会清理本节点在 `node_status` 表中的记录
- 插件关闭时，`webPanelClusterService.unregisterLocalMember()` 会注销面板集群成员
- 跨服失败会回滚 inventory / ender chest / player-state
- 玩家下次加入时可通过 `recoverRollbackOnJoin(...)` 做补偿恢复
- `reconcilePendingTransfers()` 会定期检查并修复超时/悬空状态
- 新增字段的旧快照通过手工 JSON 解析保持兼容
- 共享模块配置与共享路由快照新增 `source`（来源节点）和 `summary`（变更摘要）元信息字段

## GUI 结构

当前菜单体系采用统一的 `MenuListener + MenuHolder + MenuService` 模式：

- `HomesMenuService` / `HomesMenuHolder`
- `WarpMenuService` / `WarpMenuHolder`
- `TransferAdminMenuService` / `TransferAdminMenuHolder`
- `RouteConfigMenuService` / `RouteConfigMenuHolder`

Warp GUI 与 Homes GUI 采用相同的 54 格分页结构：

- 主区域 45 格展示位置条目
- 底栏 9 格用于分页、统计、说明与关闭/管理入口
- 点击事件统一由 `MenuListener` 在顶部背包层处理

## TPA 设计

TPA 不再局限于本服在线玩家：

- `TeleportRequestService` 负责把请求写入全局快照
- `PlayerLocationService` 负责同步玩家位置快照并校验新鲜度
- `TpaCommand` 负责解析本服/跨服目标、发起请求、处理接受/拒绝/取消
- 接受跨服请求后，最终仍走统一的跨服 handoff 主链路

## 其他插件接入

通过 Bukkit ServicesManager 获取 API：

```java
CrossServerApi api = Bukkit.getServicesManager().load(CrossServerApi.class);
```

API 提供的能力：

- 注册自定义命名空间
- 读写玩家数据 / 全局数据
- 注册同步监听器（带可卸载句柄）
- 会话锁管理
- 跨服传送请求
- 经济服务访问
- 配置中心文档（注册、读写、监听变更）
- 传送诊断与管理

详细接口说明见 [API 文档](api.md)。

## 验证建议

### 单服验证

1. 启动服务器，执行 `/crossserver status` 确认加载正常
2. `/sethome main` → `/homes` → `/home main` 测试家园
3. `/setwarp spawn` → `/warp` → 点击 GUI 测试 Warp
4. `/register test123 test123` → `/login test123` 测试认证
5. `/economy balance` 测试经济
6. 开启 `permissions` 模块后测试 `crossserver.*` 权限同步
7. `/crossserver reload` 测试热重载

### 多服验证

1. 两台子服连接同一 MySQL 和 Redis
2. `/crossserver nodes` 确认所有节点在线
3. 在 Server A `/sethome cross`，切到 Server B `/home cross` 测试跨服家园
4. 在 Server A `/setwarp crossspawn`，Server B `/warp crossspawn` 测试跨服 Warp
5. 在 Server A 对 Server B 玩家执行 `/tpa` 或 `/tpahere`，测试跨服请求与反馈
6. 切换游戏模式后跨服，验证 `player-state` 中的游戏模式同步
7. 启用权限同步后跨服，验证 `crossserver.*` 权限是否保持一致
8. 人为制造目标服世界缺失 / handoff 超时，验证失败回滚与诊断状态
