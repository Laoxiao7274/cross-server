# 配置指南

配置文件位置：`plugins/cross-server/config.yml`

## 完整配置示例

```yaml
server:
  id: "server-1"
  cluster: "my-cluster"

database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "root"
  password: "your-password"
  maximum-pool-size: 10

messaging:
  enabled: true
  redis-uri: "redis://127.0.0.1:6379/0"
  channel: "cross-server:sync"

session:
  lock-seconds: 30
  heartbeat-seconds: 10
  kick-message: "你的跨服会话正在同步中，请稍后重试"

node:
  heartbeat-seconds: 15
  offline-seconds: 45

teleport:
  handoff-seconds: 30
  arrival-check-delay-ticks: 10
  cooldown-seconds: 10
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"

modules:
  auth: true
  homes: true
  warps: true
  tpa: true
  route-config: true
  transfer-admin: true
  economy-bridge: true
  permissions: false

web-panel:
  enabled: false
  host: "127.0.0.1"
  port: 8765
  token: "change-this-token"
  master-server-id: "server-1"
  cluster-lease-seconds: 30
  cluster-heartbeat-seconds: 10
```

## 配置项详解

### server — 服务器标识

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `id` | String | `server-1` | 当前节点的唯一标识，集群内不能重复 |
| `cluster` | String | `default` | 集群名称，同一集群的子服应保持一致 |

### database — 数据库

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `jdbc-url` | String | — | MySQL JDBC 连接串，必须包含数据库名 |
| `username` | String | — | 数据库用户名 |
| `password` | String | — | 数据库密码 |
| `maximum-pool-size` | Int | `10` | HikariCP 连接池最大连接数 |

> 插件启动时会**自动创建所有数据表**，无需手动建表。但数据库（如 `cross_server`）需提前创建。

### messaging — 跨服消息

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | Boolean | `false` | 是否启用 Redis PubSub 实时广播 |
| `redis-uri` | String | `redis://127.0.0.1:6379/0` | Redis 连接串 |
| `channel` | String | `cross-server:sync` | PubSub 频道名，同一集群的子服应保持一致 |

> 关闭 `messaging.enabled` 后插件仍可运行，但跨服实时通知（如 TPA 请求更新、Warp 变更广播）将不可用。

### session — 会话管理

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `lock-seconds` | Int | `30` | 会话锁超时时间（秒），防止同玩家在多服写入 |
| `heartbeat-seconds` | Int | `10` | 本地会话心跳间隔（秒） |
| `kick-message` | String | `你的跨服会话正在同步中，请稍后重试` | 会话冲突时踢出玩家的消息 |

### node — 节点监控

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `heartbeat-seconds` | Int | `15` | 节点心跳间隔（秒） |
| `offline-seconds` | Int | `45` | 节点超过此秒数未心跳则视为离线 |

### teleport — 跨服传送

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `handoff-seconds` | Int | `30` | handoff 超时时间（秒），超时后目标服不再接受 |
| `arrival-check-delay-ticks` | Int | `10` | 玩家到达后延迟多少 tick 再检查 handoff |
| `cooldown-seconds` | Int | `10` | 同一玩家两次跨服传送的冷却时间（秒） |

#### teleport.gateway — 传送网关

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `type` | String | `proxy-plugin-message` | 网关类型，当前仅 `proxy-plugin-message` 可用 |
| `plugin-message-channel` | String | `BungeeCord` | 插件消息通道名（Velocity 用 `BungeeCord`） |
| `connect-subchannel` | String | `Connect` | 连接子频道名 |
| `server-map` | Map | `{}` | 本服 ID → 代理中注册的服务器名映射（大小写敏感） |

> `server-map` 的 value 必须和代理中注册的服务器名**完全一致**。

### modules — 模块开关

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `auth` | Boolean | `true` | 登录认证模块（注册/登录/改密 + 跨服免重登 Ticket） |
| `homes` | Boolean | `true` | 跨服家园（`/sethome` `/home` `/homes`） |
| `warps` | Boolean | `true` | 全局 Warp（`/setwarp` `/warp` `/delwarp` + GUI） |
| `tpa` | Boolean | `true` | 跨服 TPA（`/tpa` `/tpahere` `/tpaccept` `/tpdeny`） |
| `route-config` | Boolean | `true` | 路由配置管理（`/crossserver route` + GUI） |
| `transfer-admin` | Boolean | `true` | 转服诊断管理（`/crossserver transfer` + GUI） |
| `economy-bridge` | Boolean | `true` | 经济桥接（兼容 Vault，跨服共享余额） |
| `permissions` | Boolean | `false` | 权限同步（仅同步 `crossserver.*` 权限节点） |

