# CoastGPT Native v1 Build Plan

> 战斗施工图，不是第二份 UI 清单。
>
> PWA 依据：elementera-coast/docs/native/UI_ACTION_TREE.md，文档提交 966c7e5fadb72d48500cbd477adfacee6aafde44；其扫描基线为 b6b8267e413c64569541730a69f98ba6c46400e4。
>
> Native 现状依据：coast-native-android main 912e3147a3b24a3295a5673199799a9f24715e2f，MiniiChat-derived shell PoC。
>
> 边界：v1 先造完整 App 身体，只接四类 P0 Chat 水管；P1/P2 先保留入口、层级、空状态或明确禁用态。

压缩依据不是抽样：原树覆盖 14 个主 owners、9 个 Daily 内部 owners、45 条 overlay routes、173 个 actions 与 50 个 API bases。施工切线保留原统计：actions P0/P1/P2 = 20/77/76；API P0/P1/P2/placeholder = 4/23/18/5。

## 1. 一句话产品定义

CoastGPT Native v1 是 Elementera Coast 的原生客户端身体：它不是 ChatGPT clone，也不是单页 ChatScreen；Chat 是功能中心，但必须被 Coast splash、Gate、MainShell、侧栏、房间、三套主题、功能入口、Overlay/Toast 和“小一 / Myri / 海岸”身份资产完整包围。

## 2. Native v1 最小完整页面树

    CoastGPT Native v1
    ├─ Boot / Coast splash
    │  ├─ 品牌图形或 Coast icon
    │  ├─ 小一 / 海岸短文案
    │  └─ loading / boot error
    ├─ Gate shell
    │  ├─ Elementera Coast 身份区
    │  ├─ session loading / signed-out / error
    │  └─ 登录动作占位（真实契约确认后接入）
    └─ MainShell
       ├─ Sidebar drawer
       │  ├─ Coast status strip
       │  ├─ ConversationList
       │  │  ├─ 当前会话
       │  │  ├─ 新建会话
       │  │  └─ 搜索 / loading / empty / error
       │  ├─ Rooms
       │  │  ├─ Radio
       │  │  └─ Lighthouse
       │  ├─ Feature entries
       │  │  ├─ Memory
       │  │  ├─ Daily
       │  │  ├─ Calendar
       │  │  └─ Letters
       │  └─ Identity / settings entries
       │     ├─ Theme: light / dark / gold
       │     ├─ Wolf Den
       │     └─ Serpent Desk
       ├─ TopBar
       │  ├─ drawer trigger
       │  ├─ current model label
       │  ├─ ModelQuickPicker trigger
       │  ├─ active window title
       │  └─ new chat / contextual actions
       ├─ WindowHost
       │  ├─ ChatWindow
       │  │  ├─ CoastTimeline
       │  │  ├─ message loading / empty / error
       │  │  ├─ Dogtalk row placeholder
       │  │  └─ CoastComposer
       │  └─ RoomWindow placeholder
       │     ├─ Radio identity state
       │     ├─ Lighthouse identity state
       │     └─ shared timeline / composer outline
       ├─ ModelQuickPicker sheet
       ├─ FeatureLandingHost
       │  ├─ Memory landing
       │  ├─ Daily landing
       │  ├─ Calendar landing
       │  ├─ Letters landing
       │  ├─ Wolf Den settings shell
       │  └─ Serpent Desk settings shell
       ├─ Overlay / Dialog / typed NavHost
       └─ Snackbar / Toast host

不可删减线：Boot → Gate → MainShell。ChatWindow 是 MainShell 的一个中心窗口，不是整个应用。

## 3. P0 / P1 / P2 切线

### P0：第一版必须做出“身体”

