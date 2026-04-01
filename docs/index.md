# cross-server

一个基于 Paper 的跨服数据同步插件，当前包含：

- MySQL 持久化存储
- Redis 跨服广播与失效通知
- 玩家会话锁
- economy 跨服经济模块
- 背包 / 末影箱 / 玩家状态跨服同步
- homes 跨服家园数据
- auth 登录认证与短时跨服免重登
- teleport.handoff 跨服传送基础设施
- 节点心跳、路由管理、菜单化运维入口

## 快速开始

1. 阅读[安装](setup/installation.md)
2. 如果要启用跨服传送，继续阅读[Velocity 代理](setup/velocity.md)
3. 查看[命令与权限](usage/commands.md)
4. 查看[路由管理](usage/routes.md)

## 本地预览

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

## 构建静态站点

```bash
pip install -r requirements-docs.txt
mkdocs build
```
