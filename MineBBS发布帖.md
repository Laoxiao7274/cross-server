# [跨服][Paper 1.21+][开源] Cross Server - MySQL/Redis 跨服数据同步 + Homes + Auth + 跨服传送 Handoff

`Cross Server` 是一个基于 **Paper** 的跨服数据与状态协同插件，目标不是只做“切服”，而是提供一套可扩展的跨服基础设施：

- 玩家数据跨服同步
- 会话锁，避免多服同时写同一玩家
- 跨服 homes
- 登录认证与短时跨服免重登
- 跨服传送 handoff / 诊断 / 管理
- 供其他插件调用的 `CrossServerApi`

如果你想做的不只是“代理把玩家送过去”，而是希望 **多台子服共享玩家状态、共享业务数据，并且让其他插件也能接入同一套跨服底层**，这个插件就是为这种场景准备的。

---

## 一、当前已实现功能

### 1）跨服基础能力
- MySQL 持久化存储
- Redis 广播与失效通知（可选）
- 节点心跳与离线判定
- 命名空间机制
- Bukkit `ServicesManager` 暴露 `CrossServerApi`

### 2）玩家会话控制
- 玩家会话锁
- 防止同一玩家被多服同时写入
- 跨服传送时支持 session baton-pass / prepared transfer

### 3）已接好的业务模块
- `economy` 跨服金币
- 玩家背包跨服同步
- 末影箱跨服同步
- 玩家基础状态跨服同步
- `homes` 跨服家园数据
- `auth` 登录认证

### 4）跨服传送能力
- `teleport.handoff` 最新状态快照
- proxy plugin-message gateway
- 到达目标服后自动消费 handoff 并落点传送
- ACK / recovery 基础状态记录
- transfer append-only 历史记录
- 管理命令 + 箱子菜单 UI

### 5）运维与诊断能力
- `/crossserver status`
- `/crossserver nodes`
- `/crossserver node <serverId>`
- `/crossserver transfer info <player>`
- `/crossserver transfer clear <player>`
- `/crossserver transfer menu <player>`
- `/crossserver transfer history <player>`
- `/crossserver transfer recent`
- `/crossserver auth inspect <player>`
- `/crossserver auth invalidate <player>`
- `/crossserver auth forcereauth <player>`

---

## 二、这个插件适合什么服用

适合这类场景：

- 多台 Paper 子服共用同一套玩家数据
- 想做跨服 home / 跨服传送 / 跨服免重登
- 想给自己的业务插件接一套统一跨服 API
- 不想让业务插件自己直连 Redis / MySQL
- 希望跨服问题能查、能看、能清理，而不是出事全靠猜

如果你只是想做最基础的 Bungee/Velocity 切服，这个插件可能会偏重；
但如果你想做的是 **“跨服数据系统”**，它会更合适。

---

## 三、插件核心设计思路

### 1）MySQL 是权威数据源
所有快照、会话、节点状态等最终都以 MySQL 为准。

### 2）Redis 负责广播，不负责当权威
Redis 用于跨服通知、失效同步、事件广播；不是最终数据源。

### 3）命名空间机制
不同业务模块都可以复用同一套底层。

当前已使用命名空间包括：
- `economy`
- `inventory`
- `enderchest`
- `player-state`
- `homes`
- `auth.profile`
- `auth.ticket`
- `teleport.handoff`

### 4）latest snapshot + append-only history
例如跨服传送：
- `player_snapshot` 中保存最新 handoff 状态
- `transfer_history` 中保存完整事件历史

这样既能快速拿到当前状态，也能追时间线排障。

---

## 四、当前主要功能介绍

### 1）跨服 homes
- homes 数据跨服共享
- `/homes` 为箱子菜单 UI
- 当前服 home 可直接本地传送
- 其他服 home 会自动走 handoff 跨服传送链路

### 2）登录认证 auth
- 支持注册、登录、修改密码
- 支持聊天栏输入密码登录
- 支持未登录行为限制
- 支持短时跨服免重登
- 登录过程有 Title / ActionBar / BossBar 提示

### 3）跨服传送 handoff
当前已经不是“预留接口”，而是可实际运行的一套链路：

- source 服准备 handoff
- 准备 session baton-pass
- 通过 proxy plugin-message gateway 发起真实切服
- target 服到达后消费 handoff
- 应用目标世界与目标坐标落点
- 写入 ACK / recovery / failure reason / history

### 4）transfer 管理菜单
已经提供管理员箱子菜单：

- 查看玩家当前 transfer 状态
- 查看 requestId / source / target / gateway / consume / ACK
- 查看最近 transfer 历史摘要
- recent 列表查看最近 transfer 记录
- 点击 recent 记录可直接跳转玩家详情
- 可执行清理卡住 transfer

---

## 五、对外 API

插件已经不是只能“自己用”，目前已通过 Bukkit `ServicesManager` 暴露 `CrossServerApi`，给其他插件调用。

