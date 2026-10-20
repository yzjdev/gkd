# Repository Instructions

设计与依赖见 [应用架构](docs/architecture.md)，固定目录见 [本地产物](docs/local-development.md)。本文件只维护开发约束。

## 模块与平台

- 仅两个应用模块：`app`（Application 清单、原生 res、渠道、签名、打包、R8 输入）与 `core`（全部业务、UI、平台实现和 Room）；依赖方向为 `app` → `core` → `gkd-aidl`，禁止反向依赖。`gkd-hidden-api` 只作编译期 stub 与 remap 输入，不是运行时依赖。
- `core` 是纯 Android 模块，不再区分 commonMain/androidMain/jvmMain，也不使用 `expect/actual` 或 typealias 转发。业务按职责归入 `li.gkd.app.*`；已注册 Application、Activity、Service、磁贴、特权入口及紧密关联实现保留原身份。`com.google.android.accessibility.selecttospeak.SelectToSpeakService` 的包名、路径和注册不变。包名整理不得改变组件、路由序列化名称/参数、AIDL 或持久化兼容标识。
- 公共根包及 `core` 的 namespace 为 `li.gkd.app`，UI/导航/ViewModel 位于 `li.gkd.app.ui.*`；源码目录与包名一致。`app` 的 namespace 为 `li.gkd.android`，Manifest 使用完整组件名，不写 Kotlin 代码。
- 领域逻辑不得依赖宿主；可复用工具只持有 Application Context，Activity/窗口引用限于生命周期绑定的 UI 会话。不支持的平台动作统一返回 `PlatformResult<T>`，无数据成功为 `Unit`；不得另造 Unsupported 类型或用 Boolean/null 暗示不支持。契约明确成功含义，保留异常/取消传播，不替代业务结果、用户取消或权限状态。
- 应用仓库由应用生命周期唯一持有，禁止重复创建、可替换全局实例和无意义转发。无独立会话需求时优先使用 object；需要独立生命周期或明确依赖时允许普通类及构造注入。平台依赖先于消费者初始化，所属生命周期结束时清理协程和资源，不为测试增加生产重置接口。

## UI 与生命周期

