# 命令与权限

## 玩家命令

### 家园

| 命令 | 说明 |
|------|------|
| `/home [名称]` | 传送到默认家园或指定家园（支持跨服） |
| `/homes` | 打开家园管理 GUI 菜单 |
| `/sethome <名称>` | 设置当前位置为家园 |
| `/delhome <名称>` | 删除家园 |
| `/setdefaulthome <名称>` | 设置默认家园 |

**说明：**

- 家园数据跨服共享，所有子服都能看到相同的家园列表
- 如果目标家园在当前服务器上，直接本地传送
- 如果目标家园在其他服务器上，自动触发跨服传送流程
- 家园名称支持字母、数字、下划线和连字符，最长 16 个字符

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.homes.list` | true | 查看家园列表 |
| `crossserver.homes.menu` | true | 打开家园 UI 菜单 |
| `crossserver.homes.teleport` | true | 传送回家园 |
| `crossserver.homes.set` | true | 设置家园 |
| `crossserver.homes.delete` | true | 删除家园 |
| `crossserver.homes.default` | true | 设置默认家园 |

### Warp

| 命令 | 说明 |
|------|------|
| `/warp [名称]` | 输入名称直接传送；不带参数时打开 Warp GUI |
| `/setwarp <名称>` | 设置当前位置为全局 Warp |
| `/delwarp <名称>` | 删除全局 Warp |

**说明：**

- Warp 存在于全局快照中，所有子服共享同一份列表
- `/warp` 无参数时会打开 GUI，支持分页浏览、左键传送、Shift+右键删除（需权限）
- 如果目标 Warp 在当前服务器上，直接本地传送
- 如果目标 Warp 在其他服务器上，自动触发跨服 handoff
- Warp 名称支持字母、数字、下划线和连字符，最长 24 个字符

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.warps.list` | true | 查看 Warp 列表 / 打开 Warp GUI |
| `crossserver.warps.teleport` | true | 传送到 Warp |
| `crossserver.warps.set` | op | 设置全局 Warp |
| `crossserver.warps.delete` | op | 删除全局 Warp |

### TPA / 跨服传送请求

| 命令 | 说明 |
|------|------|
| `/tpa <player>` | 请求自己传送到目标玩家 |
| `/tpahere <player>` | 邀请目标玩家传送到你这里 |
| `/tpaccept [player]` | 接受最近一条请求或指定玩家的请求 |
| `/tpdeny [player]` | 拒绝最近一条请求或指定玩家的请求 |
| `/tpcancel` | 取消你发出的所有待处理请求 |

**说明：**

- TPA 支持同服与跨服玩家；跨服时会读取玩家位置快照并自动切换为 handoff
- 请求有效期复用 `teleport.handoff-seconds` 配置
- 收到请求时会显示聊天提示、Title、ActionBar 与音效
- 接受、拒绝、取消、过期都会向相关玩家提供反馈
- 同服接受时直接 `teleportAsync`；跨服接受时自动发起跨服传送

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.tpa.use` | true | 使用 `/tpa` `/tpaccept` `/tpdeny` `/tpcancel` |
| `crossserver.tpa.here` | true | 使用 `/tpahere` |

### 登录认证

| 命令 | 说明 |
|------|------|
| `/login <密码>` | 登录（简写 `/l`） |
| `/register <密码> <确认密码>` | 注册账号（简写 `/reg`） |
| `/changepassword <旧密码> <新密码>` | 修改密码 |

**说明：**

- 未登录时会限制移动、交互、背包操作
- 登录期间显示 BossBar 倒计时（45 秒超时）
- 可以直接在聊天栏输入密码完成登录
- 登录成功后生成跨服 Ticket（5 分钟有效），切服后无需重复登录

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.auth.login` | true | 登录 |
| `crossserver.auth.register` | true | 注册 |
| `crossserver.auth.changepassword` | true | 修改密码 |

### 经济

| 命令 | 说明 |
|------|------|
| `/economy balance` | 查看自己的余额 |
| `/economy history [数量]` | 查看自己的交易记录 |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.economy.balance` | op | 查看余额 |
| `crossserver.economy.history` | op | 查看交易记录 |

---

## 管理员命令

### 服务器状态

| 命令 | 说明 |
|------|------|
| `/crossserver status` | 查看当前节点运行状态 |
| `/crossserver nodes [页码]` | 分页查看集群所有节点 |
| `/crossserver node <serverId>` | 查看指定节点详情 |
| `/crossserver reload` | 热重载配置（不重启服务器） |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.command` | op | 使用 `/crossserver` |
| `crossserver.status.view` | op | 查看状态 |
| `crossserver.nodes.view` | op | 查看节点列表 |
| `crossserver.node.view` | op | 查看单个节点 |
| `crossserver.reload` | op | 热重载 |

### 传送管理

| 命令 | 说明 |
|------|------|
| `/crossserver transfer info <玩家>` | 查看玩家传送诊断详情 |
| `/crossserver transfer <玩家>` | 快速查看玩家传送状态 |
| `/crossserver transfer menu <玩家>` | 打开传送管理 GUI 菜单 |
| `/crossserver transfer history <玩家>` | 查看玩家传送历史 |
| `/crossserver transfer recent [页码]` | 查看最近传送事件 |
| `/crossserver transfer clear <玩家>` | 清理卡住的传送状态 |
| `/crossserver transfer reconcile <player>` | 保守修复传送状态 |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.transfer.view` | op | 查看传送信息 |
| `crossserver.transfer.clear` | op | 清理传送状态 |
| `crossserver.transfer.menu` | op | 打开传送菜单 |
| `crossserver.transfer.reconcile` | op | 修复传送状态 |

### 认证管理

| 命令 | 说明 |
|------|------|
| `/crossserver auth inspect <玩家>` | 查看玩家认证信息 |
| `/crossserver auth invalidate <玩家>` | 作废玩家的跨服免重登票据 |
| `/crossserver auth forcereauth <玩家>` | 强制玩家重新认证 |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.auth.admin` | op | 认证管理命令 |

### 经济管理

| 命令 | 说明 |
|------|------|
| `/economy balance <玩家>` | 查看指定玩家余额 |
| `/economy set <玩家> <金额>` | 设置余额 |
| `/economy deposit <玩家> <金额>` | 增加余额 |
| `/economy withdraw <玩家> <金额>` | 扣除余额 |
| `/economy history <玩家> [数量]` | 查看指定玩家交易记录 |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.economy.balance` | op | 查看余额 |
| `crossserver.economy.set` | op | 设置余额 |
| `crossserver.economy.deposit` | op | 增加余额 |
| `crossserver.economy.withdraw` | op | 扣除余额 |
| `crossserver.economy.history` | op | 交易记录 |

### 路由管理

| 命令 | 说明 |
|------|------|
| `/crossserver route list` | 查看当前共享路由表 |
| `/crossserver route menu` | 打开路由配置 GUI |
| `/crossserver route set <key> <value>` | 设置路由映射 |
| `/crossserver route remove <key>` | 删除路由映射 |

**权限：**

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `crossserver.route.view` | op | 查看路由 |
| `crossserver.route.edit` | op | 编辑路由 |
