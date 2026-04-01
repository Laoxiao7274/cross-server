# 安装

## 环境要求

- Java 21
- Paper 1.21+
- MySQL 8+
- Redis（可选，启用跨服广播时需要）

## 安装步骤

1. 将构建后的插件 jar 放入每台子服的 `plugins/` 目录。
2. 确保所有子服使用相同版本的 `cross-server`。
3. 所有子服连接同一个 MySQL。
4. 如需跨服广播，所有子服连接同一个 Redis。
5. 每台子服配置不同的 `server.id`。

## 配置文件位置

`plugins/cross-server/config.yml`

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