当前可用于：

- 读取 / 保存命名空间快照
- 查询会话服务
- 查询经济服务
- 发起指定玩家跨服传送
- 查询 transfer 状态与历史
- 清理 transfer
- 查询 auth 状态
- 作废 ticket / 强制重新认证

也就是说，其他插件现在已经可以基于这个 API 做：

- 自己的跨服副本入口
- 自定义跨服传送卷轴
- 跨服活动大厅入口
- GM 工具或后台管理插件
- 自定义业务数据的跨服同步

---

## 六、命令一览

### crossserver
```text
/crossserver status
/crossserver nodes [page]
/crossserver node <serverId>
/crossserver transfer <player>
/crossserver transfer info <player>
/crossserver transfer clear <player>
/crossserver transfer menu <player>
/crossserver transfer history <player>
/crossserver transfer recent
/crossserver auth inspect <player>
/crossserver auth invalidate <player>
/crossserver auth forcereauth <player>
/crossserver reload
```

### homes
```text
/home [name]
/homes
/sethome <name>
/delhome <name>
/setdefaulthome <name>
```

### auth
```text
/login <password>
/l <password>
/register <password> <confirm>
/reg <password> <confirm>
/changepassword <old> <new>
```

### economy
```text
/economy balance <player>
/economy set <player> <amount>
/economy deposit <player> <amount>
/economy withdraw <player> <amount>
/economy history <player> [limit]
```

---

## 七、运行环境

- Java 21
- Paper 1.21+
- MySQL 8+
- Redis（可选，启用广播时需要）

---

## 八、安装方式

1. 将插件放入每台子服的 `plugins/` 目录
2. 所有子服使用相同版本的 `cross-server`
3. 所有子服连接同一个 MySQL
4. 如需跨服广播，所有子服连接同一个 Redis
5. 每台子服配置不同的 `server.id`
6. 正确配置代理侧 backend 名称映射

---

## 九、当前优点

我自己把这插件做成现在这样，核心目标就不是“再造一个普通同步插件”，而是想把它做成一套 **可继续扩业务模块的跨服底座**。

目前它的优势主要在这几点：

- 不是只同步一种数据，而是有统一命名空间机制
- 不是只做切服，而是有 handoff、ACK、recovery、history
- 不是黑盒运作，而是有诊断命令和菜单 UI
- 不是只能本体使用，而是已经暴露了 `CrossServerApi`
- 已有 homes / auth / economy / inventory / player-state 多模块验证思路

---

## 十、当前边界

目前已经能用，但也不是说所有高级能力都做满了。

当前边界：

- 自动恢复仍是偏保守 reconcile，不是激进自动重试
- 还没有做非常完整的审计后台
- recent/history 目前偏运维视角，不是完整分页后台系统
- 需要你自己正确配置 MySQL / Redis / proxy backend 映射

也就是说，它现在已经适合实际服继续接业务，但如果你追求的是“成品 SaaS 式后台 + 全自动恢复”，那还在继续完善空间内。

---

## 十一、后续计划方向

后面我准备继续补这些：

- 更完整的 transfer 恢复与补偿策略
- 更强的运维面板
- 更多可直接复用的 API
- 排行榜 / 更多跨服业务模块
- 更完善的观测与排障能力

---

## 十二、适合二开 / 接入的开发者说明

如果你是开发者，这插件最值得关注的是这一点：

**你可以把它当成跨服底层，而不是只把它当成一个成品功能插件。**

推荐接入方式：

1. 通过 Bukkit `ServicesManager` 获取 `CrossServerApi`
2. 注册自己的命名空间
3. 读写自己的玩家数据 / 全局数据
4. 必要时注册同步监听器
5. 直接复用 transfer / auth / session 能力

这样做的好处是：
- 你的业务插件不用自己维护 Redis 广播协议
- 不用自己处理 MySQL 快照结构
- 不用自己重新设计跨服传送状态机

---

## 十三、开源与反馈

如果你想看代码、自己二开、或者一起完善，都欢迎。

如果你对这类“跨服底层框架 + 业务模块接入”方向感兴趣，也欢迎反馈你更想优先看到哪一块：

- 更多现成模块
- 更完整后台
- 更强 API
- 更稳的恢复链路

---

## 十四、总结

一句话概括：

**Cross Server 不是单一功能插件，而是一套面向 Paper 多服架构的跨服基础设施。**

它现在已经具备：
- 数据同步
- 会话控制
- homes
- auth
- handoff 跨服传送
- 运维诊断
- 对外 API

如果你想在自己的服务器里做一套真正能继续扩展的跨服体系，它现在已经可以作为基础开始用了。

---

如果帖子里需要，我后面也可以继续补：
- 简洁版介绍
- 截图展示版
- 开发者接入示例版
- MineBBS 风格的短版首帖
