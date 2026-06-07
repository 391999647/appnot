# NTnotes 项目说明

## 技术栈

- **语言**: Kotlin 2.0.21 (Multiplatform)
- **UI 框架**: Kuikly 2.4.0 (腾讯跨平台框架，一套代码运行 Android/iOS/JS/HarmonyOS)
- **构建**: Gradle 8.11.1 + AGP 8.2.2 + KSP 2.0.21-1.0.28
- **序列化**: kotlinx-serialization-json 1.7.3
- **最低支持**: Android 6.0 (minSdk 24), 目标 SDK 34

## 项目结构

```
NTnotes/
├── note/                        # KMP 主模块
│   ├── build.gradle.kts         # KMP 多平台构建配置
│   ├── proguard-rules.pro
│   └── src/
│       ├── commonMain/          # 共享代码 (UI + 业务逻辑)
│       │   └── kotlin/com/noteapp/
│       │       ├── AppRepo.kt           # 全局仓库
│       │       ├── model/               # 数据模型
│       │       ├── pages/               # Kuikly 页面组件
│       │       │   ├── NoteListPage.kt  # 笔记列表
│       │       │   ├── NoteEditPage.kt  # 笔记编辑
│       │       │   ├── RecycleBinPage.kt# 回收站
│       │       │   └── components/      # 通用 UI 组件
│       │       ├── theme/               # 主题系统
│       │       └── util/                # 工具函数
│       ├── androidMain/         # Android 平台实现
│       │   └── kotlin/com/noteapp/
│       │       ├── MainActivity.kt      # Android 入口
│       │       ├── NTnotesApp.kt        # Application 类
│       │       ├── CrashReporter.kt     # 崩溃报告
│       │       └── data/                # 数据持久化 (Android)
│       ├── iosMain/             # iOS 平台实现
│       ├── jsMain/              # JS 浏览器端实现
│       └── commonTest/          # 共享测试
├── build.gradle.kts             # 根项目构建配置
├── settings.gradle.kts          # 项目设置
├── gradle.properties            # Gradle 属性
└── local.properties             # Android SDK 路径 (已配置)
```

## 构建命令

| 目标 | 命令 |
|--------|--------|
| Android Debug | `./gradlew :note:compileDebugKotlinAndroid` |
| Android Release | `./gradlew :note:compileReleaseKotlinAndroid` |
| iOS | `./gradlew :note:compileKotlinIosX64` |
| JS | `./gradlew :note:compileKotlinJs` |
| 测试 | `./gradlew :note:allTests` |

## 开发环境

- **JDK**: 17 (sdkman 管理)
- **Android SDK**: `/opt/android-sdk` (platform 34, build-tools 34.0.0)
- **Gradle**: 8.11.1 (wrapper)
- **API**: OpenAI-compatible 端点 `https://lucen.cc/v1`

## Kuikly 注意事项

- UI 使用 Kuikly DSL：`View { attr {} }` 声明式语法
- 页面入口用 `@Page` 注解（KSP 处理）
- 响应式状态用 `PagerScope.observable(mode, value)`（勿用废弃的顶层 `observable()`）
- 定时器用 `PagerScope.setTimeout()` / `PagerScope.clearTimeout()`
- 支持 Android、iOS、JS、HarmonyOS 四端
