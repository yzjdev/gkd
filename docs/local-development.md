# 本地开发产物

自定义本地产物按用途分别放在仓库根目录 `.local/` 与 `.temp/`，路径不依赖启动工作目录，禁止混合使用：

- `.local/`：与项目代码及其运行、构建直接相关的结构化数据，包括快照、依赖缓存和构建包。
- `.temp/<任务名>/`：临时脚本、排查日志、中间文件和一次性杂项输出。不得在 `.local/` 堆放这些文件或继续使用 `.local/tmp/`，也不得将应用数据放入 `.temp/`。

工具标准目录（`.gradle`、`.kotlin`、`node_modules`、模块 `build`）保持默认。应用自身存储布局中的缓存、日志与临时文件仍由应用管理，不属于开发任务的临时杂项。

selector 下载中间文件位于 `.temp/selector-dist/download-*/`，操作结束后清理各自会话目录。

| 相对 `.local/` 的目录 | 用途 |
| --- | --- |
| `snapshots/` | 调试、分析使用的快照 JSON 与关联图片 |
| `cache/` | 可重新获取的依赖及工具缓存 |
| `verifications/<会话ID>/` | Android 验证截图、日志和恢复记录 |
| `backups/` | 主动保留的备份 |

## 应用存储布局

`AppStorageLayout` 由宿主注入 files/cache/privateFiles 根目录，Android 使用外部应用目录：

| 相对 `data/` 的路径 | 消费者 |
| --- | --- |
| `files/db/gkd.db` | Room 数据库 |
| `files/store/` | 生产设置 |
| `files/subscription/` | 订阅文件 |
| `files/snapshot/` | 应用自身快照 |
| `files/log/` | 普通运行日志；按日期和 2 MiB 滚动，总量 16 MiB，保留 7 天 |
| `files/matching-diagnostics/<记录ID>/` | 每次采集的 `diagnostic.json` 及附件；记录 ID 为开始时间的毫秒时间戳，摘要保存在数据库 `match_diagnostic` 表。停止时保存未完成现场，收尾后更新，诊断页分享附带选中的目录，关于页导出日志包含所有已保存诊断 |
| `files/crash/temp/` | 临时崩溃文件 |
| `files/private-store/` | 私有配置 |
| `cache/coil/` | 图片缓存 |
| `cache/webview/` | 网页用户数据 |
| `cache/shared/` | 备份、日志等共享输出 |

数据目录视为私有，privateFiles 与 files 使用不同根目录；Android 私有存储使用系统内部目录。

## 隔离与清理

Android 验证在 `.local/verifications/<会话ID>/` 保存截图、日志和恢复记录，不写入日常应用数据。

Gradle clean 不删除本地产物。缓存和临时文件可按需清理；快照、备份和验证会话产物必须保留。历史报告保留原名称，不推测所属会话。可复用工具提交到源码目录，不长期放在忽略目录；不增加旧目录回退或双写。

日志写入队列最多 1024 条、2 MiB，单条最多 64 KiB；超限会标记截断或累计丢弃数量，队列拥塞时优先保留较高级别。Release 文件保留 INFO/WARN/ERROR，DEBUG 仅调试构建采集。公用日志分享先复制稳定的日志文件集，`metadata/log-writer.txt` 包含当前进程的丢弃、截断及写入失败状态。
