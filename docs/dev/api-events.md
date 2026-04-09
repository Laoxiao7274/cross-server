# 监听器与事件 API

这页适合做：

- 热更新配置
- 联动其他插件
- 做运维看板
- 做通知 / 告警 / Bot 集成

## Homes 变化监听

```java
Runnable homesHandle = api.registerHomesListener(event -> {
    getLogger().info("玩家 homes 已变化: " + event.playerId());
});
```

## Warps 变化监听

```java
Runnable warpsHandle = api.registerWarpsListener(event -> {
    getLogger().info("Warp 数据已变化，version=" + event.version());
});
```

## TPA 请求变化监听

```java
Runnable tpaHandle = api.registerTpaRequestsListener(event -> {
    getLogger().info("TPA 请求数据已变化，version=" + event.version());
});
```

## Transfer 变化监听

```java
Runnable transferHandle = api.registerTransferListener(event -> {
    getLogger().info("Transfer handoff 已变化: " + event.playerId());
});
```

## Auth 变化监听

```java
Runnable authProfileHandle = api.registerAuthProfileListener(event -> {
    getLogger().info("Auth profile 已变化: " + event.playerId());
});

Runnable authTicketHandle = api.registerAuthTicketListener(event -> {
    getLogger().info("Auth ticket 已变化: " + event.playerId());
});
```

## 共享模块与共享路由监听

```java
Runnable modulesHandle = api.registerSharedModulesListener(event -> {
    getLogger().info("共享模块已变化，version=" + event.version());
});

Runnable routesHandle = api.registerSharedRoutesListener(event -> {
    getLogger().info("共享路由已变化，version=" + event.version());
});
```

## 节点配置监听

```java
Runnable snapshotHandle = api.registerNodeConfigSnapshotListener("survival-1", event -> {
    getLogger().info("节点配置快照已变化: " + event.serverId());
});

Runnable applyHandle = api.registerNodeConfigApplyListener("survival-1", event -> {
    getLogger().info("节点配置申请状态已变化: " + event.serverId());
});
```

## 底层同步监听

如果你需要更底层的控制，仍然可以直接监听 namespace：

```java
api.registerSyncListener("my-plugin.player-data", message -> {
    getLogger().info("Data updated: " + message.namespace() + "/" + message.targetId());
});

Runnable handle = api.registerSyncListenerHandle("my-plugin.player-data", listener);
handle.run();
```

## 使用建议

- 长生命周期监听器优先用返回 `Runnable` 的版本
- 插件关闭时记得调用 `handle.run()` 解除监听
- 如果监听器里要做重 I/O，自己切异步
- 优先使用 facade 监听器，而不是手写内部 namespace 字符串
