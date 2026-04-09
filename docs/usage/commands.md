# 使用指南

这不是单纯的“命令列表”，而是一份完整的功能手册。

适合这些人阅读：

- 小白服主
- 普通管理员
- 技术管理员
- 想把功能介绍发给玩家的人

## 功能总览

CrossServer 当前可用的主要功能，可以按下面几类理解：

| 分类 | 包含内容 | 适合谁用 |
|------|----------|----------|
| 玩家日常功能 | 家园、Warp、TPA、登录认证 | 普通玩家 |
| 管理运维功能 | 状态查看、节点管理、模块开关、路由管理、重载 | 服主 / 技术管理员 |
| 诊断修复功能 | 转服诊断、清理卡住状态、认证管理、经济流水 | 技术管理员 |
| 图形菜单功能 | Homes GUI、Warp GUI、路由管理 GUI、Transfer 管理 GUI | 玩家 / 管理员 |
| Web 面板功能 | 模块、路由、配置文档、节点配置、日志中心、转服诊断 | 服主 / 技术管理员 |
| 配置中心功能 | 共享模块、共享路由、配置文档、节点配置快照 | 技术管理员 / 第三方插件 |

## 一、玩家日常功能

这部分是玩家最常用的功能。

### 1. 家园系统

用途：让玩家保存多个家园，并在任意子服返回这些家园。

#### 玩家能做什么

| 命令 | 作用 | 权限 |
|------|------|------|
| `/homes` | 打开家园 GUI 菜单 | `crossserver.homes.list` / `crossserver.homes.menu` |
| `/home [name]` | 回默认家园或指定家园 | `crossserver.homes.teleport` |
| `/sethome <name>` | 把当前位置设置为家园 | `crossserver.homes.set` |
| `/delhome <name>` | 删除家园 | `crossserver.homes.delete` |
| `/setdefaulthome <name>` | 设置默认家园 | `crossserver.homes.default` |

#### GUI 菜单里能做什么

打开方式：`/homes`

| 操作 | 效果 |
|------|------|
| 左键家园 | 传送到该家园 |
| 右键家园 | 设为默认家园 |
| Shift + 右键家园 | 删除该家园 |
| 上一页 / 下一页 | 翻页查看更多家园 |

#### 特点

- 支持跨服家园
- 如果家园不在当前子服，会自动跨服传送
- 默认家园会在 GUI 里特殊标记

### 2. Warp 系统

用途：为全服提供公共传送点。

#### 玩家和管理员可用命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/warp [name]` | 打开 Warp 菜单或直接传送 | `crossserver.warps.list` / `crossserver.warps.teleport` |
| `/setwarp <name>` | 创建 Warp | `crossserver.warps.set` |
| `/delwarp <name>` | 删除 Warp | `crossserver.warps.delete` |

#### GUI 菜单里能做什么

打开方式：`/warp`

| 操作 | 效果 |
|------|------|
| 左键 Warp | 传送到 Warp |
| Shift + 右键 Warp | 删除 Warp |
| 上一页 / 下一页 | 翻页 |

#### 特点

- 支持全局 Warp
- 支持跨服 Warp
- 可以用 GUI 浏览，不用强记名字

### 3. TPA 玩家传送请求

用途：玩家之间互相请求传送，支持同服和跨服。

#### 可用命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/tpa <player>` | 请求传送到对方身边 | `crossserver.tpa.use` |
| `/tpahere <player>` | 邀请对方传送到你身边 | `crossserver.tpa.here` |
| `/tpaccept [player]` | 接受请求 | `crossserver.tpa.use` |
| `/tpdeny [player]` | 拒绝请求 | `crossserver.tpa.use` |
| `/tpcancel` | 取消你发出的所有请求 | `crossserver.tpa.use` |

#### 使用说明

- `/tpaccept` 不写名字时，会接受最近一条请求
- `/tpdeny` 不写名字时，会拒绝最近一条请求
- `/tpcancel` 会清掉你当前发出去的所有待处理请求
- 如果目标玩家在别的子服，只要在线且位置快照有效，也能正常请求

### 4. 登录认证

前提：你开启了 `auth` 模块。

