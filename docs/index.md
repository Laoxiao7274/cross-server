# CrossServer 文档

CrossServer 是一个 **Paper 1.21+ 跨服数据同步插件**，为 Velocity / BungeeCord 代理下的多子服集群提供完整的数据同步解决方案。

## 核心功能

| 功能 | 说明 |
|------|------|
| 背包 & 末影箱同步 | 切服自动恢复，不怕丢东西 |
| 跨服经济 | Vault 兼容，所有子服共用钱包 |
| 跨服家园 | `/home` 自动跨服传送 |
| 全局 Warp | `/warp` 命令与 GUI 浏览，支持跨服传送 |
| TPA / TPAHERE | 支持同服与跨服玩家请求传送 |
| 玩家状态同步 | 血量、饥饿、经验、等级 |
| 登录认证 | 跨服免重登 Ticket 机制 |
| 跨服传送 | 完整 handoff 生命周期 + GUI 管理 |
| 故障恢复 | 插件关闭保护、失败回滚、状态修复 |
| 节点监控 | 集群节点心跳与在线状态 |
| 内置 Web 面板 | 多页面运维管理面板（仪表盘 / 模块 / 路由 / 配置文档 / 节点配置 / 日志中心 / 转服诊断） |
| 节点配置远程管理 | 在线编辑各子服 messaging / 面板 / 模块配置并提交重载 |
| 日志中心 | 按节点查看 CrossServer 插件日志 |
| 开放 API | 其他插件可直接接入 |

## 快速开始

**第一次使用？** 按顺序阅读：

1. [安装教程](setup/installation.md) — 从零开始，每一步都有说明
2. [配置指南](setup/config.md) — 所有配置项的详细解释
3. [Velocity 代理](setup/velocity.md) — 配置代理实现跨服传送

**已安装完成？** 查看使用指南：

- [使用指南总览](usage/commands.md) — 先看自己该读哪一份
- [玩家指南](usage/player-guide.md) — 家园 / Warp / TPA / 登录认证
- [服主指南](usage/owner-guide.md) — 功能管理 / 权限分配 / Web 面板使用
- [技术管理员指南](usage/admin-guide.md) — 节点管理 / 诊断修复 / 共享配置 / Web API
- [路由管理](usage/routes.md) — 跨服传送路由配置

**开发者？** 查看技术文档：

- [架构概览](dev/architecture.md) — 从启动装配、核心服务、命名空间、传送链路到代码阅读入口的完整说明
- [API 总览](dev/api.md) — 先看 API 分页结构与接入入口
- [玩家功能 API](dev/api-player.md) — homes / warps / TPA / 玩家位置 / auth 玩家侧能力
- [配置与运维 API](dev/api-platform.md) — 配置中心、共享模块、共享路由、节点配置、transfer 管理
- [监听器与事件 API](dev/api-events.md) — homes / warps / TPA / auth / transfer / shared config / node config 监听器
