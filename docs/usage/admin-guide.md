# 技术管理员指南

这份文档写给：

- 技术管理员
- 负责跨服排障的人
- 负责多节点统一运维的人

## 一、核心管理命令

### 1. 基础状态与节点检查

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver help` | 查看帮助 | `crossserver.command` |
| `/cs help [page]` | 分页查看帮助（每页 5 条） | `crossserver.command` |
| `/cs menu` | 打开 GUI 总菜单入口 | `crossserver.command` |
| `/crossserver status` | 查看当前节点状态 | `crossserver.status.view` |
| `/crossserver nodes [page]` | 查看节点列表 | `crossserver.nodes.view` |
| `/crossserver node <serverId>` | 查看单节点详情 | `crossserver.node.view` |

### 2. 热重载

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver reload` | 重载配置和内部服务 | `crossserver.reload` |

说明：

- 与 Web 面板、路由 GUI 的“重载本节点”使用同一套安全重载逻辑
- 会防止并发重载

## 二、共享配置管理

### 1. 共享模块开关

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver modules list` | 查看模块 local/shared/effective | `crossserver.modules.view` |
| `/crossserver modules set <module> <true|false>` | 设置共享覆盖 | `crossserver.modules.edit` |
| `/crossserver modules clear <module>` | 清除共享覆盖 | `crossserver.modules.edit` |

支持模块：

- `auth`
- `homes`
- `warps`
- `tpa`
- `route-config`
- `transfer-admin`
- `economy-bridge`
- `permissions`

### 2. 共享路由管理

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver route list` | 查看共享路由与本地合并结果 | `crossserver.route.view` |
| `/crossserver route menu` | 打开路由管理 GUI | `crossserver.route.view` |
| `/crossserver route set <serverId> <proxyServer>` | 设置共享路由覆盖 | `crossserver.route.edit` |
| `/crossserver route remove <serverId>` | 删除共享路由覆盖 | `crossserver.route.edit` |

### 3. 节点配置远程管理

推荐优先使用 Web 面板完成。

当前支持白名单字段：

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

- 目标节点自动发布配置快照
- 主控节点提交变更申请
- 目标节点自动写回本地 `config.yml`
- 写回后自动排队重载

## 三、转服诊断与修复

### 1. 命令

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver transfer <player>` | 快速查看玩家 transfer 状态 | `crossserver.transfer.view` |
| `/crossserver transfer info <player>` | 查看详细诊断 | `crossserver.transfer.view` |
| `/crossserver transfer history <player>` | 查看该玩家最近 transfer 历史 | `crossserver.transfer.view` |
| `/crossserver transfer menu <player>` | 打开该玩家的 transfer GUI | `crossserver.transfer.menu` |
| `/crossserver transfer recent [page]` | 查看 recent transfer GUI | `crossserver.transfer.menu` |
| `/crossserver transfer reconcile <player>` | 尝试保守修补异常状态 | `crossserver.transfer.reconcile` |
| `/crossserver transfer clear <player>` | 清理 handoff 与 prepared transfer | `crossserver.transfer.clear` |

### 2. 建议排障顺序

当玩家跨服卡住时，建议按顺序操作：

1. `/crossserver transfer info <player>`
2. `/crossserver transfer history <player>`
3. `/crossserver transfer reconcile <player>`
4. 仍不正常再 `/crossserver transfer clear <player>`

### 3. GUI 能看到什么

Transfer 管理 GUI 中可以查看：

- handoff 状态
- session transfer 状态
- recovery status
- suggested actions
- 历史记录

## 四、认证与经济管理

### 1. 认证管理

| 命令 | 作用 | 权限 |
|------|------|------|
| `/crossserver auth inspect <player>` | 查看认证状态 | `crossserver.auth.admin` |
| `/crossserver auth invalidate <player>` | 让 Ticket 失效 | `crossserver.auth.admin` |
| `/crossserver auth forcereauth <player>` | 强制重新认证 | `crossserver.auth.admin` |

### 2. 经济管理

| 命令 | 作用 | 权限 |
|------|------|------|
| `/economy balance <player>` | 查看余额 | `crossserver.economy.balance` |
| `/economy history <player> [limit]` | 查看流水 | `crossserver.economy.history` |
| `/economy set <player> <amount>` | 设置余额 | `crossserver.economy.set` |
| `/economy deposit <player> <amount>` | 增加余额 | `crossserver.economy.deposit` |
| `/economy withdraw <player> <amount>` | 扣除余额 | `crossserver.economy.withdraw` |

## 五、Web 面板运维

### 1. 页面能力

| 页面 | 用途 |
|------|------|
| 仪表盘 | 查看节点状态、主控节点、集群概览 |
| 模块 | 管理共享模块开关 |
| 路由 | 管理共享路由 |
| 配置文档 | 编辑 JSON / YAML 配置文档 |
| 节点配置 | 管理目标节点配置快照和变更申请 |
| 日志中心 | 按节点查看日志 |
| 转服诊断 | 查看 recent transfer 和玩家详情 |

### 2. Web API

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

#### 认证

```http
X-CrossServer-Token: 你的token
X-CrossServer-Actor: 操作者（可选）
```

## 六、你最需要注意的坑

### 1. 改了共享配置但本服没立刻生效

解决：

```text
/crossserver reload
```

### 2. Web 面板能打开但外部访问不到

优先检查：

- `web-panel.host`
- 防火墙
- 反向代理配置

### 3. 某个节点一直显示离线

优先检查：

- 是否连接同一个 MySQL
- `server.id` 是否重复
- 该节点是否真的正常启动

### 4. 跨服 TPA 失败

优先检查：

- 目标玩家是否在线
- 位置快照是否新鲜
- 目标节点是否在线
- handoff 是否已过期

## 七、推荐权限组合

技术管理员通常建议给：

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
