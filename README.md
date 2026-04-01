# cross-server

一个基于 Paper 的跨服数据同步插件，当前已经包含 MySQL 持久化、Redis 广播、玩家会话锁、经济模块、背包同步、玩家状态同步、跨服 homes 数据、登录认证与短时免重登能力。

## 文档站

已接入 MkDocs + Material 文档结构。

本地预览：

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

构建静态站点：

```bash
pip install -r requirements-docs.txt
mkdocs build
```

GitHub Pages 部署会使用 `.github/workflows/docs.yml`。


## 功能概览

- MySQL 持久化存储
- Redis 跨服广播与失效通知
- 玩家会话锁，避免同一玩家被多服同时写入
- 命名空间机制，便于其他插件接入
- `economy` 跨服经济模块
- 玩家背包 / 末影箱跨服快照同步
- 玩家基础状态跨服同步（血量、饥饿、饱和度、经验、等级等）
- `homes` 跨服家园数据同步
- `auth` 登录认证、未登录限制、短时跨服免重登
- `teleport.handoff` 跨服传送基础设施（准备/到达消费，已接 session baton-pass 与 proxy plugin-message gateway）
- Vault Economy 兼容层
- 节点心跳上报与集群节点状态查询
- `CrossServerApi` 通过 Bukkit ServicesManager 对外暴露
- `/crossserver reload` 热重载配置与内部服务

## 环境要求

- Java 21
- Paper 1.21+
- MySQL 8+
- Redis（可选，启用跨服广播时需要）

## 安装

1. 将构建后的插件 jar 放入每台子服的 `plugins/` 目录。
2. 确保所有子服使用相同版本的 `cross-server`。
3. 所有子服连接同一个 MySQL。
4. 如需跨服广播，所有子服连接同一个 Redis。
5. 每台子服配置不同的 `server.id`。

## 配置

配置文件路径：`plugins/cross-server/config.yml`

### 先说结论：跨服 HOME / handoff 必须接代理

如果你要使用当前插件的跨服传送能力（包括跨服 `/home`），**前面必须接 Velocity / BungeeCord / Waterfall 这类代理**。

当前插件的跨服切服方式是：

- 子服通过 Bukkit Plugin Message
- 通道：`BungeeCord`
- 子通道：`Connect`
- 把玩家从当前 Paper 子服切到代理配置里的另一个后端服

所以：

- 只开多个独立 Paper，**不接代理**：不能真正跨服切换
- 接了 Velocity，并让玩家通过 Velocity 进入子服：可以正常使用当前跨服传送链路

如果没有代理，你看到“正在连接目标服务器”只表示插件发出了切服请求，不表示一定真的完成了跨服。

### Velocity 部署方式

推荐结构：

```text
玩家
  -> Velocity
      -> server-1 (Paper)
      -> server-2 (Paper)
```

要求：

1. 所有玩家都先进入 Velocity，再由 Velocity 转发到子服
2. 所有子服共用同一个 MySQL
3. 如启用广播，所有子服共用同一个 Redis
4. 每台子服的 `server.id` 必须唯一
5. `teleport.gateway.server-map` 的 value 必须与 Velocity 中的后端服务器名一致

### Velocity 侧示例配置

下面是一个最小可用思路，重点是 **Velocity 里的后端服名** 要和插件路由映射对上。

假设你有两个后端子服：

- Velocity 后端名：`server-1`
- Velocity 后端名：`server-2`

那么 Velocity 的 `velocity.toml` 中后端服务器部分应类似：

```toml
[servers]
server-1 = "127.0.0.1:25566"
server-2 = "127.0.0.1:25567"
try = ["server-1"]
```

如果你实际在 Velocity 中写的是别的后端名，例如：

```toml
[servers]
survival-a = "127.0.0.1:25566"
survival-b = "127.0.0.1:25567"
try = ["survival-a"]
```

那么插件里的 `server-map` 就必须写成：

```yml
teleport:
  gateway:
    server-map:
      server-1: "survival-a"
      server-2: "survival-b"
```

也就是说：

- `server.id`：插件内部节点 ID
- `server-map.key`：插件内部节点 ID
- `server-map.value`：Velocity 后端服务器名

### Velocity 与 Paper 的安全转发