| P0 身体 | v1 最低完成定义 |
| --- | --- |
| Coast splash / Boot | 冷启动先看到 Coast 身份与 loading；失败时有 retry，不直接闪入 Chat |
| Gate shell | 有独立 Gate 页面和 session 状态；不把当前本地假密码当成真实登录契约 |
| MainShell | Modal drawer、TopBar、WindowHost、Overlay、Snackbar 形成稳定主壳 |
| Sidebar | Coast status、会话、Radio/Lighthouse、各 feature 与设置入口全部可见 |
| Theme | light / dark / gold 三套可切换，并覆盖系统栏、drawer、sheet、composer |
| ConversationList | 拉取、空态、新建、选择当前会话；rename/delete 延后 |
| ChatWindow | 历史 loading/empty/error/ready，消息时间线和生成态 |
| Stable Composer | composerReady 前明确 disabled；可发送时稳定贴合 IME；生成时同位按钮变 Stop |
| Model quick picker | TopBar 始终显示 current model；sheet 可从 profile model_box 选模型 |
| Dogtalk row | 可见、折叠、说明“稍后接入”；不伪造保存结果 |
| Rooms | Radio/Lighthouse 各自有入口、标题、说明和共享 RoomWindow placeholder |
| Feature landings | Memory/Daily/Calendar/Letters/Wolf/Desk 均可导航到有身份的空状态 |
| Overlay / feedback | typed NavHost、返回、Dialog、Snackbar；禁用按钮必须说明原因 |
| Identity assets | launcher/icon、Myri avatar、小一、海岸房间名、Coast status 和基础图形 |

P0 UI 不等于 173 个 actions 全接通。第一版只让 20 个 P0 action 对应的身体与主交互成立，其余保持诚实占位。

### P0 真接后端：只接四类

| API base | v1 方法 | Repository | ViewModel / state | UI 去向 |
| --- | --- | --- | --- | --- |
| /api/chat | POST，stream=true | ChatRepository | ChatViewModel；GenerationState | CoastTimeline 增量更新、Composer Send/Stop |
| /api/chat/conversations | GET、POST | ConversationRepository | ConversationListViewModel | Sidebar list、active id、empty/create fallback |
| /api/chat/history | GET、PUT | ConversationRepository 或 HistoryRepository | ChatViewModel；ConversationState | 当前 timeline、串行 save chain |
| /api/chat/profile | GET、PUT | ProfileRepository | AppViewModel / ModelViewModel | Myri avatar、current model、model_box、quick picker |

调用方向固定为：

    Compose UI
      └─ UiEvent
         └─ feature ViewModel
            └─ repository interface
               └─ CoastApiClient / SSE transport
                  └─ immutable UiState
                     └─ Compose render

P0 方法切线：

- conversations 的 PATCH/DELETE 属于 P1；同一个 API base 在 P0 只做 GET/POST。
- history 的 PUT 必须有串行化 save chain，避免流式完成、切 variant 或切会话时后写覆盖前写。
- profile 的 PUT 在 P0 只承担 current chat model 切换；完整模型箱管理属于 P1。
- 不为 Gate 猜造第五个 endpoint。真实登录是独立契约门，确认 cookie、token 或系统浏览器回跳后再实现。

### P1：第二阶段或第一版轻功能

- 完整模型箱、模型目录刷新、搜索和 model_box 管理。
- 会话 rename/delete；消息 copy/reaction/regenerate/variants；generation footprint。
- Radio/Lighthouse 真列表、发送、已读、撤回、retry 和 ask-API。
- Dogtalk 保存、读取、归档和清除。
- Daily hub 轻实现；Calendar month/day；Letters local-first。
- Wolf Den / Serpent Desk 设置壳、头像与视觉偏好、export/import/diagnostics。
- Desk slip 与清晰的 offline/stale 状态。

### P2：后续深功能

- Memory soil、pockets、entries、跨房间记忆与 vector status。
- Daily summary、moments/diary/album 编辑器、media 与 legacy migration。
- Calendar event/note editor。
- Worldbook、Workbench、Toolroom、run-control 与 sandbox。
- Mailbox visitor 的完整 Gate、同意、账户和消息流。

第一版漂亮占位：Radio、Lighthouse、Memory、Daily、Calendar、Letters、Wolf、Desk。第一版可见但禁用：图片、麦克风、空输入通话、完整模型管理、Daily 发布/编辑、Calendar 新建、Memory 落袋/编辑、Worldbook/Workbench。每个禁用动作都用 Snackbar 或页面说明解释，不能沉默。

## 4. Kotlin / Compose 文件树建议

