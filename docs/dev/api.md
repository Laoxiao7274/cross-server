# API 总览

这套 API 是给其他插件开发者用的。

如果你以前没接过 CrossServer，先记住一句话：

**优先用功能级 API，不要一上来就自己拼底层快照 payload。**

也就是说：

- 想做家园功能，优先用 Homes API
- 想做 Warp 功能，优先用 Warps API
- 想做 TPA 功能，优先用 TPA API
- 想做共享配置，优先用配置中心文档 API
- 想监听变化，优先用 facade 监听器

## 这份文档怎么读

为了更方便新手理解，API 文档已经拆成 3 类：

- [玩家功能 API](api-player.md)
- [配置与运维 API](api-platform.md)
- [监听器与事件 API](api-events.md)

建议阅读顺序：

1. 先看本文
2. 再看 [玩家功能 API](api-player.md)
3. 然后看 [配置与运维 API](api-platform.md)
4. 最后看 [监听器与事件 API](api-events.md)

## CrossServerApi 是什么

`CrossServerApi` 是 CrossServer 对外暴露的统一入口。

它不是单纯几个工具方法，而是把以下能力集中暴露给第三方插件：

- 玩家数据快照
- 全局数据快照
- homes / warps / TPA 功能
- 玩家位置查询
- 配置中心文档
- 共享模块与共享路由
- 节点配置
- transfer 运维能力
- auth 业务态与管理能力
- 多种监听器与事件 facade

## 第一步：获取 API

```java
import org.bukkit.Bukkit;
import org.xiaoziyi.crossserver.api.CrossServerApi;

CrossServerApi api = Bukkit.getServicesManager().load(CrossServerApi.class);
if (api == null) {
    // 说明 CrossServer 没有加载，或者当前服务器没装这个插件
    return;
}
```

### 说明

- 这段代码通常放在你的 `onEnable()` 里
- 建议拿到之后缓存到你自己的插件类字段里
- 如果 `api == null`，不要继续调用，否则一定会空指针

## 什么时候用哪类 API

| 你的目标 | 优先使用 |
|----------|----------|
| 给玩家做菜单、传送、交互功能 | [玩家功能 API](api-player.md) |
| 做共享配置、运维、节点管理 | [配置与运维 API](api-platform.md) |
| 监听变化做联动、热更新、告警 | [监听器与事件 API](api-events.md) |
| 存你自己插件的私有数据 | 玩家/全局快照 API |

## 什么时候才该用底层快照 API

CrossServer 依然保留底层能力，比如：

- `registerNamespace(...)`
- `savePlayerData(...)`
- `loadPlayerData(...)`
- `saveGlobalData(...)`
- `loadGlobalData(...)`
- `registerSyncListener(...)`

这些适合：

- 你在做自己的全新功能
- CrossServer 还没有现成功能级 facade
- 你明确知道自己要维护 payload 格式

如果项目里已经有现成 facade，不建议回退到底层自己重复造协议。

## 给新手的简单建议

### 1. 优先调用功能级 API

比如：

- 不要自己解析 `homes` 命名空间 payload，直接用 Homes API
- 不要自己解析 `teleport.requests`，直接用 TPA API

### 2. 想做共享配置，优先用配置中心文档

不要把复杂配置直接塞进裸 `global_snapshot`。

### 3. 想响应变化，优先用 facade 监听器

不要一开始就自己记内部 namespace 字符串。

## 下一页看什么

- 如果你想做玩家功能：看 [玩家功能 API](api-player.md)
- 如果你想做配置 / 面板 / 管理：看 [配置与运维 API](api-platform.md)
- 如果你想做监听、热更新、联动：看 [监听器与事件 API](api-events.md)