> 模块开关支持通过集群共享配置中心统一覆盖。本地 config.yml 中的值是"默认值"，可通过 `/crossserver modules set` 或 Web 面板设置"共享覆盖"，最终生效值 = 共享覆盖 ?? 本地默认。

### web-panel — 内置 Web 面板

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | Boolean | `false` | 是否让当前节点加入 Web 面板集群登记 |
| `host` | String | `127.0.0.1` | 主控节点绑定的 HTTP 地址 |
| `port` | Int | `8765` | 主控节点监听的 HTTP 端口 |
| `token` | String | `change-this-token` | 访问令牌，请务必改成强随机值 |
| `master-server-id` | String | 当前 `server.id` | 指定哪个节点是主控节点（通常填大厅服） |
| `cluster-lease-seconds` | Int | `30` | 面板成员登记过期时间（秒） |
| `cluster-heartbeat-seconds` | Int | `10` | 面板成员刷新状态间隔（秒） |

**集群行为：**

- 开启 `web-panel.enabled: true` 的节点会把自己登记到共享配置中心
- 只有 `web-panel.master-server-id` 对应的节点会真正监听 HTTP 端口
- 其他开启节点作为受管节点，只上报成员状态，不会重复绑定端口
- 节点正常关闭时会主动注销自己的面板成员记录
- 心跳定时任务会周期性执行主控选举、日志发布、配置快照刷新
- 若主控节点未开启或离线，面板页面不会由其他节点自动接管

**访问面板：**

浏览器访问 `http://host:port/`，输入 `X-CrossServer-Token` 后即可使用。

面板包含以下标签页：

| 标签页 | 功能 |
|--------|------|
| 仪表盘 | 集群状态总览、节点列表、面板成员信息 |
| 模块 | 查看并修改共享模块开关（local / shared / effective） |
| 路由 | 查看并修改共享路由（新增 / 整体覆盖 / 删除） |
| 配置文档 | 查看已注册配置文档及 payload |
| 节点配置 | 查看各节点配置快照，在线编辑 messaging / webPanel / modules 并提交 |
| 日志中心 | 按节点查看 CrossServer 插件同步日志 |
| 转服诊断 | 查看最近转服记录，按玩家名查询详细诊断 |

**注意：**

- 在面板中保存共享模块开关或共享路由后，只是写入共享配置中心；当前节点通常仍需触发一次本节点 reload 才会立即生效
- `/crossserver reload`、路由菜单里的"重载本节点"、面板内的"重载本节点"现在都使用同一套安全排队式 reload 逻辑（`AtomicBoolean` 防并发）
- 面板触发重载时会短暂断开，然后自动尝试重连

### 节点配置远程管理

主控节点可在"节点配置"标签页中，查看各子服上报的配置快照，并在线修改以下白名单字段：

| 分类 | 可编辑字段 | 说明 |
|------|-----------|------|
| `messaging` | `enabled`, `redisUri`, `channel` | Redis 消息层配置 |
| `webPanel` | `enabled`, `host`, `port`, `masterServerId`, `token` | Web 面板配置（token 留空表示不修改） |
| `modules.*` | `auth`, `homes`, `warps`, `tpa`, `routeConfig`, `transferAdmin`, `economyBridge`, `permissions` | 各模块开关 |

**工作流程：**

1. 目标节点启动后自动发布配置快照到集群配置中心（命名空间 `node.config`）
2. 主控节点在面板中查看快照并编辑白名单字段
3. 主控节点提交"申请"到目标节点
4. 目标节点收到申请后，将变更写入本地 `config.yml` 并排队重载

**安全限制：**

- 只能修改白名单内的字段，不会意外覆盖数据库连接、代理配置等敏感信息
- 每次申请只影响一个目标节点
- 申请状态可在面板中实时追踪（pending -> applying -> applied / failed）

### 日志中心

Web 面板的"日志中心"标签页可按节点查看 CrossServer 插件同步到配置中心的日志，便于集群排障。日志通过共享配置中心的 `web.panel.log` 命名空间同步，主控和受管节点均可参与。
