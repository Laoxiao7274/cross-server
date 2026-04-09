# 玩家功能 API

这页适合做：

- 玩家菜单
- NPC 交互
- 自定义家园 / Warp / TPA GUI
- 玩家追踪 / 观战 / 跳转功能

## Homes API

### 这组 API 是干什么的

这组 API 用来操作玩家的跨服家园数据。

你可以用它做：

- 自定义 homes 菜单
- 给 NPC 加“打开家园列表”功能
- 做网页或 Bot 展示玩家家园

### 常用方法

| 方法 | 作用 |
|------|------|
| `listHomes(playerId)` | 读取玩家全部家园 |
| `getDefaultHome(playerId)` | 读取默认家园名 |
| `setDefaultHome(playerId, homeName)` | 设置默认家园 |
| `setHome(player, homeName)` | 把玩家当前位置保存成家园 |
| `teleportHome(player, homeName)` | 传送到家园 |
| `deleteHome(playerId, homeName)` | 删除家园 |

### 带注释示例

```java
UUID playerId = player.getUniqueId();

// 读取玩家已有的全部家园
List<HomeEntry> homes = api.listHomes(playerId);

// 读取默认家园名称，可能为 null
String defaultHome = api.getDefaultHome(playerId);

// 把当前位置保存成名为 main 的家园
String setResult = api.setHome(player, "main");

// 把 main 设为默认家园
String setDefaultResult = api.setDefaultHome(playerId, "main");

// 传送到 main 家园
String teleportResult = api.teleportHome(player, "main");

// 删除旧家园
String deleteResult = api.deleteHome(playerId, "old-home");
```

### 参数怎么理解

- `playerId`：玩家 UUID，用于读取/删除数据
- `player`：在线玩家对象，用于“设置当前位置”或“执行传送”
- `homeName`：家园名称，和命令里的名字一样

### 返回值怎么理解

这些方法大多返回 `String`，因为内部原本就是命令风格消息。

你可以：

- 直接发给玩家
- 记录到日志
- 自己再包装成 GUI 提示

## Warps API

### 这组 API 是干什么的

这组 API 用来操作全局 Warp。

适合：

- 自定义 Warp 菜单
- 服务器大厅菜单
- 统一公共地点入口

### 常用方法

| 方法 | 作用 |
|------|------|
| `listWarps()` | 列出全部 Warp |
| `setWarp(player, warpName)` | 把玩家当前位置设置为 Warp |
| `teleportWarp(player, warpName)` | 传送到指定 Warp |
| `deleteWarp(warpName, actorName)` | 删除 Warp |

### 带注释示例

```java
// 读取所有 Warp
List<WarpEntry> warps = api.listWarps();

// 把当前位置保存成 spawn
String setResult = api.setWarp(player, "spawn");

// 传送到 spawn
String teleportResult = api.teleportWarp(player, "spawn");

// 删除一个 Warp
String deleteResult = api.deleteWarp("old-spawn", "my-plugin");
```

### 参数怎么理解

- `warpName`：Warp 名称
- `player`：在线玩家对象，因为要读取当前位置或执行传送
- `actorName`：操作来源标识，建议传你的插件名

## TPA API

### 这组 API 是干什么的

这组 API 用来创建、查询、接受、拒绝、取消 TPA 请求。

你可以用它做：

- 自定义 TPA GUI
- NPC 发起互传
- 网页 / Bot 操作 TPA

### 常用方法

| 方法 | 作用 |
|------|------|
| `createTpaRequest(...)` | 创建 TPA / TPAHERE 请求 |
| `getLatestTpaRequest(receiverId)` | 取最近一条待处理请求 |
| `listPendingTpaRequests(receiverId)` | 取全部待处理请求 |
| `getTpaRequestStatus(receiverId, senderId)` | 查某条请求状态 |
| `acceptTpaRequest(receiver, senderId)` | 接受请求 |
| `denyTpaRequest(receiverId, senderId)` | 拒绝请求 |
| `cancelOutgoingTpaRequests(senderId)` | 取消自己发出的请求 |

### 带注释示例

```java
// 发起一个普通 TPA 请求：sender 想传送到 receiver 身边
boolean created = api.createTpaRequest(
    senderId,
    senderName,
    receiverId,
    receiverName,
    "lobby",
    TeleportRequestService.TpaType.TPA
);

// 查看 receiver 当前收到的全部待处理请求
List<TeleportRequestService.PendingRequest> pending = api.listPendingTpaRequests(receiverId);

// 查询指定请求状态
TeleportRequestService.RequestStatus status = api.getTpaRequestStatus(receiverId, senderId);

// receiver 接受 sender 的请求
CrossServerApi.TpaActionResult accepted = api.acceptTpaRequest(receiverPlayer, senderId);

// receiver 拒绝 sender 的请求
CrossServerApi.TpaActionResult denied = api.denyTpaRequest(receiverId, senderId);

// sender 取消自己全部已发出的请求
List<TeleportRequestService.PendingRequest> cancelled = api.cancelOutgoingTpaRequests(senderId);
```

### 参数怎么理解

- `senderId / senderName`：发起请求的玩家
- `receiverId / receiverName`：接收请求的玩家
- `senderServerId`：请求发起者当前所在服务器 ID
- `receiver`：接受请求时必须传在线玩家对象，因为可能要立即执行传送

### 返回值怎么理解

`acceptTpaRequest(...)` / `denyTpaRequest(...)` 返回 `TpaActionResult`：

- `success()`：是否成功
- `message()`：结果消息
- `request()`：对应的请求对象

## 玩家位置 API

### 这组 API 是干什么的

这组 API 用来读取玩家最近一次同步的位置快照。

适合：

- 观战插件
- 管理员跳转工具
- 自定义跨服追踪 / 传送功能

### 带注释示例

```java
Optional<PlayerLocationSnapshot> location = api.getPlayerLocation(playerId);

if (location.isPresent()) {
    // 检查位置快照是否足够新鲜，避免拿很久之前的位置做传送
    boolean fresh = api.isPlayerLocationFresh(location.get());
    if (fresh) {
        // 转成跨服传送目标
        TeleportTarget target = api.toTeleportTarget(location.get());
    }
}
```

## Auth 玩家侧业务态 API

### 这组 API 是干什么的

这组 API 用来判断玩家当前认证状态。

适合：

- 自定义登录 GUI
- 自定义进服拦截逻辑
- 某些功能要求“必须登录后才能使用”

### 带注释示例

```java
boolean authenticated = api.isAuthenticated(playerId);

// 这个值通常表示：当前这个玩家是否应该被阻止继续操作
boolean blocked = api.shouldBlockUnauthenticatedPlayer(playerId);

// 如果你要做更细逻辑，也可以直接取 ticket
Optional<AuthTicket> ticket = api.loadAuthTicket(playerId);
```

## 底层玩家数据快照

如果你的插件要保存“自己的玩家私有数据”，可以直接用底层快照：

```java
// 注册你自己的命名空间
api.registerNamespace("my-plugin.player-data");

// 保存玩家数据 payload
api.savePlayerData(playerId, "my-plugin.player-data", payload);

// 读取玩家数据 payload
Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, "my-plugin.player-data");
```

适合：

- 任务进度
- 技能点
- 自定义属性
- 小型成就系统