#### 玩家可用命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/login <password>` | 登录 | `crossserver.auth.login` |
| `/l <password>` | 登录简写 | `crossserver.auth.login` |
| `/register <password> <confirm>` | 注册 | `crossserver.auth.register` |
| `/reg <password> <confirm>` | 注册简写 | `crossserver.auth.register` |
| `/changepassword <old> <new>` | 修改密码 | `crossserver.auth.changepassword` |

#### 特点

- 注册一次后，支持跨服免重登 Ticket
- 短时间内跨服不需要重复登录

## 二、管理员运维功能

### 1. 节点状态与基础检查

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver help` | 查看帮助 | `crossserver.command` |
| `/crossserver status` | 查看当前节点状态 | `crossserver.status.view` |
| `/crossserver nodes [page]` | 查看所有节点状态 | `crossserver.nodes.view` |
| `/crossserver node <serverId>` | 查看某个节点详情 | `crossserver.node.view` |

#### 适合什么时候用

- 插件刚装好，确认有没有正常连接 MySQL / Redis
- 多台子服都开起来后，确认节点是否在线
- 某台服掉线时查看它的最后心跳和延迟

### 2. 热重载

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver reload` | 重载插件配置和内部服务 | `crossserver.reload` |

#### 什么时候需要用

- 改了共享模块开关后
- 改了共享路由后
- 目标节点写回 `config.yml` 后需要本地重新加载时

#### 统一入口

现在这些入口都是同一套安全逻辑：

- `/crossserver reload`
- 路由菜单里的“重载本节点”
- Web 面板里的“重载本节点”

特点：

- 防止并发重载
- 重载期间会保护内部状态
- Web 面板触发重载时会短暂断开再自动尝试恢复

### 3. 路由管理

用途：统一维护 `serverId -> proxyServer` 映射。

#### 命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver route list` | 查看路由合并结果 | `crossserver.route.view` |
| `/crossserver route menu` | 打开路由管理 GUI | `crossserver.route.view` |
| `/crossserver route set <serverId> <proxyServer>` | 设置共享路由覆盖 | `crossserver.route.edit` |
| `/crossserver route remove <serverId>` | 删除共享路由覆盖 | `crossserver.route.edit` |

#### GUI 里能做什么

打开方式：`/crossserver route menu`

| 操作 | 效果 |
|------|------|
| 左键已有路由 | 进入聊天编辑模式 |
| 右键已有路由 | 进入删除覆盖输入模式 |
| 点击“新增路由” | 新建一条路由 |
| 点击“刷新” | 重新读取共享路由和本地合并结果 |
| 点击“重载本节点” | 让当前节点立即应用共享路由 |

#### 路由来源说明

GUI 和命令里会看到这些来源：

- `仅本地`
- `仅共享`
- `本地 + 共享覆盖`

### 4. 模块开关管理

用途：统一控制模块是否在集群中启用。

#### 命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver modules list` | 查看本地默认 / 共享覆盖 / 最终生效值 | `crossserver.modules.view` |
| `/crossserver modules set <module> <true|false>` | 设置共享覆盖 | `crossserver.modules.edit` |
| `/crossserver modules clear <module>` | 清除共享覆盖 | `crossserver.modules.edit` |

#### 支持的模块名

| 模块名 | 作用 |
|--------|------|
| `auth` | 登录认证 |
| `homes` | 家园系统 |
| `warps` | Warp 系统 |
| `tpa` | TPA 请求 |
| `route-config` | 路由管理 |
| `transfer-admin` | 转服诊断 |
| `economy-bridge` | 经济桥接 |
| `permissions` | 权限同步 |

#### 理解方式

- `local` = 当前节点 config.yml 里的默认值
- `shared` = 集群共享覆盖值
- `effective` = 当前真正生效的值

## 三、诊断与修复功能

### 1. 转服诊断

用途：排查玩家跨服卡住、掉状态、handoff 异常等问题。

#### 命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver transfer <player>` | 快速看玩家转服状态 | `crossserver.transfer.view` |
| `/crossserver transfer info <player>` | 看详细诊断 | `crossserver.transfer.view` |
| `/crossserver transfer history <player>` | 看该玩家最近转服历史 | `crossserver.transfer.view` |
| `/crossserver transfer menu <player>` | 打开玩家诊断 GUI | `crossserver.transfer.menu` |
| `/crossserver transfer recent [page]` | 打开最近转服记录 GUI | `crossserver.transfer.menu` |
| `/crossserver transfer reconcile <player>` | 尝试修补异常状态 | `crossserver.transfer.reconcile` |
| `/crossserver transfer clear <player>` | 强制清理卡住状态 | `crossserver.transfer.clear` |

