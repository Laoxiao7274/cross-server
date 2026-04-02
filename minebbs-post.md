# CrossServer

> Paper 1.21+ 跨服数据同步插件：背包不丢、经济通用、家园跨服、Warp 共享、TPA 可跨服

## 插件简介

CrossServer 是一个面向 **Velocity / BungeeCord 多子服集群** 的跨服同步插件，目标很直接：

**让玩家在多服之间切换时，数据不断档、状态不丢失、传送更稳定。**

它不是只做单一功能，而是把跨服服组里最常见的一整套能力整合起来：

- 背包同步
- 末影箱同步
- 经济同步
- 家园跨服
- Warp 共享
- 跨服 TPA
- 登录认证 / 免重登 Ticket
- 玩家状态同步
- 游戏模式同步
- 可选权限同步
- 共享模块开关 / 配置中心
- 跨服传送失败回滚与修复

适合这类服组：

- 生存服 + 资源服
- 大厅服 + 多玩法分区
- 多节点分线集群
- 希望统一玩家数据体验的 Paper 服组

---

## 亮点功能

### 1. 背包 & 末影箱跨服同步
玩家切服后自动恢复背包和末影箱内容，不用担心换服丢物品。

### 2. 玩家状态同步
除了背包数据，还会同步：

- 血量
- 饥饿
- 饱和度
- 经验 / 总经验
- 等级
- 着火状态
- 氧气值
- **游戏模式**

也就是说，玩家从 A 服切到 B 服，状态不会莫名重置。

### 3. 可选权限同步模块
新增 `permissions` 模块，可同步 **`crossserver.*`** 权限节点。

当前实现特点：

- 默认关闭，按需开启
- 只同步插件自身相关权限，避免误伤其他权限系统
- 适合统一本插件命令权限在多服之间的保持一致

> 目前不直接接管 LuckPerms / Vault Permission 的完整权限体系，而是采用更保守的方式，只处理 `crossserver.*`。

### 4. 全局 Warp
支持：

- `/warp`
- `/setwarp`
- `/delwarp`
- GUI 菜单查看和传送

Warp 数据全服共享，目标在别的子服时会自动走跨服传送。

### 5. 跨服家园系统
支持：

- `/home`
- `/homes`
- `/sethome`
- `/delhome`
- `/setdefaulthome`

玩家在任意子服设置的家，都可以在其他子服直接回去。

### 6. 跨服 TPA
支持：

- `/tpa`
- `/tpahere`
- `/tpaccept`
- `/tpdeny`
- `/tpcancel`

不只是同服玩家可用，**跨服在线玩家也能直接发起 TPA 请求**。

### 7. 登录认证 + 跨服免重登
支持：

- `/login`
- `/register`
- `/changepassword`

带跨服 Ticket 机制，短时间内换服不需要重复登录。

### 8. 跨服传送保护
这是这类插件里很关键的一块。插件内置：

- handoff 传送交接
- prepared session 管理
- 失败回滚
- 关闭服务器时保护收口
- 玩家回滚补偿恢复
- 定期 reconcile 修复悬空状态

尽量减少：

- 卡传送
- 丢背包
- 丢状态
- 半截切服

### 9. 模块开关 + 共享配置中心
这是这次比较核心的新能力。

支持对主要功能模块统一开关：

- `auth`
- `homes`
- `warps`
- `tpa`
- `route-config`
- `transfer-admin`
- `economy-bridge`
- `permissions`

并且支持：

- 本地 `config.yml` 作为默认值
- 集群共享配置作为覆盖层
- `/crossserver reload` 热加载生效

也就是说，服组不再需要每台子服都手改一次模块开关，可以当成一个轻量“配置中心”来用。

---

## 命令预览

### 玩家命令

```text
/home [name]
/homes
/sethome <name>
/delhome <name>
/setdefaulthome <name>

/warp [name]
/setwarp <name>
/delwarp <name>

/tpa <player>
/tpahere <player>
/tpaccept [player]
/tpdeny [player]
/tpcancel

/login <password>
/register <password> <confirm>
/changepassword <old> <new>
```

### 管理命令

```text
/crossserver status
/crossserver nodes
/crossserver node <serverId>
/crossserver reload

/crossserver route list
/crossserver route set <serverId> <proxyServer>
/crossserver route remove <serverId>

/crossserver modules list
/crossserver modules set <module> <true|false>
/crossserver modules clear <module>

/crossserver transfer info <player>
/crossserver transfer clear <player>
/crossserver transfer history <player>
/crossserver transfer reconcile <player>

/crossserver auth inspect <player>
/crossserver auth invalidate <player>
/crossserver auth forcereauth <player>
```