- 生产页面、组件、业务和事件链位于 `core`。新增入口须覆盖下游弹窗、面板与返回路径，不用整页 Toast/占位掩盖缺失；交付需业务、读写、生命周期和验证证据。
- 共用 `GkAppNavigation` 注册及 `GkNavigation` 保存状态/条目生命周期；禁止字符串分支注册生产路由、复制平台路由或另写保存/导航规则。普通/编辑页面过渡统一声明，公共页面只注册一次；HTTP 标签等状态读取生产来源。
- MainViewModel 属于主界面的 ViewModelStore，页面 ViewModel 属于具体导航条目；有需要保留的页面状态或事件流程时按需创建，不为纯展示或组件瞬时状态创建。根不持有其他 ViewModel 或子 ViewModelStore；子路由用条目标准 API 获取，多次入栈实例独立、出栈清理，不预建、缓存或跨路由借用。导航不重建仓库，宿主退出清理 ViewModel/协程。
- 路由及私有 Composable 可获取页面 ViewModel、处理平台 UI；主题、通用布局、可复用组件和工具仅接收状态/事件，不通过默认参数、工厂、CompositionLocal 或全局入口隐藏获取。Service、悬浮窗、后台任务使用自身作用域和应用仓库，不借用或补建页面 ViewModel。
- 采用单主界面设计，同一应用运行实例只存在一个根 MainViewModel，其生命周期覆盖所有子页面 ViewModel。生产页面、其私有 Composable 及所属子页面 ViewModel 可通过 `MainViewModel.requireCurrent()` 获取已注册的根实例，无需逐层传递。宿主完成初始化和必要绑定后注册，根实例清理时注销；静态入口只提供访问，不创建实例或延长其生命周期。主题、通用布局、可复用组件、工具、Repository、Service、悬浮窗和后台任务不得通过该入口获取，也不引入 LocalMainViewModel。
- 页面 ViewModel 可组织校验、保存、确认弹窗及后续导航等事件流程，按需使用 MainViewModel 的页面能力；Compose 负责展示、收集状态、转发事件及 IME、焦点等组合/宿主相关操作。不为缩短调用增加纯转发方法，不把页面流程转移到根 ViewModel。平台 UI 操作显式接收所属 UiHost 或事件回调，宿主引用绑定所属生命周期，不由页面 ViewModel 长期持有。
- 自定义页面业务状态，包括编辑草稿及基准、搜索/筛选条件、分页、保存冲突和页面切换后需要保留的状态，优先由所属页面 ViewModel 使用 `MutableStateFlow<T>` 管理，对外暴露只读 `StateFlow<T>` 和明确修改方法，默认不使用 `rememberSaveable` 或自定义 Saver。页面状态随所属导航条目清理，不为保留它而提升到 MainViewModel；StateFlow 本身不提供落盘或进程重启恢复。
- 滚动、焦点、动画、拖拽及简单组件展开等纯 UI 工具状态留在 Compose，允许使用官方 `rememberXxxState`；确需恢复的简单工具/组件状态可使用 `rememberSaveable`，无需恢复的瞬时状态使用 `remember`。不为规避 rememberSaveable 将这些工具状态搬入 ViewModel，也不以工具状态名义承载页面业务对象。`rememberXxxState` 不访问 ViewModel、数据库、Store、Service 或导航。组合工作使用 rememberCoroutineScope，稳定且绑定同一组合的 scope 不作冗余 remember key。主题状态、时序及深色优先级由 `core` 统一，宿主只给系统主题/色板。
- `XxxUiState/Actions` 仅用于确有需要的复用、预览或复杂契约；UiState 是不可变快照，不含 Flow、Paging 或高频状态；多个构造路径重复映射时才提取私有构建函数。业务状态不通过 CompositionLocal 传递。
- 应用/仓库只读 Flow 在实际消费处用 `collectAsStateWithLifecycle` 收集，Paging 用专用 API，高频状态放最小子树；不为转发而复制进 UiState/ViewModel；生产路由页面可获取所属 ViewModel，可复用组件接收不可变值和明确事件。ViewModel 可管理业务查询聚合、Paging 缓存、加载/失败和保存冲突基准，不因 map/combine 一律迁出。
- ViewModel 状态仅在所属 VM 内修改，对外只暴露只读状态及明确事件方法；可变字段保持私有，只读 StateFlow 使用 Explicit Backing Fields 持有 MutableStateFlow，禁止 `_xxxFlow` 双属性及 `.asStateFlow()`。Composable 条件输出可用条件区块或清晰的提前返回，优先沿用邻近风格，不为形式增加嵌套或无关重构。
- 共享页面用 `GkScaffold` 接收原生 Insets，不重复加系统留白。滚动内容末尾用 `GkPageBottomSpace()` / LazyColumn 的 `gkPageBottomSpace()`，高度由 GkPageBottomSpaceDefaults 统一；已有末尾 item 可内嵌但不重复添加，不替代系统栏或 IME Insets。
- 动画不阻塞交互：禁止因过渡、图标变形、普通开关短暂保存或假设快速点击而禁用、节流、延时解锁或吞点击。禁用仅对应数据、输入、权限等业务前提，写入一致性交业务层处理。退场重复内容可隐藏于无障碍导航，但不改变颜色/增加等待；检查覆盖过渡中点击和切换。

## 最小实现与防御边界

