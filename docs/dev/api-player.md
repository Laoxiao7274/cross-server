# 玩家功能 API

这页适合做：

- 自定义玩家菜单
- NPC 交互
- 玩家功能扩展插件
- 观战 / 跳转 / 快速传送工具

## Homes API

```java
List<HomeEntry> homes = api.listHomes(playerId);
String defaultHome = api.getDefaultHome(playerId);

String setDefaultResult = api.setDefaultHome(playerId, "main");
String setResult = api.setHome(player, "main");
String teleportResult = api.teleportHome(player, "main");
String deleteResult = api.deleteHome(playerId, "old-home");
```

适合：

- 自定义家园 GUI
- NPC 打开玩家家园列表
- 网页或 Bot 展示玩家 homes

## Warps API

```java
List<WarpEntry> warps = api.listWarps();

String setResult = api.setWarp(player, "spawn");
String teleportResult = api.teleportWarp(player, "spawn");
String deleteResult = api.deleteWarp("spawn", "my-plugin");
```

适合：

- 自定义 Warp GUI
- 服务器菜单插件
- 公共地点传送入口

## TPA API

```java
boolean created = api.createTpaRequest(senderId, senderName, receiverId, receiverName, "lobby", TeleportRequestService.TpaType.TPA);

Optional<TeleportRequestService.PendingRequest> latest = api.getLatestTpaRequest(receiverId);
List<TeleportRequestService.PendingRequest> pending = api.listPendingTpaRequests(receiverId);
TeleportRequestService.RequestStatus status = api.getTpaRequestStatus(receiverId, senderId);

CrossServerApi.TpaActionResult accepted = api.acceptTpaRequest(receiverPlayer, senderId);
CrossServerApi.TpaActionResult denied = api.denyTpaRequest(receiverId, senderId);

List<TeleportRequestService.PendingRequest> cancelled = api.cancelOutgoingTpaRequests(senderId);
```

### 适合什么场景

- 自定义 TPA GUI
- NPC 发起玩家互传
- Bot 或网页面板处理 TPA 请求

## 玩家位置 API

```java
Optional<PlayerLocationSnapshot> location = api.getPlayerLocation(playerId);
if (location.isPresent() && api.isPlayerLocationFresh(location.get())) {
    TeleportTarget target = api.toTeleportTarget(location.get());
}
```

适合：

- 观战插件
- 玩家追踪系统
- 管理员快速跳转工具
- 基于玩家当前位置的跨服传送入口

## Auth 玩家侧业务态 API

```java
boolean authenticated = api.isAuthenticated(playerId);
boolean blocked = api.shouldBlockUnauthenticatedPlayer(playerId);
Optional<AuthTicket> ticket = api.loadAuthTicket(playerId);
```

适合：

- 自定义登录界面
- 玩家进服限制检查
- 某些功能要求“登录后才能使用”的插件

## 底层玩家数据快照

如果你想存储自己插件的玩家私有数据：

```java
api.registerNamespace("my-plugin.player-data");
api.savePlayerData(playerId, "my-plugin.player-data", payload);

Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, "my-plugin.player-data");
```

适合：

- 任务进度
- 技能点
- 玩家自定义数据
- 小型成就系统