当前 package 和单 app module 足够；不在 v1 过早拆多 Gradle module。下一轮目标结构：

    app/src/main/kotlin/com/elementeracoast/app/
    ├─ MainActivity.kt
    ├─ CoastApp.kt
    ├─ navigation/
    │  ├─ CoastDestination.kt
    │  └─ CoastNavHost.kt
    ├─ core/
    │  ├─ network/
    │  │  ├─ CoastApiClient.kt
    │  │  ├─ CoastHttpClient.kt
    │  │  ├─ CoastSseParser.kt
    │  │  └─ NetworkResult.kt
    │  ├─ session/
    │  │  ├─ SessionContract.kt
    │  │  └─ SessionStore.kt
    │  └─ design/
    │     ├─ CoastIcons.kt
    │     └─ CoastDimensions.kt
    ├─ data/
    │  ├─ model/
    │  │  ├─ Profile.kt
    │  │  ├─ Conversation.kt
    │  │  ├─ Message.kt
    │  │  └─ StreamEvent.kt
    │  ├─ repository/
    │  │  ├─ ProfileRepository.kt
    │  │  ├─ ConversationRepository.kt
    │  │  └─ ChatRepository.kt
    │  └─ preferences/
    │     └─ CoastPreferences.kt
    ├─ feature/
    │  ├─ boot/
    │  │  └─ CoastBootScreen.kt
    │  ├─ gate/
    │  │  ├─ GateScreen.kt
    │  │  └─ GateViewModel.kt
    │  ├─ shell/
    │  │  ├─ MainShell.kt
    │  │  ├─ CoastDrawer.kt
    │  │  ├─ CoastTopBar.kt
    │  │  ├─ CoastStatusStrip.kt
    │  │  ├─ ShellViewModel.kt
    │  │  └─ WindowScope.kt
    │  ├─ conversations/
    │  │  ├─ ConversationList.kt
    │  │  └─ ConversationListViewModel.kt
    │  ├─ chat/
    │  │  ├─ ChatWindow.kt
    │  │  ├─ ChatViewModel.kt
    │  │  ├─ ChatUiState.kt
    │  │  └─ GenerationState.kt
    │  ├─ model/
    │  │  ├─ ModelPickerContent.kt
    │  │  └─ ModelViewModel.kt
    │  ├─ dogtalk/
    │  │  └─ DogtalkPlaceholder.kt
    │  ├─ rooms/
    │  │  ├─ RoomWindow.kt
    │  │  └─ RoomDestination.kt
    │  ├─ landing/
    │  │  ├─ FeatureCatalog.kt
    │  │  └─ FeatureLandingHost.kt
    │  └─ settings/
    │     └─ SettingsHub.kt
    └─ ui/
       ├─ common/
       │  ├─ CoastComposer.kt
       │  ├─ CoastTimeline.kt
       │  ├─ CoastFeatureLanding.kt
       │  ├─ CoastFeatureScaffold.kt
       │  ├─ CoastStatePane.kt
       │  └─ CoastListRow.kt
       └─ theme/
          ├─ CoastTheme.kt
          ├─ CoastColorSchemes.kt
          ├─ CoastTypography.kt
          └─ CoastTokens.kt

    app/src/test/kotlin/com/elementeracoast/app/
    ├─ core/network/CoastSseParserTest.kt
    ├─ feature/chat/ChatViewModelTest.kt
    ├─ feature/conversations/ConversationListViewModelTest.kt
    └─ feature/shell/ShellNavigationTest.kt

    app/src/androidTest/kotlin/com/elementeracoast/app/
    ├─ MainShellSmokeTest.kt
    ├─ ComposerImeTest.kt
    └─ ThemeAndNavigationTest.kt

结构原则：状态靠近 feature owner；不要建立新的万能 CoastShellState，也不要让 MainActivity 认识 Chat 细节。

## 5. 数据同步骨架

### 总数据流

    Gate
    └─ SessionContract
       ├─ cookie contract，或
       ├─ token contract，或
       └─ system-browser login + app-link return
          └─ MainShell

    Profile
    └─ GET /api/chat/profile
       ├─ current_chat_model
       ├─ current_image_model
       ├─ model_box
       └─ assistant_avatar_dataurl

    Conversations
    └─ GET /api/chat/conversations
       ├─ Sidebar list
       ├─ active conversation
       └─ empty → POST create 或显示明确 create action

    History
    └─ GET /api/chat/history?conversation_id=...
       ├─ messages
       ├─ variants
       └─ serial save chain → PUT /api/chat/history

    Chat
    └─ POST /api/chat，stream=true
       ├─ SSE meta
       ├─ SSE delta
       ├─ SSE usage
       ├─ SSE done
       ├─ SSE error
       └─ coroutine cancellation = stop generation

