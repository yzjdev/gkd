# 应用架构

本文描述当前设计、依赖和资源用法；强制开发约束见 [AGENTS.md](../AGENTS.md)，本地目录见 [开发产物](local-development.md)。

## 模块与源码归属

依赖方向为 `app` → `core`，`core` 依赖 `gkd-aidl`；`gkd-hidden-api` 仅作为编译期 stub 与 remap 输入。

| 位置 | 职责 |
| --- | --- |
| `core/src/main/kotlin/li/gkd/app` | 业务、Repository、页面、组件、ViewModel、导航、主题与全部 Android 平台实现 |
| `core/src/main/kotlin/li/gkd/db` | Room 实体、DAO、分页查询和数据库事务 |
| `core/src/main/kotlin/li/gkd/selector` | 选择器解析与匹配 |
| `core/src/main/res` | 应用文案与共享原生资源 |
| `app` | Application 清单、原生 res、渠道、签名、打包及 R8 输入 |
| `gkd-aidl` / `gkd-hidden-api` | Binder 协议编译 / Android 隐藏 API stub |

`core` 与 `app` 的 Android namespace 分别是 `li.gkd.app` 和 `li.gkd.android`，页面和 ViewModel 位于 `li.gkd.app.ui.*`，Manifest 使用完整组件类名；已注册组件（含 SelectToSpeakService）保留原身份。

平台能力直接在 `core` 的 `li.gkd.app.*` 中实现，不再区分公共/平台源码集，也不使用 `expect/actual`。不支持的具体动作统一返回 `PlatformResult.Unsupported`。

## 状态与生命周期

当前应用仓库使用 `object`，平台启动先初始化真实依赖，再启动消费者；退出先提交待保存数据，再关闭应用协程、网络、数据库和日志。

采用单主界面设计，根 `MainViewModel` 由主界面的 ViewModelStore 持有，生命周期覆盖子页面。生产页面、私有 Composable 和子页面 ViewModel 通过 `MainViewModel.requireCurrent()` 访问已注册的根实例；根不持有子 ViewModel。页面 ViewModel 组织保存、确认和后续导航等事件流程，Compose 负责展示和 IME、焦点等界面操作。

`GkAppNavigation` 是生产路由注册，`GkNavigation` 管理导航条目、框架保存状态及 ViewModel 生命周期。每次入栈由 `AppNavEntry` 区分，即使路由相同也不共享条目状态。HomeRoute 的 HomeViewModel 管理五标签状态与业务，子路由持有各自需要的 ViewModel；纯展示和组件瞬时状态不单独创建 ViewModel。

仓库只读 Flow 在消费处收集；ViewModel 管理业务查询聚合、Paging、加载/失败及保存冲突基准。数据库查询首发前使用 `Loadable.Loading`，已加载空结果使用 `Ready(emptyList())`。搜索/筛选、编辑草稿及基准等自定义页面状态由所属 ViewModel 的 `MutableStateFlow` 持有，页面通过只读 `StateFlow` 和明确事件方法使用，不依赖自定义 Saver 恢复。状态随所属导航条目清理，StateFlow 本身不承担落盘或进程重启恢复。

滚动、焦点、动画等工具状态留在 Compose，使用官方 `rememberXxxState`；简单工具/组件状态确需恢复时允许 `rememberSaveable`，瞬时状态使用 `remember`。Navigation3 和官方组件内部的保存机制保留；主题通过 `rememberAppearance` 跟随组合生命周期。具体边界统一见 [开发规范](../AGENTS.md#ui-与生命周期)。

`A11yRuntime` 选择真实服务，各服务持有自己的规则引擎、事件缓存及延迟任务，资源随所属 Android 组件释放。页面退出不销毁应用仓库。

## 数据读写与兼容

| 数据 | 读取 | 写入与一致性 |
| --- | --- | --- |
| 设置与名单 | `SettingsRepository` 只读状态 | 明确事件更新/保存，`awaitPersistence` 确认落盘 |
| 订阅 | `SubscriptionRepository.snapshotFlow`、订阅 DAO | 仓库协调解析、文件原子写入与数据库补偿 |
| 规则配置 | DAO、共享 `ruleGroupState` | `RuleGroupConfigService` / `SubscriptionConfigStore` 事务校验与更新 |
| 运行记录 | 日志和访问记录 DAO | `RuntimeRecordRepository` 写入、批次提交与裁剪 |
| 快照 | 快照 DAO、`SnapshotStore` | `SnapshotCaptureRepository` 编排采集，Store 提交、替换与删除 |
| 备份与日志归档 | `BackupManager`、`LogArchive` | 共享文件流程，宿主处理选择、分享与保存目的地 |
| Service 与权限 | 平台运行状态 | 真实组件和权限请求宿主管理生命周期 |

业务副作用由明确事件触发，不通过派生展示 Flow 驱动。设置更新先发布内存状态并提交持久化请求；转换函数保持无副作用，恢复时可能应用于当前状态和回滚状态。备份提交持有订阅写锁，数据库回滚和文件补偿完成后释放；设置回滚保留恢复期间的新命令。

路由序列化名称、组件类名、AIDL、数据库结构、生产设置文件名/字段/默认值属于兼容契约。

应用通过 `AppStorageLayout` 使用真实文件、Repository/DAO 和共享算法。具体目录及缓存消费者见 [开发产物](local-development.md)。

## 依赖管理

- 外部 Gradle 坐标和版本统一放在 [版本目录](../gradle/libs.versions.toml)。`core` 以 `api` 导出应用消费的库，`app` 只声明打包必需项。
- `core` 依赖 `gkd-aidl` 的 Binder 产物。`gkd-hidden-api` 通过顶层 `remapApi` 提供编译期 stub 与 remap 索引，不作为运行时依赖；remap 插件转换 `core` 的字节码。
- remap 与 priv-ui 使用发布版本，以版本目录为准。依赖调整需核对直接使用及运行时注册，验证 Android `gkd`；涉及产物或字节码转换时检查最终打包链路。

## 字符串资源

共享文案位于 `core/src/main/res/values/strings.xml`，通过 Android 资源 ID 访问。资源键按用途命名，例如 `rule_enable_in_app`、`app_count`，不加 `gk_`；无障碍相关键使用 `a11y`。日志、诊断、协议字段、URL 和用户输入不属于固定 UI 文案。

```kotlin
import androidx.compose.ui.res.stringResource
import li.gkd.app.R

Text(stringResource(R.string.app_count, apps.size.toString()))
```

Compose 使用 `androidx.compose.ui.res.stringResource`，协程使用 `li.gkd.app.resources.getString`，同步低频边界使用 `Int.getSync()`。不在静态初始化或高频路径缓存解析文案。

参数仅使用 `%1$s`、`%2$s`，数字、日期、百分比及单位先由 Kotlin 按明确 Locale 和精度格式化。要展示百分号或占位符语法时将其作为参数传入。换行使用 `\n`，XML 的 `&`、`<` 用实体转义。`${i}` 等持久化通知模板变量是普通文本，不随语言切换改写用户模板。

领域和存储错误保留语义、参数与原因，由共享 UI 映射资源；订阅/规则使用 `SubscriptionException`，仓库不接收文案适配器。默认订阅名称只用于创建缺失记录，已有用户名称优先。

`validateStringResources` 只校验占位符。翻译使用 Android 语言限定目录；当前只有默认中文资源。
