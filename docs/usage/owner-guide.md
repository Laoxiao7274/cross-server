# 服主指南

这份文档写给：

- 服主
- 普通管理员
- 不写代码，但要负责服务器正常运行的人

## 你最常会用到什么

如果你是服主，最常接触的是这些：

- 给玩家分权限
- 创建 Warp
- 打开或关闭某些模块
- 用 Web 面板看整个集群状态
- 改完配置后重载插件

## 一、你要知道的主要功能

| 功能 | 你会拿它做什么 |
|------|----------------|
| 家园 | 让玩家保存多个家并跨服返回 |
| Warp | 给全服设置公共传送点 |
| TPA | 让玩家互相请求传送 |
| 认证 | 做登录/注册系统 |
| Web 面板 | 用网页统一管理多台子服 |
| 模块开关 | 快速关闭某个功能模块 |
| 路由管理 | 控制跨服目标映射 |

## 二、常用管理员命令

### 1. 查看状态

| 命令 | 作用 |
|------|------|
| `/crossserver status` | 看当前节点状态 |
| `/crossserver nodes` | 看所有子服节点是否在线 |
| `/crossserver node <serverId>` | 看某台节点详情 |

### 2. 重载插件

| 命令 | 作用 |
|------|------|
| `/crossserver reload` | 重载插件配置与服务 |

适合这些时候使用：

- 改了共享模块开关
- 改了共享路由
- 节点配置写回后想马上生效

### 3. 管理模块开关

| 命令 | 作用 |
|------|------|
| `/crossserver modules list` | 查看模块状态 |
| `/crossserver modules set <module> <true|false>` | 开启或关闭共享模块 |
| `/crossserver modules clear <module>` | 取消共享覆盖，恢复本地默认 |

支持的模块：

- `auth`
- `homes`
- `warps`
- `tpa`
- `route-config`
- `transfer-admin`
- `economy-bridge`
- `permissions`

### 4. 管理共享路由

| 命令 | 作用 |
|------|------|
| `/crossserver route list` | 查看共享路由 |
| `/crossserver route menu` | 打开路由管理 GUI |
| `/crossserver route set <serverId> <proxyServer>` | 设置路由 |
| `/crossserver route remove <serverId>` | 删除路由 |

## 三、玩家相关功能，你通常怎么用

### 1. 给玩家家园功能

通常给这些权限：

- `crossserver.homes.list`
- `crossserver.homes.menu`
- `crossserver.homes.teleport`
- `crossserver.homes.set`
- `crossserver.homes.delete`
- `crossserver.homes.default`

### 2. 给玩家 Warp 功能

通常给：

- `crossserver.warps.list`
- `crossserver.warps.teleport`

如果是管理员要创建 Warp，再额外给：

- `crossserver.warps.set`
- `crossserver.warps.delete`

### 3. 给玩家 TPA 功能

通常给：

- `crossserver.tpa.use`
- `crossserver.tpa.here`

### 4. 给玩家登录认证功能

如果你启用了认证模块，通常给：

- `crossserver.auth.login`
- `crossserver.auth.register`
- `crossserver.auth.changepassword`

## 四、Web 面板怎么用

如果你开启了 Web 面板，很多管理事情都可以改到网页里做。

### 面板有哪些页面

| 页面 | 用途 |
|------|------|
| 仪表盘 | 查看整个集群状态 |
| 模块 | 开关模块 |
| 路由 | 改共享路由 |
| 配置文档 | 查看和编辑配置中心文档 |
| 节点配置 | 改子服的 messaging / webPanel / modules |
| 日志中心 | 查看各节点日志 |
| 转服诊断 | 排查玩家跨服问题 |

### 服主最常用的几个页面

#### 仪表盘

用来看：

- 有哪些节点在线
- 哪个节点是 Web 主控节点
- 整个集群是否正常

#### 模块页

适合：

- 临时关闭 TPA
- 暂时关闭认证
- 关闭某个还没准备好的功能

#### 节点配置页

适合：

- 统一调整 Redis 配置
- 调整某台子服的 Web 面板配置
- 给某台服单独开启/关闭模块

## 五、你最常遇到的几个操作场景

### 场景 1：新服上线了

你通常要做：

1. 配好 `server.id`
2. 把它接入同一个 MySQL
3. 如需实时同步，接入同一个 Redis
4. 确保代理里有对应服务器名
5. 用 `/crossserver nodes` 检查是否在线

### 场景 2：想临时关闭 TPA

做法：

```text
/crossserver modules set tpa false
/crossserver reload
```

### 场景 3：想给全服加一个公共传送点

做法：

```text
/setwarp spawn
```

然后玩家就可以：

```text
/warp spawn
```

### 场景 4：改了路由，但没生效

说明你还需要重载当前节点：

```text
/crossserver reload
```

## 六、服主最容易踩坑的地方

### 1. 改了共享配置，却没马上生效

这是正常现象。

因为共享配置先写入配置中心，不一定让当前节点立即热生效。通常还要再：

```text
/crossserver reload
```

### 2. Web 面板只能本机打开

通常是因为：

- `web-panel.host` 还是 `127.0.0.1`

如果你想让别的机器访问，通常需要：

- 改成 `0.0.0.0` 或服务器实际内网 IP
- 配反向代理

### 3. 普通玩家不需要 `/crossserver` 权限

大多数情况下：

- 普通玩家不需要 `crossserver.command`
- 只给他们家园、Warp、TPA、认证权限即可