- 按明确需求和已确认的问题实现最小改动。不得为假设中的异常、竞态或未来扩展，新增状态、缓存、刷新机制、抽象层或生命周期联动。
- 添加校验、重试、兜底或异常捕获前，先核对依赖 API 和现有调用链的契约；已有保证不得重复实现。不确定契约时先查源码，不用防御代码代替确认。
- 操作只检查自身必需的前提，不因单项操作刷新全部能力，不在 UI、ViewModel、服务层重复执行同一检查。UI 提示与执行前校验确有不同职责时可分别保留。
- 不把未知、加载中或查询失败等同于权限拒绝；不得据此扩大为停服、断连、切换模式或修改用户设置，除非需求明确要求。
- 异常在能实际处理它的边界捕获；禁止仅为返回默认值、重复记录日志或捕获后原样抛出而增加包装。
- 抽象应消除已有重复或隔离真实差异。简单逻辑优先直接调用，不引入仅转发、预留扩展或另建同义状态的封装。
- 保留外部输入校验、权限边界、事务一致性和资源释放等必要保护；新增保护应能说明具体失败场景，以及现有机制为何不足。

## 状态、存储与副作用

- Room 查询保持冷 Flow，在所属 ViewModel 按一致性边界聚合后转为 `StateFlow<Loadable<XxxUiState>>`；Loading 表示首发未完整，Ready(emptyList()) 表示已加载为空，禁止空集合初值、计数器或 attachLoad 推断加载完成。
- 数据库/文件/网络写入和 Service 启停由明确用户、系统事件或领域方法触发，在 Repository/Store 完成一致性处理；禁止用派生 map/combine/stateIn 的 collect/onEach/watch 或 Composable 状态监听驱动。允许单一权威状态同步到幂等外部投影，但回调不再读取其他状态拼装写入。
- 一致读取字段由事实源提供同一不可变快照；debounce、conflate、collectLatest 等调度机制不保证一致性；锁或事务必须覆盖全部相关读写，禁止仅锁消费端便宣称多个独立状态源原子一致。Repository 不读取资源/弹 Toast，错误保留原因交 UI 显示。生产设置文件名、字段、默认值是契约，迁移验证旧数据加载、失败重试、并发写入和恢复回滚。
- 复用 AppStorageLayout 与真实 Repository/DAO；文件事务、归档、Cookie、网络配置和维护放共享层，宿主仅适配根目录、编解码、文件选择及系统动作。
- 宿主只注入 files/cache/privateFiles 根目录，布局遵循 [本地产物](docs/local-development.md)。图片缓存、网页用户数据、归档、临时文件须实际接入，不能仅创建目录。
- 自定义产物仅放根 `.local/`，工具标准目录不变。不得随缓存清理日常数据、备份或未处理恢复记录；不增旧目录回退/双写。

## 资源与 Kotlin 规范

- 禁止新增 XML，例外仅 Android 原生字符串及语言限定资源、app_icon/service 等平台配置。可用 Kotlin 表达的 UI/图标用 `.kt`；页面图标必须 ImageVector，drawable XML 仅限 Manifest 等平台配置及其依赖，即使同图标双用，页面仍用 ImageVector。不确定例外时先问用户。
- 文案统一放 `core/src/main/res/values/strings.xml`，用 `R.string.xxx` 引用；不建自定义 UiString 表或资源副本。只有 Manifest/平台配置/变体标签保留原生 ID，Kotlin 可按需读取平台标签。
- 资源 ID 不限于 UI 用途，允许枚举、业务状态、异常、Repository 等对象持有和传递，供 UI、HTTP、后台任务和导出等场景使用；在实际消费或输出边界按需解析。资源 ID 与已解析的文案字符串分开处理：不在枚举、单例或静态初始化中解析、缓存文案字符串；语义/参数与持久化数据分离，语言切换不改写用户模板。
- 参数只用 `%1$s`、`%2$s` 等位置文本占位符；禁止数值/精度/补零格式指令，Kotlin 按明确 Locale/精度先格式化。Compose 用 `androidx.compose.ui.res.stringResource`，协程用 `li.gkd.app.resources.getString`；`Int.getSync()` 仅限低频同步边界，不用于静态初始化、高频或 Compose 路径。
- Kotlin 可见性按实际访问需求收窄，优先使用 private；`core` 禁止使用 internal 关键字，需要跨文件或类访问的声明使用默认 public，其他模块允许 internal。仅为公开属性收窄可见性/可变性的 `_xxx` 字段改用 Explicit Backing Fields；普通私有缓存、生成命名及 Lambda `_` 不受限。避免循环静态初始化。
- 工具按职责组织并优先沿用邻近代码风格；无状态工具可用顶级函数，有共享状态或明确命名空间需求时使用 object，不为形式新增包装。XxxExt.kt 只放扩展，普通工具放职责明确的文件；不借规范调整进行无关重构。
- 跨文件/页面复用 UI 统一 `Gk` + PascalCase，名称描述用途，禁用 Perf/Custom 泛化前缀。单组件文件同名，组件族及配套声明可同文件，文件以 Gk 开头并描述组件族；配置用 GkAbcDefaults/Colors，图标用 GkIcon/GkIcons。页面、私有 Composable、Preview、工具、Modifier 扩展、独立状态及其 Render() 不强制此前缀。