#### GUI 能做什么

Transfer GUI 里可以：

- 看当前玩家 handoff 状态
- 看 session transfer 状态
- 看恢复状态和建议动作
- 查看历史记录
- 执行 clear
- 在 recent 列表和详情页之间切换

### 2. 认证管理

用途：处理认证异常、强制玩家重新登录。

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver auth inspect <player>` | 查看认证运行时状态和快照状态 | `crossserver.auth.admin` |
| `/crossserver auth invalidate <player>` | 让玩家 Ticket 失效 | `crossserver.auth.admin` |
| `/crossserver auth forcereauth <player>` | 强制重新认证 | `crossserver.auth.admin` |

### 3. 经济管理

用途：查看余额、调账、查流水。

| 命令 | 作用 | 权限 |
|------|------|------|
| `/economy balance <player>` | 查看余额 | `crossserver.economy.balance` |
| `/economy history <player> [limit]` | 查看最近流水 | `crossserver.economy.history` |
| `/economy set <player> <amount>` | 直接设置余额 | `crossserver.economy.set` |
| `/economy deposit <player> <amount>` | 增加余额 | `crossserver.economy.deposit` |
| `/economy withdraw <player> <amount>` | 扣除余额 | `crossserver.economy.withdraw` |

## 四、图形菜单功能

除了命令，插件还内置了几套 GUI。

| 菜单 | 打开方式 | 适合谁 |
|------|----------|--------|
| Homes 菜单 | `/homes` | 玩家 |
| Warp 菜单 | `/warp` | 玩家 / 管理员 |
| 路由管理菜单 | `/crossserver route menu` | 技术管理员 |
| Transfer 管理菜单 | `/crossserver transfer menu <player>` / `/crossserver transfer recent` | 技术管理员 |

这些菜单的意义是：

- 不需要记住太多命令参数
- 更适合小白管理员使用
- 可以在 GUI 里看到更多上下文信息

## 五、Web 面板功能

如果你开启了 Web 面板，那么很多事情都可以直接在网页里做。

### 1. 面板能做什么

| 页面 | 功能 |
|------|------|
| 仪表盘 | 查看集群状态、节点列表、面板主控节点信息 |
| 模块 | 查看并修改共享模块开关 |
| 路由 | 查看并修改共享路由 |
| 配置文档 | 查看并编辑配置中心文档，支持 JSON / YAML |
| 节点配置 | 修改目标节点的 messaging / webPanel / modules 白名单配置 |
| 日志中心 | 按节点查看同步日志 |
| 转服诊断 | 查看 recent transfer 和玩家详细诊断 |

### 2. 面板适合谁

- 服主
- 技术管理员
- 负责多节点统一运维的人

### 3. Web 面板接口

如果你想做自动化、接反向代理、或者自己写脚本，这些接口有用。

#### 只读接口

- `GET /api/overview`
- `GET /api/status`
- `GET /api/modules`
- `GET /api/routes`
- `GET /api/config-documents`
- `GET /api/logs`
- `GET /api/node-configs`
- `GET /api/node-configs/detail?serverId=<节点ID>`
- `GET /api/transfers/recent`
- `GET /api/transfers/player?player=<玩家名>`

#### 写入接口

- `PUT /api/modules`
- `PUT /api/routes`
- `PUT /api/config-documents`
- `POST /api/routes`
- `DELETE /api/routes?serverId=<子服ID>`
- `POST /api/node-configs/apply`

#### 鉴权方式

```http
X-CrossServer-Token: 你的token
X-CrossServer-Actor: 操作者（可选）
```

## 六、配置中心与共享功能

除了命令和网页，你还需要知道插件内部有哪些“共享能力”。

### 1. 共享模块配置

作用：统一覆盖模块开关。

使用入口：

- `/crossserver modules ...`
- Web 面板“模块”页

### 2. 共享路由表

作用：统一覆盖跨服路由。

使用入口：

- `/crossserver route ...`
- 路由 GUI
- Web 面板“路由”页

### 3. 配置文档中心

作用：给第三方插件存储共享配置。

特点：

- 支持 JSON
- 支持 YAML
- 自动补元信息：`schemaVersion`、`updatedBy`、`updatedAt`、`source`、`summary`

使用入口：

- 第三方插件 API
- Web 面板“配置文档”页

### 4. 节点配置远程管理

作用：主控节点远程修改目标子服的部分本地配置。

当前允许修改的白名单字段：

- `messaging.enabled`
- `messaging.redisUri`
- `messaging.channel`
- `webPanel.enabled`
- `webPanel.host`
- `webPanel.port`
- `webPanel.masterServerId`
- `webPanel.token`
- `modules.*`

特点：

- 目标节点启动后会自动上报配置快照
- 主控节点修改后，会提交给目标节点本地写回 `config.yml`
- 写回后会排队重载目标节点
- 不会直接暴露数据库、代理等敏感配置

## 七、权限分组建议

### 1. 普通玩家推荐权限

- `crossserver.homes.list`
- `crossserver.homes.menu`
- `crossserver.homes.teleport`
- `crossserver.homes.set`
- `crossserver.homes.delete`
- `crossserver.homes.default`
- `crossserver.warps.list`
- `crossserver.warps.teleport`
- `crossserver.tpa.use`
- `crossserver.tpa.here`

如果启用了认证模块，再加：

- `crossserver.auth.login`
- `crossserver.auth.register`
- `crossserver.auth.changepassword`

### 2. 运营 / 建筑管理员推荐权限

- 普通玩家权限全部
- `crossserver.warps.set`
- `crossserver.warps.delete`

### 3. 技术管理员推荐权限

- `crossserver.command`
- `crossserver.status.view`
- `crossserver.nodes.view`
- `crossserver.node.view`
- `crossserver.route.view`
- `crossserver.route.edit`
- `crossserver.modules.view`
- `crossserver.modules.edit`
- `crossserver.reload`
- `crossserver.transfer.view`
- `crossserver.transfer.menu`
- `crossserver.transfer.reconcile`
- `crossserver.transfer.clear`
- `crossserver.auth.admin`
- `crossserver.economy.balance`
- `crossserver.economy.history`
- `crossserver.economy.set`
- `crossserver.economy.deposit`
- `crossserver.economy.withdraw`

## 八、常见使用场景

### 场景 1：玩家要在所有子服都能回家

给玩家：

- `crossserver.homes.*` 对应权限

玩家使用：

- `/sethome main`
- `/home main`
- `/homes`

### 场景 2：服主要统一关闭 TPA

可以用：

- `/crossserver modules set tpa false`

或者：

- Web 面板 -> 模块 -> 关闭 `tpa`

然后执行：

- `/crossserver reload`

### 场景 3：新增一台子服

你通常需要做这几件事：

1. 在代理里注册服务器
2. 配好新节点 `server.id`
3. 在 `teleport.gateway.server-map` 或共享路由中加入映射
4. 确认 `/crossserver nodes` 能看到它在线
5. 如需统一管理，把 `web-panel.enabled` 也打开

### 场景 4：某个玩家跨服卡住了

建议顺序：

1. `/crossserver transfer info <player>`
2. `/crossserver transfer reconcile <player>`
3. 还不行再 `/crossserver transfer clear <player>`

### 场景 5：想用网页统一管所有子服

做法：

1. 选一个主控节点
2. 所有需要加入的节点都开启 `web-panel.enabled: true`
3. 所有节点使用同一组 token
4. 主控节点配置 `web-panel.master-server-id`
5. 浏览器访问主控节点 `http://host:port/`

## 九、最容易踩坑的地方

### 1. 改了共享配置却没马上生效

这是最常见情况。

原因：

- 共享配置先写入配置中心
- 当前节点不会自动把所有配置立刻热应用

解决：

- 执行 `/crossserver reload`
- 或点击 Web 面板 / 路由菜单里的“重载本节点”

### 2. Web 面板能打开，但别的机器访问不到

检查：

- `web-panel.host` 是否还是 `127.0.0.1`
- 如果要外部访问，通常应改成 `0.0.0.0` 或服务器内网 IP
- 同时建议配反向代理，不要直接裸暴露

### 3. 普通玩家需不需要 `/crossserver` 权限

一般不需要。

普通玩家只需要家园、Warp、TPA、认证相关权限，不需要管理命令权限。