### 冷启动顺序

1. Boot 展示 Coast splash，AppUiState 为 Loading。
2. 解析 SessionContract；未认证进入 Gate，失败显示可重试 error。
3. 认证后并行读取 profile 与 conversations。
4. 用 DataStore 的 lastConversationId 尝试恢复；若已不存在，选择服务端当前项或第一项。
5. 有 active id 后读取 history；空列表时提供新建，不伪造会话。
6. profile、conversation 和 history 达到可用或明确空态后，composerReady 才为 true。
7. 任一请求失败仍保留 MainShell 身体，显示局部 CoastStatePane；不白屏。

### 当前 conversation id

- 运行时唯一真值在 ConversationListViewModel 的 StateFlow。
- 写入 DataStore 的只是 lastConversationId，便于冷启动恢复；每次启动都必须对服务端 conversations 重新校验。
- active id 作为 ChatViewModel 的输入，切换时先取消当前 generation，再加载新 history。
- 不把完整消息或私密正文写进 DataStore。

### composerReady

composerReady 是派生条件，不是可随意翻转的独立 flag：

- session 已可用；
- active conversation 已解析；
- profile 和 history 已完成为 ready 或合法 empty；
- 当前没有会话切换、初始化或不可恢复错误。

未 ready：输入、图片、麦克风、发送全部 disabled，并显示 loading/error 原因。Generating 时文本输入策略由产品决定，但主按钮必须同位变成 Stop。

### generation / stop / save

- ChatViewModel 只拥有一个 generation Job。
- POST 响应由 CoastSseParser 解析 meta/delta/usage/done/error；delta 只更新内存中的当前 assistant variant。
- Stop 取消 coroutine，同时关闭 HTTP response body；不得只改 UI flag。
- 切会话、退出 Gate 或 ViewModel clear 都要取消 generation。
- done 后进入串行 history PUT；失败保留本地可见正文并显示“未同步”，不假装已保存。
- parser 必测分片、CRLF、多 data 行、空行、错误事件、done、用户取消。

### current model switching

- current model 与 model_box 的服务端真值来自 profile。
- quick sheet 只显示 profile 已允许的模型；目录刷新和增删模型留到 P1。
- 选择时调用 profile PUT；保存中禁用重复选择，失败回滚并 Snackbar。
- 每次生成把选中模型快照写进 request/state，切换模型不能篡改已经在流式中的请求。

### 持久化边界

| 存储 | v1 内容 |
| --- | --- |
| DataStore | theme、lastConversationId、非敏感 UI 偏好、已确认的 Gate 展示状态 |
| Keystore-backed secure store | 仅当确认是 bearer token 时保存 token；绝不放普通 DataStore 或源码 |
| ViewModel memory | messages、variants、composer draft、generation、sheet/menu、Room placeholder、局部 loading/error |
| Server | profile、conversation list、history 和 chat 真数据 |

Native v1 不需要 Room 数据库。四类 P0 API 已以服务端为真值；先用 ViewModel 内存和 DataStore 偏好即可。只有明确要求离线历史、可恢复草稿或复杂实体缓存时，再为对应数据设计 Room schema，不能为了“架构完整”提前引入。

## 6. 当前 PoC 处理策略

结论：保留工程承重和少量交互语义，重写 MainShell；不能从 ChatScreen 向外堆功能。