---

## 环境要求

- **Java 21+**
- **Paper 1.21+**
- **MySQL 8.0+**
- **Redis 6.0+**（可选，但推荐）
- **Velocity / BungeeCord**（需要跨服传送时必须）

---

## 安装说明

### 1. 准备数据库
创建一个 MySQL 数据库，例如：

```sql
CREATE DATABASE cross_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

插件启动时会自动建表。

### 2. 配置 `config.yml`
基础需要配置：

- `server.id`
- `server.cluster`
- `database.jdbc-url`
- `database.username`
- `database.password`
- `messaging.redis-uri`（如果启用 Redis）
- `teleport.gateway.server-map`

### 3. 在代理中注册子服
无论是 Velocity 还是 BungeeCord，都要先把子服注册好。

### 4. 启动后检查
可先执行：

```text
/crossserver status
/crossserver nodes
```

确认节点状态正常。

---

## 配置亮点

### 共享模块开关
现在支持：

```yaml
modules:
  auth: true
  homes: true
  warps: true
  tpa: true
  route-config: true
  transfer-admin: true
  economy-bridge: true
  permissions: false
```

并且可以通过命令统一管理：

```text
/crossserver modules list
/crossserver modules set permissions true
/crossserver modules clear permissions
```

### 权限同步模块说明
开启 `permissions` 后：

- 玩家加入时加载权限快照
- 玩家退出 / 被踢 / 死亡 / 重生 / 自动保存时保存权限快照
- 跨服前会先 flush 权限状态
- 当前只同步 `crossserver.*`

---

## 适用场景

如果你的服组有这些需求，这个插件会比较适合：

- 想统一多个子服的玩家背包 / 状态 / 家园 / Warp
- 想做真正可用的跨服 TPA
- 想减少换服后数据错乱、状态丢失
- 想要更稳的跨服传送保护与回滚
- 想把模块开关做成多服共享，而不是每台服手改

---

## 当前版本进展

本次更新重点包括：

- 新增共享模块配置中心
- 新增 `/crossserver modules` 管理命令
- 玩家状态同步新增 **游戏模式同步**
- 新增可选 **权限同步模块**
- 共享模块配置快照兼容旧数据
- 文档同步整理
- 修复默认配置与权限声明的小回归问题

---

## 测试状态

当前已完成：

- `mvn compile` 编译通过
- 模块开关接入完成
- 游戏模式同步接入完成
- 权限同步主链路接入完成
- 文档同步完成

建议服组实测项：

- 游戏模式跨服保持是否正常
- `crossserver.*` 权限跨服保持是否正常
- `/crossserver modules list|set|clear` 是否符合预期
- 跨服失败时 rollback 是否正常

---

## 下载 / 使用说明

你可以把插件包放到 GitHub Releases 提供下载，也可以直接让 GitHub 自动构建测试包。

### GitHub 自动构建

这个仓库已经可以接入 GitHub Actions 自动构建：

- 推送到 `master` 自动编译
- 提交 PR 自动检查
- 每次构建都会生成可下载 Artifact
- 推送版本标签时自动发布 GitHub Release 并上传 jar

常用命令：

```bash
git push origin master
```

发正式版：

```bash
git tag v1.0.0
git push origin v1.0.0
```

如果只是下载测试包：

1. 打开仓库 `Actions`
2. 进入最近一次 `build`
3. 下载页面底部的 `Artifacts`

如果你打算发布到资源帖，建议再补：

- 插件下载地址
- 演示截图
- GUI 截图
- 服组测试截图
- 更新日志

---

## 更新日志（可直接附在帖子底部）

### 最新更新
- 新增共享模块配置中心
- 新增 `permissions` 模块
- 新增玩家游戏模式同步
- 新增 `/crossserver modules list|set|clear`
- 改进旧快照兼容性
- 修复默认 teleport 配置缺失问题
- 修复 `crossserver.transfer.reconcile` 权限声明缺失问题

---

## 说明

如果你希望，我还可以继续帮你补一版更适合 MineBBS 的内容，比如：

1. **更营销一点的宣传版**
2. **更技术一点的开发版**
3. **带配色 / 折叠块 / 分栏风格的排版版**
4. **附更新日志和安装教程的正式发布帖版**
