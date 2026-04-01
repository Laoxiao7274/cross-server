# 架构概览

## 整体架构

CrossServer 采用分层架构设计：

```
┌─────────────────────────────────────────────────┐
│                   命令 & UI 层                     │
│   Commands (crossserver/home/economy/login)      │
│   Chest GUI Menus (Homes/Transfer/Route)         │
├─────────────────────────────────────────────────┤
│                   业务服务层                        │
│   EconomyService / HomesSyncService / AuthService │
│   PlayerInventorySyncService / PlayerStateSyncService │
│   CrossServerTeleportService                     │
├─────────────────────────────────────────────────┤
│                   核心引擎层                        │
│   SyncService（同步 & 无效化广播）                   │
│   SessionService（分布式会话锁）                     │
├─────────────────────────────────────────────────┤
│                   基础设施层                        │
│   StorageProvider (MySQL + HikariCP)             │
│   MessagingProvider (Redis PubSub / Noop)        │
├─────────────────────────────────────────────────┤
│                   启动层                           │
│   CrossServerPlugin (JavaPlugin 生命周期)          │
└─────────────────────────────────────────────────┘
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
| `player-state` | JSON | 血量、饥饿、经验等 |
| `homes` | JSON | 家园位置列表 |
| `auth.profile` | JSON | 认证账号信息 |
| `auth.ticket` | JSON | 跨服免重登票据 |
| `teleport.handoff` | JSON | 跨服传送交接数据 |

## 跨服传送流程

```
Server A                                      Server B
  │                                              │
  ├─ 玩家执行 /home base                          │
  ├─ flush inventory + state + homes             │
  ├─ SessionService.prepareTransfer()            │
  ├─ AuthService.issueTicket()                   │
  ├─ 保存 TeleportHandoff (PENDING)              │
  ├─ PluginMessage -> Velocity                   │
  │                                              │
  │        Velocity 切服                         │
  │                                              │
  │                                              ├─ PlayerJoinEvent
  │                                              ├─ TeleportArrivalListener 检查 handoff
  │                                              ├─ SessionService.claimSession()
  │                                              ├─ 传送到目标坐标
  │                                              ├─ load inventory + state + homes
```

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

详细接口说明见 [API 文档](api.md)。

## 验证建议

### 单服验证

1. 启动服务器，执行 `/crossserver status` 确认加载正常
2. `/sethome main` → `/homes` → `/home main` 测试家园
3. `/register test123 test123` → `/login test123` 测试认证
4. `/economy balance` 测试经济
5. `/crossserver reload` 测试热重载

### 多服验证

1. 两台子服连接同一 MySQL 和 Redis
2. `/crossserver nodes` 确认所有节点在线
3. 在 Server A `/sethome cross`，切到 Server B `/home cross` 测试跨服家园
4. 在 Server A `/economy deposit <玩家> 100`，切到 Server B `/economy balance` 确认余额同步
5. `/crossserver transfer info <玩家>` 确认传送状态正常推进