| 当前文件 / 结构 | 下一轮决策 | 原因 |
| --- | --- | --- |
| settings.gradle.kts、根 build.gradle.kts、gradle.properties | 保留 | 单 app module、JDK 17、Compose 基线足够 |
| app/build.gradle.kts | 保留并增量加入 Navigation、DataStore、HTTP、测试依赖 | 不需要重建工程；不提前多模块化 |
| AndroidManifest.xml | 保留应用标识、portrait、adjustResize、cleartext=false；A5 才加 INTERNET | 当前安全默认正确，网络权限随真接后端进入 |
| .gitignore、CI binary/signing/provider guards | 保留并继续加强 | 已防 APK、AAB、keystore、通用 provider 残留 |
| THIRD_PARTY_NOTICES.md、MiniiChat MIT License | 保留 | InputBar、ModelPicker、generation owner 仍有派生来源 |
| MainActivity.kt | 保留路径、重写内容 | 只负责 edge-to-edge 与 CoastApp；删除 authenticated → ChatScreen 直跳 |
| LoginScreen.kt | 替换并改名为 GateScreen.kt | 可参考视觉层次，但本地假密码不是主 Gate 契约 |
| ChatScreen.kt | 拆分后删除旧文件 | 其消息比例可参考；不能继续同时拥有 header、timeline、composer、sheet 和 App 根 |
| InputBar.kt | 替换为 CoastComposer.kt 后删除旧文件 | 保留 multiline 与 send/stop 同位语义，加入 ready、subject、Dogtalk、enabled actions |
| ModelPicker.kt | 迁为 ModelPickerContent.kt 后删除旧文件 | 保留搜索/选中/sheet 交互；同一内容以后也供 full page |
| Models.kt | 拆为 API model 与各 feature UiState 后删除 | CoastShellState 把 auth、messages、models、sheet 混成万能状态 |
| CoastShellViewModel.kt | 拆分后删除 | Gate、Shell、Conversation、Chat、Model 必须分别有 owner；假流式不得进入产品路径 |
| CoastGatewayClient.kt | 保留边界思想，替换接口 | 当前 login/listModels/stopGeneration 不是四类 P0 repository contract |
| CoastTheme.kt | 重写为 light/dark/gold 三方案 | 现有单一深海金只可作色彩种子，不是最终设计系统 |

下一轮应删除的行为，而不是本轮删除文件：

- enterLocalShell 的“非空密码即认证”。
- sendFakeMessage、固定 chunks、假模型、假欢迎消息。
- MainActivity 中 Gate/Chat 的二分直渲染。
- 单一 CoastShellState 和单一 CoastShellViewModel。
- “普通聊天应用先成立，再补海岸”的产品方向。

MiniiChat-derived 内容只能作为经过 MIT 标注的局部交互参考：消息流比例、multiline composer、send/stop 同位、picker 交互、单 generation Job。它不能继续决定导航、产品信息架构、provider 模型或视觉身份。不得引入其通用 BYOK/provider/assistant/media 系统，也不得复制 GPL/AGPL 项目实现。

## 7. 组件合并策略

| Compose 组件 | 合并范围 | 关键 contract |
| --- | --- | --- |
| CoastComposer | Chat 与 Room 共用 | subject slot、Dogtalk slot、enabled actions、composerReady、send/stop 同位、IME-safe |
| CoastTimeline | Chat、Radio、Lighthouse 共用 | typed timeline items；Chat P0 真数据，Rooms v1 placeholder |
| ModelPickerContent | quick sheet 与 P1 full page 共用 | query、selected id、loading/empty/error、onSelect；host 决定 sheet/screen |
| SettingsHub | Wolf Den 与 Serpent Desk 共用骨架 | section list 与 state pane 共用；标题、文案、图标和 identity 不合并 |
| CoastFeatureLanding | Memory、Daily、Calendar、Letters 等 P0 landing | feature art、title、copy、status、primary/secondary action slot |
| CoastFeatureScaffold | screen、overlay、sheet 的统一内容壳 | back、title、subtitle、header action、scroll body、insets |
| CoastStatePane | loading、empty、error、offline | feature-specific copy、retry/action slot；不允许全局无差别错误页 |
| CoastListRow | conversation、room、feature row | leading identity、title/subtitle、badge、selected/disabled、trailing action |

额外骨架组件：

- MainShell 只组合 drawer、top bar、window、overlay、snackbar，不直接请求 API。
- FeatureLandingHost 根据 typed destination 渲染，不用字符串 selector 或 Web Event Spine。
- 所有 feature 通过 UiEvent → ViewModel → UiState 单向流动；Compose 本地 state 只留短暂视觉状态。

## 8. 视觉 / 资产迁移策略

### 图标与头像

- 从 PWA 已审核资产中选 launcher/icon 基准，生成 Android mipmap-mdpi 到 xxxhdpi 与 adaptive icon foreground/background；不把整套网页 public 目录原样复制。
- Myri avatar 以 public/media/myri-default-avatar.jpg 为来源，经尺寸与许可核对后进入 drawable-nodpi/myri_default_avatar.jpg；服务端 profile 的 assistant_avatar_dataurl 可在运行时覆盖。
- SVG 仅在 Android VectorDrawable 支持的 path 范围内转换；复杂 SVG 保留为审图参考，不能盲转丢形。
- 保留“小一”、海岸日期、Radio、Lighthouse、Wolf Den、Serpent Desk 的名称和图形层级。身份文案进入 strings.xml，不散落在 Composable。

