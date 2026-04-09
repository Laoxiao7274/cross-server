# 监听器与事件 API

这页适合做：

- 热更新配置
- 做联动插件
- Bot 通知
- 监控面板
- 告警系统

## 先理解这件事

CrossServer 现在已经提供了很多 facade 监听器。

这意味着你不需要自己记内部 namespace，只要直接注册对应监听器即可。

大多数监听器都会返回一个 `Runnable`：

- 注册时拿到它
- 插件关闭时调用 `run()` 解除监听

## Homes 变化监听

### 什么时候用

- 玩家家园列表更新时自动刷新 GUI
- 玩家 homes 变化时同步到网页或缓存

### 示例

```java
Runnable homesHandle = api.registerHomesListener(event -> {
    // event.playerId() 表示哪个玩家的 homes 数据发生了变化
    getLogger().info("玩家 homes 已变化: " + event.playerId());
});
```

## Warps 变化监听

### 什么时候用

- Warp 列表变化后自动刷新菜单
- 自定义面板同步 Warp 数据

### 示例

```java
Runnable warpsHandle = api.registerWarpsListener(event -> {
    // event.version() 是这次变化后的版本号
    getLogger().info("Warp 数据已变化，version=" + event.version());
});
```

## TPA 请求变化监听

### 什么时候用

- 自定义 TPA GUI 自动刷新
- TPA 请求变化时通知网页 / Bot

### 示例

```java
Runnable tpaHandle = api.registerTpaRequestsListener(event -> {
    getLogger().info("TPA 请求数据已变化，version=" + event.version());
});
```

## Transfer 变化监听

### 什么时候用

- 自定义 transfer 诊断工具
- 监控跨服 handoff 状态变化

### 示例

```java
Runnable transferHandle = api.registerTransferListener(event -> {
    getLogger().info("Transfer handoff 已变化: " + event.playerId());
});
```

## Auth 变化监听

### 什么时候用

- 某玩家认证配置变化时刷新状态
- ticket 更新时联动自定义逻辑

### 示例

```java
Runnable authProfileHandle = api.registerAuthProfileListener(event -> {
    // 认证 profile 变化
    getLogger().info("Auth profile 已变化: " + event.playerId());
});

Runnable authTicketHandle = api.registerAuthTicketListener(event -> {
    // 认证 ticket 变化
    getLogger().info("Auth ticket 已变化: " + event.playerId());
});
```

## 共享模块与共享路由监听

### 什么时候用

- 热更新集群模块配置
- 路由变化时刷新本地缓存或 GUI

### 示例

```java
Runnable modulesHandle = api.registerSharedModulesListener(event -> {
    getLogger().info("共享模块已变化，version=" + event.version());
});

Runnable routesHandle = api.registerSharedRoutesListener(event -> {
    getLogger().info("共享路由已变化，version=" + event.version());
});
```

## 节点配置监听

### 什么时候用

- 管理插件查看节点配置快照是否更新
- 监听节点配置申请状态变化

### 示例

```java
Runnable snapshotHandle = api.registerNodeConfigSnapshotListener("survival-1", event -> {
    // 某个节点上报了新的配置快照
    getLogger().info("节点配置快照已变化: " + event.serverId());
});

Runnable applyHandle = api.registerNodeConfigApplyListener("survival-1", event -> {
    // 某个节点配置申请状态已变化
    getLogger().info("节点配置申请状态已变化: " + event.serverId());
});
```

## 底层同步监听

如果你需要更底层的控制，依然可以直接监听 namespace：

```java
api.registerSyncListener("my-plugin.player-data", message -> {
    // 监听你自己的命名空间数据变化
    getLogger().info("Data updated: " + message.namespace() + "/" + message.targetId());
});

Runnable handle = api.registerSyncListenerHandle("my-plugin.player-data", listener);

// 在插件关闭时解除监听
handle.run();
```

## 给新手的使用建议

### 1. 优先用 facade 监听器

比如：

- 想监听 homes 变化，就用 `registerHomesListener(...)`
- 不要一开始就自己记 `homes` namespace

### 2. 监听器里不要做重 I/O

如果你要：

- 查数据库
- 调 HTTP
- 写文件

建议自己切异步。

### 3. 记得在插件关闭时解除监听

只要方法返回 `Runnable`，就最好在 `onDisable()` 里调用它的 `run()`。