你的 Velocity 和 Paper 还要正确配置代理转发，否则玩家连接链路本身就不完整。

至少要确保：

#### Velocity
- 启用现代转发（modern forwarding）
- 配好 forwarding secret

#### Paper 子服
- 按 Velocity 要求开启代理支持
- Paper 侧 secret 与 Velocity 一致

这一部分请按你当前 Velocity 版本的官方要求配置；核心目标是：

- 玩家必须通过 Velocity 进入子服
- 子服不能被玩家绕过代理直连

### 子服 cross-server 配置示例

#### server-1

```yml
server:
  id: "server-1"
  cluster: "qingchuan"

teleport:
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

#### server-2

```yml
server:
  id: "server-2"
  cluster: "qingchuan"

teleport:
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

### server-map 怎么理解

`server-map` 的含义是：

```text
插件内部目标服 ID -> Velocity 后端服名
```

例如：

```yml
server-map:
  server-1: "server-1"
  server-2: "server-2"
```

表示：

- 当插件要去 `server-1` 时，让代理切到 Velocity 的 `server-1`
- 当插件要去 `server-2` 时，让代理切到 Velocity 的 `server-2`

如果 `server.id` 与 Velocity 后端名不同，也完全没问题，只要映射正确：

```yml
server:
  id: "paper-survival-01"

teleport:
  gateway:
    server-map:
      paper-survival-01: "survival-a"
      paper-survival-02: "survival-b"
```

### 共享路由表说明

当前插件已经支持“本地配置 + 数据库共享覆盖层”的路由合并模型：

- 本地 `config.yml` 中的 `teleport.gateway.server-map` 是默认基线
- 数据库共享配置 `cluster.config / teleport.routes` 可以覆盖同名 `serverId` 的路由
- `/crossserver reload` 后重新读取并生效

所以你可以：

1. 先在每台子服配置一份可工作的本地 `server-map`
2. 后续再通过插件的共享路由能力统一覆盖

### 重复 server.id 保护

当前插件已增加重复节点拦截：

- 如果同 cluster 下已有相同 `server.id` 节点仍在线
- 新节点启动时会直接报错并拒绝继续初始化

这意味着：

- `server.id` 不能重复
- 复制子服目录后，一定要先改 `config.yml` 再启动

### 推荐排查顺序

如果跨服 `/home` 不生效，按这个顺序查：

1. 玩家是不是通过 Velocity 进服，而不是直连 Paper
2. Velocity 的后端服务器名是否与 `server-map.value` 一致
3. 每台子服的 `server.id` 是否唯一
4. 所有子服是否连接同一个 MySQL
5. 所有子服是否连接同一个 Redis（如果启用 messaging）
6. 目标服 world 名是否存在
7. `/crossserver nodes` 是否能看到所有节点在线
8. `/crossserver transfer info <player>` 是否能看到 handoff 状态推进

至少需要检查这些配置：

- `server.id`
- `server.cluster`
- `database.jdbc-url`
- `database.username`
- `database.password`
- `messaging.enabled`
- `messaging.redis-uri`（仅在 `messaging.enabled: true` 时必填）
- `messaging.channel`（仅在 `messaging.enabled: true` 时必填）
- `teleport.gateway.type`
- `teleport.gateway.plugin-message-channel`
- `teleport.gateway.connect-subchannel`
- `teleport.gateway.server-map`
- `session.lock-seconds`
- `session.heartbeat-seconds`
- `session.kick-message`
- `node.heartbeat-seconds`
- `node.offline-seconds`
- `teleport.handoff-seconds`
- `teleport.arrival-check-delay-ticks`

### 节点心跳参数

- `node.heartbeat-seconds`：节点上报一次心跳的间隔
- `node.offline-seconds`：超过这个时间未更新心跳，就会判定为离线

## 启动后会自动完成

- 连接 MySQL
- 初始化数据表
- 连接 Redis（启用时）
- 初始化会话锁
- 初始化同步服务
- 注册经济服务
- 检测 Vault 并注册兼容层（如果存在）
- 通过 Bukkit Service 注册 `CrossServerApi`
- 启动玩家会话心跳任务
- 启动节点心跳任务
- 启动在线玩家数据定时保存任务

