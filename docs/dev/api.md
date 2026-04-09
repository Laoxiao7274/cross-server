# API 总览

CrossServer 通过 Bukkit `ServicesManager` 暴露 `CrossServerApi`，其他插件可以把它当成一套“跨服平台能力入口”。

## 这份文档怎么读

为了更方便浏览，API 文档已经拆成多页：

- [玩家功能 API](api-player.md) — homes / warps / TPA / 玩家位置 / auth 玩家侧能力
- [配置与运维 API](api-platform.md) — 配置中心、共享模块、共享路由、节点配置、transfer 管理
- [监听器与事件 API](api-events.md) — homes / warps / TPA / auth / transfer / shared config / node config 等监听器

如果你是第一次接入，建议阅读顺序：

1. 先看本文，理解 API 定位
2. 再看 [玩家功能 API](api-player.md)
3. 接着看 [配置与运维 API](api-platform.md)
4. 最后看 [监听器与事件 API](api-events.md)

## API 定位

`CrossServerApi` 现在已经不只是底层工具类，而是一套逐步平台化的 facade，主要覆盖这些能力：

- 玩家数据快照
- 全局数据快照
- homes / warps / TPA 玩家功能
- 玩家位置读取与跨服目标转换
- 配置中心文档
- 共享模块配置
- 共享路由
- 节点配置
- transfer 诊断与修复
- auth 业务态与管理能力
- 多种模块监听器

## 获取 API

```java
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.bukkit.Bukkit;

CrossServerApi api = Bukkit.getServicesManager().load(CrossServerApi.class);
if (api == null) {
    return;
}
```

建议在插件 `onEnable()` 中获取并缓存。

## 最常见的接入路径

| 需求 | 推荐页面 |
|------|----------|
| 做玩家菜单或玩家功能增强 | [玩家功能 API](api-player.md) |
| 做共享配置、节点管理或运维插件 | [配置与运维 API](api-platform.md) |
| 想监听变化做热更新或联动 | [监听器与事件 API](api-events.md) |

## 命名空间与底层能力

如果你要直接使用底层快照能力，仍然可以使用：

- `registerNamespace(...)`
- `savePlayerData(...)` / `loadPlayerData(...)`
- `saveGlobalData(...)` / `loadGlobalData(...)`
- `registerSyncListener(...)`

但如果项目里已经有功能级 facade，优先使用 facade，而不是自己手写 payload 协议。

## 一个简单建议

- 想做“功能调用”：优先用 facade
- 想做“共享配置”：优先用配置中心文档
- 想做“变化联动”：优先用监听器 facade
- 只有在没有现成 facade 的情况下，再回退到底层 namespace 快照
