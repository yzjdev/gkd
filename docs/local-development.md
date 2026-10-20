# 本地开发产物

所有自定义本地产物统一位于仓库根目录 `.local/`，路径不依赖启动工作目录。工具标准目录（`.gradle`、`.kotlin`、模块 `build`）保持默认。

| 相对 `.local/` 的目录 | 用途 |
| --- | --- |
| `snapshots/` | 调试、分析使用的快照 JSON 与关联图片 |
| `cache/` | 可重新获取的依赖及工具缓存 |
| `tests/android/<会话ID>/` | Android 验证截图、日志和恢复记录 |
| `backups/` | 主动保留的备份 |
| `tmp/<任务名>/` | 临时脚本、中间文件和一次性输出 |

## 应用存储布局

`AppStorageLayout` 由应用注入 files/cache/privateFiles 根目录，Android 使用系统私有目录：

| 相对根目录的路径 | 消费者 |
| --- | --- |
| `files/db/gkd.db` | Room 数据库 |
| `files/store/` | 生产设置 |
| `files/subscription/` | 订阅文件 |
| `files/snapshot/` | 应用自身快照 |
| `files/crash/temp/` | 临时崩溃文件 |
| `files/private-store/` | 私有配置 |
| `cache/coil/` | 图片缓存 |
| `cache/webview/` | 网页用户数据 |
| `cache/shared/` | 备份、日志等共享输出 |

## 隔离与清理

Android 验证产物放在 `.local/tests/android/<会话ID>/`，不写入日常目录。

Gradle clean 不删除日常数据。缓存和临时文件可按需清理；日常数据、备份以及包含未处理恢复记录的会话必须保留。可复用工具提交到源码目录，不长期放在忽略目录；不增加旧目录回退或双写。