## 命令

### crossserver

```text
/crossserver help
/crossserver status
/crossserver nodes [page]
/crossserver node <serverId>
/crossserver transfer <player>
/crossserver transfer help
/crossserver transfer info <player>
/crossserver transfer clear <player>
/crossserver transfer menu <player>
/crossserver transfer history <player>
/crossserver transfer recent [page]
/crossserver transfer reconcile <player>
/crossserver auth inspect <player>
/crossserver auth invalidate <player>
/crossserver auth forcereauth <player>
/crossserver reload
```

权限：

- `crossserver.command`
- `/crossserver` 无参数或 `/crossserver help` 会按当前权限显示可用子命令
- `crossserver.status.view`
- `crossserver.nodes.view`
- `crossserver.node.view`
- `crossserver.transfer.view`：查看 `transfer/info/history`
- `crossserver.transfer.menu`：打开 `transfer menu/recent` 菜单入口
- `crossserver.transfer.clear`：清理卡住的 transfer 状态
- `crossserver.transfer.reconcile`：触发保守 reconcile 修补
- `crossserver.auth.admin`
- `crossserver.reload`

### economy

```text
/economy balance <player>
/economy set <player> <amount>
/economy deposit <player> <amount>
/economy withdraw <player> <amount>
/economy history <player> [limit]
```

权限：

- `crossserver.economy.balance`
- `crossserver.economy.history`
- `crossserver.economy.set`
- `crossserver.economy.deposit`
- `crossserver.economy.withdraw`

### homes

```text
/home [name]
/homes
/sethome <name>
/delhome <name>
/setdefaulthome <name>
```

说明：

- `/homes` 现在会打开 homes 菜单 UI
- `homes` 数据跨服共享，存储在 `player_snapshot` 的 `homes` 命名空间
- 当前服务器上的 home 仍然直接本地传送
- 如果目标 home 位于其他服务器，会进入 `teleport.handoff` 准备流程
- 当前仓库已经具备 handoff 持久化、session baton-pass、proxy plugin-message gateway 与目标服到达消费能力
- `/crossserver help` 或直接 `/crossserver` 会按发送者权限显示可用命令
- `/crossserver transfer <player>` 与 `/crossserver transfer info <player>` 可查看最近一次 handoff 的 requestId、状态、来源/目标服、gateway 发送时间、落点应用时间、prepared transfer 状态与失败原因
- `/crossserver transfer history <player>` 可查看最近 transfer 历史事件
- `/crossserver transfer recent [page]` 可打开带分页的 recent transfer 管理菜单
- `/crossserver transfer reconcile <player>` 可触发一次保守的 transfer reconcile，用于补 ACK、补清理与收敛过期/残留状态
- `transfer.view` 只负责文本诊断与历史查看，`transfer.menu` 负责 recent/menu 菜单入口，`transfer.reconcile` 负责保守修补动作
- transfer 管理菜单现在支持 recent -> detail -> history 三层导航，并展示 session baton / prepared transfer 诊断信息
- 已增加 transfer ACK / recovery 基础状态记录，以及 prepared/baton 残留诊断与保守清理能力
- `CrossServerApi` 已支持其他插件发起“指定玩家 -> 指定服务器/世界/坐标”的跨服传送请求，以及读取 transfer 历史与 auth 管理接口
- home 数据包含 `serverId`

权限：

- `crossserver.homes.list`
- `crossserver.homes.menu`
- `crossserver.homes.teleport`
- `crossserver.homes.set`
- `crossserver.homes.delete`
- `crossserver.homes.default`

### auth

```text
/login <password>
/l <password>
/register <password> <confirm>
/reg <password> <confirm>
/changepassword <old> <new>
```

说明：

- 玩家首次进入会读取 `auth.profile` 和 `auth.ticket`
- 未注册玩家会收到注册提示
- 已注册未登录玩家会收到登录提示
- 玩家可通过命令登录，也可直接在聊天栏输入密码完成登录
- 登录期间会显示 Title、ActionBar、BossBar
- 登录成功后会生成短时 ticket，用于跨服免重登
- 未认证阶段会限制移动、交互、背包、丢弃/拾取、非白名单命令等行为

权限：

