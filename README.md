# CrossServer

> 一个面向 **Paper 1.21+ 多子服集群** 的跨服数据同步插件。  
> 背包不丢、经济共通、家园跨服、Warp 共享、TPA 跨服、Web 面板统一运维。

[![Build](https://img.shields.io/github/actions/workflow/status/Laoxiao7274/cross-server/build.yml?branch=master&label=build)](https://github.com/Laoxiao7274/cross-server/actions)
[![Release](https://img.shields.io/github/v/release/Laoxiao7274/cross-server?label=release)](https://github.com/Laoxiao7274/cross-server/releases)
[![Docs](https://img.shields.io/badge/docs-online-4db6ff)](https://laoxiao7274.github.io/cross-server/)
[![Java](https://img.shields.io/badge/java-21%2B-orange)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/paper-1.21%2B-blue)](https://papermc.io/)

适用于 **Velocity / BungeeCord** 代理下的多子服环境。  
插件以 **MySQL** 作为持久化中心，以 **Redis** 作为可选实时消息层，并内置 **Web 面板、配置中心、节点配置远程管理、日志中心、转服诊断** 等运维能力。

## 为什么用它

如果你有多个子服，你大概率会遇到这些问题：

- 玩家切服后背包、状态、位置体验割裂
- 家园、Warp、TPA 只能在单服里用
- 管理员需要在多台子服重复改配置
- 某个玩家跨服卡住时，很难快速排查原因
- 多服共用经济、认证、权限时，维护成本很高

CrossServer 的目标就是把这些问题一次性解决掉。

## 核心卖点

### 面向玩家的体验

- **背包 & 末影箱同步**：切服自动恢复，不怕丢物品
- **玩家状态同步**：血量、饥饿、经验、等级、游戏模式可跨服保持一致
- **跨服家园**：`/home`、`/homes`、`/sethome` 直接跨服使用
- **全局 Warp**：`/warp` + GUI 菜单，支持本服与跨服传送
- **跨服 TPA / TPAHERE**：向其他子服在线玩家发起传送请求
- **登录认证**：注册、登录、改密，支持跨服免重登 Ticket
- **跨服经济**：兼容 Vault，所有子服共用同一份余额

### 面向服主的管理能力

- **共享模块开关**：统一开关 `auth / homes / warps / tpa / permissions` 等模块
- **共享路由表**：统一维护 `serverId -> proxyServer` 映射
- **热重载**：`/crossserver reload` 可直接重载当前节点
- **图形化菜单**：Homes、Warp、路由管理、Transfer 诊断都有 GUI
- **文档站点**：安装、配置、玩家使用、技术运维全部分层文档化

### 面向技术管理员的运维能力

- **内置 Web 面板**：主控节点统一查看整个集群
- **节点配置远程管理**：在线修改目标节点的 `messaging / webPanel / modules`
- **配置中心文档**：支持第三方插件注册共享配置文档
- **JSON / YAML 双格式配置文档编辑**：Web 面板可直接编辑并保存
- **配置文档 schema 约束**：可为共享配置文档注册字段规则，保存时做服务端校验
- **配置文档历史记录**：保留最近版本，便于回看配置演进
- **运维历史记录**：模块、路由、节点配置申请都开始保留最近变更历史
- **回滚能力**：可从历史记录中回滚配置文档、共享模块、共享路由到指定版本
- **日志中心**：按节点查看插件同步日志
- **转服诊断与修复**：查看 handoff、history、recovery 状态，并执行 reconcile / clear
- **开放 API**：其他插件可直接接入同步与配置中心能力
- **开放 API**：其他插件可直接接入 homes、warps、TPA、共享路由、共享模块、配置文档、transfer 修复等能力

## 功能一览

| 功能分类 | 说明 |
|----------|------|
| 背包同步 | 背包、末影箱、玩家状态跨服一致 |
| 家园系统 | 家园跨服保存与回家 |
| Warp 系统 | 全局 Warp、GUI 菜单、跨服传送 |
| TPA 系统 | 支持跨服玩家请求传送 |
| 认证系统 | 注册、登录、改密、跨服免重登 |
| 经济桥接 | Vault 兼容，共用余额与流水 |
| 权限同步 | 可选同步 `crossserver.*` 权限节点 |
| 路由管理 | 本地配置 + 共享覆盖 |
| 模块配置 | 本地默认 + 共享覆盖 |
| 节点监控 | 节点心跳、状态、在线视图 |
| Web 面板 | 集群总览、模块、路由、配置文档、节点配置、日志、转服诊断 |
| 配置中心 | 第三方插件共享配置文档 |
| 远程节点配置 | 主控节点提交配置变更到目标节点 |

## Web 面板预期体验

CrossServer 不是只有命令行。

开启 `web-panel.enabled: true` 后，你可以得到一个适合多服运维的内置控制台：

- **仪表盘**：查看节点、主控节点、集群概览
- **模块页**：统一开关模块
- **路由页**：集中修改共享路由
- **配置文档页**：编辑 JSON / YAML 配置文档
- **配置文档页**：编辑 JSON / YAML 配置文档，并查看 schema 约束与最近历史
- **节点配置页**：修改某台子服的 Redis / Web 面板 / 模块配置
- **节点配置页**：修改某台子服的 Redis / Web 面板 / 模块配置
- **日志中心**：按节点看插件日志
- **转服诊断页**：定位某个玩家跨服问题

同时，Web 面板现在也更适合真实运维场景：

- 高风险操作会给出确认提示
- 配置文档页会展示 schema 约束与历史记录
- 模块页与路由页会展示最近历史记录
- 节点配置详情页会展示最近申请历史
- 历史记录支持查看详情与回滚到指定版本
- 空数据区域会给出引导说明
- 错误态与风险操作按钮更明显

这意味着你不用每次都 SSH 上每一台机器改配置，很多事情都可以直接在浏览器里完成。

## 适合哪些服

CrossServer 特别适合这些场景：

- 大厅服 + 生存服 + 资源服 + 副本服
- 多节点小游戏集群
- 需要统一经济与认证的群组服
- 想把管理操作从命令行迁移到网页面板的服

如果你只有单服，它也能工作，但优势会主要体现在：

- 经济桥接
- 认证系统
- GUI 与 Web 运维体验

## 环境要求

| 组件 | 要求 | 说明 |
|------|------|------|
| Java | 21+ | 必须 |
| 服务端 | Paper 1.21+ | 不支持 Spigot / CraftBukkit |
| MySQL | 8.0+ | 必需，存储跨服数据 |
| Redis | 6.0+ | 可选，用于实时广播 |
| 代理 | Velocity / BungeeCord | 需要跨服传送时必须 |

## 快速开始

### 1. 准备数据库

```sql
CREATE DATABASE cross_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

插件启动时会自动建表，不需要手动建表结构。

### 2. 下载插件

从 [Releases](https://github.com/Laoxiao7274/cross-server/releases) 下载最新版本，放入每台子服的 `plugins/` 目录。

### 3. 配置关键项

首次启动后会生成 `plugins/cross-server/config.yml`。

一个典型配置示例如下：

```yaml
server:
  id: "lobby"
  cluster: "my-cluster"

database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "root"
  password: "your-password"

messaging:
  enabled: true
  redis-uri: "redis://127.0.0.1:6379/0"
  channel: "cross-server:sync"

teleport:
  gateway:
    server-map:
      lobby: "lobby"
      survival: "survival"

modules:
  auth: true
  homes: true
  warps: true
  tpa: true
  economy-bridge: true
  permissions: false

web-panel:
  enabled: true
  host: "127.0.0.1"
  port: 8765
  token: "change-this-token"
  master-server-id: "lobby"
  cluster-lease-seconds: 30
  cluster-heartbeat-seconds: 10
```

### 4. 启动后先做这几件事

建议按这个顺序检查：

1. `/crossserver status`
2. `/crossserver nodes`
3. `/homes`、`/warp`、`/tpa` 试跑基础功能
4. 打开 Web 面板确认集群视图正常

## 文档入口

### 给第一次使用的人

- [安装教程](docs/setup/installation.md)
- [配置指南](docs/setup/config.md)
- [Velocity 代理说明](docs/setup/velocity.md)

### 给玩家 / 服主 / 管理员

- [使用指南总览](docs/usage/commands.md)
- [玩家指南](docs/usage/player-guide.md)
- [服主指南](docs/usage/owner-guide.md)
- [技术管理员指南](docs/usage/admin-guide.md)
- [路由管理](docs/usage/routes.md)

### 给开发者

- [架构概览](docs/dev/architecture.md)
- [API 文档](docs/dev/api.md)

### 在线文档站点

- https://laoxiao7274.github.io/cross-server/

## 常见问题

### 改了共享配置却没马上生效

这是正常情况。共享配置先写入配置中心，通常还要执行一次：

```text
/crossserver reload
```

### Web 面板打不开或只能本机访问

优先检查：

- `web-panel.enabled`
- `web-panel.host`
- `web-panel.port`
- `web-panel.master-server-id`
- 防火墙和反向代理设置

### 某个玩家跨服卡住了

建议使用：

```text
/crossserver transfer info <player>
/crossserver transfer reconcile <player>
```

必要时再用：

```text
/crossserver transfer clear <player>
```

## GitHub Actions 与发版

仓库已经配置好自动化流程：

- push `master`：自动执行插件构建
- push `master`：自动构建并部署文档站点
- push `v*` tag：自动构建 JAR、创建 GitHub Release、上传产物

推荐发版步骤：

1. 更新 `pom.xml` 版本号
2. push `master`
3. push 对应 tag，例如 `v1.0.5`

## 项目定位

CrossServer 更像是一个“多子服同步与运维底座”，而不是只做某一个功能的命令插件。

它既能让玩家感受到更丝滑的跨服体验，也能让服主和技术管理员真正把多服运维统一起来。