### 三套主题

- 从 PWA tokens.css 翻译 light/dark/gold 的 background、surface、panel、text、muted、accent、outline、danger；映射为三个 Coast ColorScheme。
- spacing、radius、elevation、bubble width、timeline rhythm 和 typography 分开建 Coast tokens，不能只换 Material primary。
- Theme 走 DataStore；切换后系统栏、drawer、sheet、dialog、Snackbar、composer 同步更新。
- 网页 CSS 是视觉规则来源，不复制 CSS，也不模拟 DOM breakpoint。

### Splash 与移动端

- 使用 Android system splash 承接冷启动，再进入短暂 Compose CoastBootScreen。
- v1 可用静态 Coast 图形加轻微 fade/scale；不需要重动画依赖，但绝不直接进入 Chat。
- 使用 edge-to-edge、WindowInsets.safeDrawing、WindowInsets.ime 和 adjustResize；不用 visualViewport 或 CSS safe-area 方案。
- PWA Service Worker、manifest cache、download anchor、file input、clipboard、AbortController 分别换成 repository/cache、Android manifest、SAF/share、Photo Picker、ClipboardManager、coroutine cancellation。

## 9. 下一轮真正写代码的施工批次

每批独立可提交；从 A1 开始，不从 ChatScreen 扩张。

| 批次 | 修改范围与主要文件 | 验收标准 | 后端 | 可打 debug APK |
| --- | --- | --- | --- | --- |
| A1 — Reframe app root | MainActivity、CoastApp、CoastDestination/NavHost、MainShell、WindowScope | 启动路径为 Boot → Gate → MainShell；MainActivity 不再直接渲染 ChatScreen；壳可用假 UiState 预览 | 否 | 是 |
| A2 — Identity, Gate, themes | CoastBootScreen、GateScreen、CoastTheme 三方案、tokens、基础 icons/strings、insets | 有 Coast splash/Gate；light/dark/gold 可切；系统栏与 IME 不破版；不实现假认证 | 否 | 是 |
| A3 — Full Coast shell | CoastDrawer、TopBar、StatusStrip、FeatureCatalog/LandingHost、RoomWindow、Overlay/Snackbar | 侧栏、Rooms、Memory/Daily/Calendar/Letters/Wolf/Desk 全可到达；占位有说明 | 否 | 是 |
| A4 — Chat body components | ConversationList、ChatWindow、CoastTimeline、CoastComposer、DogtalkPlaceholder、ModelPickerContent | composer ready/disabled/generating/stop 视觉齐；模型 sheet 和会话列表在 MainShell 内；仅 fixture | 否 | 是 |
| A5 — P0 read/write repositories | HTTP/session seam、ProfileRepository、ConversationRepository、history、DataStore、ViewModels | Gate 契约确认后，profile/conversations/history 可加载；空/错误不白屏；create 与 model switch 可用 | 是：profile、conversations、history | 是 |
| A6 — Chat SSE and stop | ChatRepository、CoastSseParser、ChatViewModel、history save chain、parser tests | meta/delta/usage/done/error 正确；Stop 取消网络；切会话取消；成功后串行保存 | 是：chat + history | 是 |
| A7 — State hardening | ViewModel tests、MainShell smoke、retry/offline/disabled copy、navigation restoration | 冷启动、空会话、失败、旋转/重组、生成取消均有稳定状态；P1/P2 未偷接 | 不新增 | 是 |
| A8 — Coast polish and security gate | launcher/adaptive icons、Myri、accessibility、theme/insets/IME tests、CI/notice | 不像普通 LLM app；talkback labels/触控尺寸合格；无 secret/keystore/provider residue；debug build 通过 | 不新增 | 是，作为 v1 验收包 |

A5 的开工门：先拿到主 Gate 的真实认证契约。未确认前，A1–A4 可完整构建“身体”，但不得把 PoC 本地密码包装成产品登录。

## 10. Native v1 第一版 APK 验收清单