- `crossserver.auth.login`
- `crossserver.auth.register`
- `crossserver.auth.changepassword`

## 数据结构

插件会自动创建这些表：

- `player_session`
- `player_snapshot`
- `global_snapshot`
- `player_identity`
- `economy_transaction`
- `node_status`

其中：

- 玩家经济数据保存在 `player_snapshot`，命名空间为 `economy`
- 玩家背包保存在 `player_snapshot`，命名空间为 `inventory`
- 玩家末影箱保存在 `player_snapshot`，命名空间为 `enderchest`
- 玩家基础状态保存在 `player_snapshot`，命名空间为 `player-state`
- 玩家家园数据保存在 `player_snapshot`，命名空间为 `homes`
- 玩家登录资料保存在 `player_snapshot`，命名空间为 `auth.profile`
- 玩家免重登票据保存在 `player_snapshot`，命名空间为 `auth.ticket`
- 玩家跨服传送 handoff 保存在 `player_snapshot`，命名空间为 `teleport.handoff`
- 玩家会话接力仍以 `player_session` 为权威，使用 transfer reservation 字段完成 session baton-pass
- 经济流水保存在 `economy_transaction`
- 节点心跳保存在 `node_status`

## 其他插件接入

推荐方式：

1. 通过 Bukkit `ServicesManager` 获取 `CrossServerApi`
2. 注册自己的命名空间
3. 通过 `CrossServerApi` 读取或保存数据
4. 如有需要，为指定命名空间注册同步监听器

说明：

- 保存玩家或全局数据后会自动广播同步消息
- 远端节点会收到失效通知，并可通过同步监听器处理
- MySQL 仍然是最终权威数据源，Redis 主要负责通知

当前可直接参考 `economy`、`homes`、`auth`、`teleport` 模块作为接入模板。

## 验证建议

### 单服验证

1. 启动服务器
2. 执行 `/crossserver status`
3. 执行 `/sethome main`
4. 执行 `/homes` 和 `/home main`
5. 新玩家进入服务器，检查注册 Title / ActionBar / BossBar 是否正常显示
6. 执行 `/register <password> <confirm>` 或直接使用 `/login <password>`
7. 执行 `/economy deposit <玩家> 10`
8. 执行 `/crossserver reload`，再重复一次状态、homes、auth、economy 验证

### 多服验证

1. 准备两台子服，连接同一个 MySQL 和 Redis
2. 分别启动两台服
3. 执行 `/crossserver nodes` 查看节点状态
4. 在 A 服设置 home，去 B 服执行 `/homes` 检查是否能读到同一份数据
5. 如果 home 指向其他服，检查 `/home <name>` 或 homes 菜单左键是否开始真实代理切服
6. 在源服或目标服执行 `/crossserver transfer info <player>` 或 `/crossserver transfer menu <player>`，检查 requestId、状态推进、gateway 发送时间、落点应用时间与失败原因是否可见
7. 检查 prelogin 是否可通过 session baton-pass，join 后是否消费 `teleport.handoff` 并落到正确位置
8. 检查 source/target 服的 Title、ActionBar、Sound 提示是否正常
9. 在 A 服完成登录后，短时间切到 B 服，检查是否触发跨服免重登
10. ticket 过期后再次切服，检查是否重新要求登录
11. 在一台服修改玩家金币，在另一台服查询余额和流水
12. 检查背包、末影箱、玩家状态是否同步恢复
13. 检查节点离线状态是否按 `node.offline-seconds` 生效

## 当前边界

- `homes` 已经跨服共享数据，且跨服 home 会走 `teleport.handoff`
- `teleport.handoff` 已支持 handoff 持久化、session baton-pass、proxy plugin-message gateway、目标服到达消费与基础诊断
- `auth` 已支持短时跨服免重登，但不会替代会话锁
- 当前仍未包含 ACK、自动重试、历史审计列表等更强恢复能力

## 适合继续扩展的方向

- 完整跨服传送 handoff 服务
- 排行榜缓存
- 更细粒度的本地缓存失效策略
- 冲突重试与更强的故障恢复逻辑

如果要继续做业务模块，建议优先复用现有命名空间和同步服务，而不是让业务插件直接操作 Redis 或数据库。