## 验证与 Git

- 按改动做最小必要验证：默认执行 `sh gradlew assembleDebug`，仅在需要定位模块问题时才指定 `:core` / `:app` 任务。渠道仅保留 gkd。
- UI 默认使用对应编译及人工/adb 检查，不要求自动 UI 套件；仅为已发生、难以稳定人工复查的重要回归增加小范围自动测试，禁止仅复述静态布局或实现细节。业务状态机、持久化、事务恢复、协议/兼容测试保留，不按 ui 包名删除。
- 新测试说明输入、预期及回归，优先边界/异常/兼容行为；存储测试验证实际读写恢复，不仅路径。测试遵循生产生命周期。禁止为测试拆散生产逻辑、扩大可见性、增加专用 API、ViewModel、嵌套 Store 或生产重置/替换接口。
- 不测试枚举/常量集合、连续编号、同注册表推导关系或类型系统保证的静态声明；外部协议/持久化/跨版本标识例外，名称或注释说明保护的契约。
- Android 权限、IME、系统栏、预测返回、无障碍/Service 按需 adb 验证。真机/模拟器交互前记录自动化开关与运行模式，关闭 enableAutomator 并退出自动化模式，核对后再检查；finally 在成功/失败/中断后仅恢复本次字段并核对，不用旧整份配置覆盖。
- 用户仅要求提交/推送时，只做轻量核对及 Git 操作；未经明确要求，不扩展为深度审查、临时 worktree 验证、全量构建/测试、Release Gate 或发布检查。

## 依赖与 Android 专项

- Android framework Java/AIDL 源码定位、跨版本签名/可用性、缺失分析及 Java hidden-API 生成必须使用 [android-api-diff skill](.agents/skills/android-api-diff/SKILL.md) 与其 CLI，保留默认 JSON，不自行模拟版本检查。安装/更新 skill 在根执行 `android-api-diff skill install`。
- 修改隐藏接口/父类实现、系统回调、remap 或相关模块依赖/R8 配置，以及排查相关混淆 Release 故障时，必须使用 [android-hidden-api-r8 skill](.agents/skills/android-hidden-api-r8/SKILL.md)。检查最终应用模块 `app` 的实际 R8 输入及优化后的运行时契约，不以源码 override、编译成功或 Debug 正常代替；`core` 可见的 `gkd-hidden-api` stub 不代表最终 R8 可见。默认仅验证受影响的 gkd Release 路径，未经用户要求或证据支持不扩大审计范围。
- 特权进程 UserService 的可能失败 Binder 方法在最外层末端 `catch (e: Throwable)` 捕获可恢复错误，通过 Binder 可传输异常/结果保留原类型、消息、堆栈；不只在主进程捕获，不让 NoSuchMethodError 等 LinkageError 逃逸崩溃。VirtualMachineError/ThreadDeath 等终止错误可原样抛出，不伪装普通失败。