- [ ] 打开后不是单页 ChatScreen。
- [ ] 有 Coast splash，冷启动不闪白、不直跳 Chat。
- [ ] 有独立 Gate shell；认证 loading/error 有说明。
- [ ] 有 MainShell，壳在局部 API 失败时仍存在。
- [ ] 有 Sidebar drawer 与 scrim/back 行为。
- [ ] 有 TopBar、current model label 和 active window title。
- [ ] 有 Coast status strip。
- [ ] 有 ConversationList，支持 loading/empty/open/create。
- [ ] 有 ChatWindow 与 CoastTimeline。
- [ ] 有稳定 CoastComposer；输入法不挡 composer。
- [ ] 冷启动 composer 有 disabled/loading 状态，不能提前误发。
- [ ] 生成时 Send 原位变 Stop，Stop 真正取消 coroutine 和 HTTP。
- [ ] 有 ModelQuickPicker sheet，保存失败可回滚。
- [ ] 有 Dogtalk 可见占位且说明未接入。
- [ ] Radio 和 Lighthouse 都有入口及不同身份文案。
- [ ] RoomWindow placeholder 可进入、可返回。
- [ ] Memory、Daily、Calendar、Letters、Wolf Den、Serpent Desk 全部有入口。
- [ ] Feature landing 不是空白页，含图形/说明/状态。
- [ ] 有 Overlay/NavHost、Dialog 与 Snackbar host。
- [ ] light/dark/gold 三主题覆盖主壳、sheet、dialog、composer 与系统栏。
- [ ] P0 API 成功时可加载 profile/会话/历史并聊天。
- [ ] P0 API 失败时显示局部错误与 retry，不白屏、不假成功。
- [ ] history 保存失败显示未同步，不吞正文。
- [ ] 禁用按钮有可见说明，不沉默。
- [ ] 旋转/重组或切前后台不重复发请求、不重复生成。
- [ ] 无 secrets、API key、keystore、APK/AAB 入仓。
- [ ] 不引入通用 provider/BYOK 系统。
- [ ] 保留 MiniiChat MIT attribution；不复制 GPL/AGPL 代码。

## 11. 风险与未定问题

| # | 风险 / 未定问题 | 开工决策 |
| ---: | --- | --- |
| 1 | 主 Gate 登录契约未知；PWA 主 SPA 没有 Gate DOM/controller，mailbox Gate 不是替代品 | A5 前确认 cookie、bearer 或浏览器回跳；不猜 endpoint、不保存明文密码 |
| 2 | 现有 PoC 的 MainActivity → ChatScreen 是产品根 | A1 从 MainShell 重写；ChatScreen 只拆取局部语义 |
| 3 | P0 真接后端容易顺手拖进 models/rooms/memory | repository allowlist 只保留四类 API base，PR/测试扫描阻止越界 |
| 4 | Android SSE 可能遇到分片、CRLF、半包与取消后残留回调 | 独立 parser、单 Job owner、关闭 response body、覆盖取消测试 |
| 5 | history schema、variants 与 save chain 的精确 JSON 仍需按后端 contract 固化 | A5 前抓取非敏感 schema/fixtures；不凭 UI 树猜字段 |
| 6 | Theme 与 edge-to-edge/insets 太晚处理会迫使整壳返工 | A2 先验收三主题、状态栏、导航栏、IME 与小屏 |
| 7 | PWA 资产直接搬运可能丢失 adaptive icon 形状、清晰度或许可线索 | 建资产清单、逐项来源/许可/尺寸核对，再生成 Android resources |
| 8 | DataStore 或日志误存聊天正文、token | DataStore 只存非敏感偏好；token 仅进 Keystore-backed store；release 日志脱敏 |
| 9 | v1 不上 Room 时离线历史能力有限 | 明确 v1 以服务端为真值；先做 stale/offline UI，需求成立后再设计 schema |
| 10 | 占位入口过多可能看似“能点但不能用” | P0 必须统一空状态和阶段说明；优先保证 Chat 真用，P1 按房间与日常顺序接入 |

## 执行结论

最快但不缩水的路线是：保留当前工程、许可证、安全护栏和三个有价值的交互语义；先用 A1–A4 重建 Coast 身体，再用 A5–A6 只接 profile、conversations、history、chat 四类水管。任何从现有 ChatScreen 外围继续叠侧栏和页面的方案都应拒绝，因为它会再次得到“有海岸配色的普通 LLM App”，而不是 Elementera Coast 的原生客户端。
