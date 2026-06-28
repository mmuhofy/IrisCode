# Iris Code — Memory Bank
_Last updated: 2026-06-28 — Session 1: Project Setup completed_

---

## Project Identity

| Field | Value |
|-------|-------|
| App name | Iris Code |
| Package | `com.iris.iriscode` |
| Tagline | "Code anywhere. Agent-powered." |
| License | MIT |
| Distribution | F-Droid + signed APK (GitHub Releases) |
| Repo | TBD — to be created |

---

## Vision

Android-native, standalone agentic coding environment.
Mobile version of Claude Code / OpenCode — but with an independent agent core.
Built for Termux power users and mobile developers.
Runs locally on the phone without any external server infrastructure.
User gives instructions → agent writes → user approves the diff.
No code editor. User strictly acts as a reviewer.

---

## Confirmed Stack

| Component | Decision |
|-----------|----------|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose BOM 2026.04.01 |
| Min SDK | 23 |
| Android Studio | Quail 1 — 2026.1.1 Patch 2 |
| LLM (v1.0) | Gemini 2.5 Flash (Google GenAI SDK) |
| LLM (v1.1+) | + Anthropic, OpenAI |
| HTTP/Stream | OkHttp + SSE |
| Storage | Room 2.x + DataStore (encrypted keys) |
| File Ops | Kotlin File API + Okio |
| Diff | git diff (primary) + java-diff-utils (fallback) |
| DI | Hilt |
| Build | Gradle KTS + Version Catalog |
| Terminal/PTY | JNI — inspired by termux/termux-app |
| Fuzzy Search | Lightweight Kotlin impl |
| IPC | Unix Domain Socket (v1.2) |
| Loading Anim | Lottie |
| Architecture | MVVM + Clean Architecture |

---

## Visual Identity

| Element | Value |
|---------|-------|
| Color scheme | Dark Purple / Violet |
| Background | `#0D0D14` |
| Surface | `#13131F` |
| Primary | `#7C3AED` (violet-600) |
| Accent | `#A78BFA` (violet-400) |
| Text | `#F4F4F5` |
| Subtle text | `#71717A` |
| Code font | JetBrains Mono |
| UI font | Inter / system |
| Loading | Lottie — stylized eye, slow blink, violet glow, "Iris Code" fade in |

---

## Architecture Layers

ui/        → Compose screens, ViewModels
domain/    → Use cases, interfaces, IrisTool — pure Kotlin
data/      → Room, Gemini API client, repo implementations
agent/     → Agent loop, ToolRegistry, compaction, streaming
terminal/  → PTY session, terminal emulator bridge
di/        → Hilt modules
util/      → Constants, extensions

---

## Core Features

### Work Mode
Three modes, switchable from toolbar chip or `/mode` slash command:

| Mode  | Behavior |
|-------|----------|
| PLAN  | Read-only. Agent analyzes and suggests. write_file + bash disabled. |
| BUILD | Full tool use. write_file + bash active. Diff/Approve flow on. Default. |
| AUTO  | BUILD + both autonomy toggles forced ON. Agent writes and runs without asking. |

UI: Toolbar chip `[ PLAN | BUILD | AUTO ]` + `/mode plan`, `/mode build`, `/mode auto` slash commands.

### In-Chat Model Switcher
- Toolbar shows current model name (short): `[flash ▾]`
- Tap → opens model bottom sheet (same as `/models`)
- Toolbar layout:
  `← [Project Name ▾]  [BUILD]  [flash ▾]`

### Agent Chat
- Gemini 2.5 Flash, tool use loop
- BYO API Key (encrypted DataStore)
- Per-project session history
- Compaction pipeline (auto, token threshold)
- `/info` card: tokens used, cost estimate, compaction remaining, model, duration, tool calls

### Tool Set (MVP — minimal)

read_file    → agent reads file into context
write_file   → triggers DiffApproveEvent
bash         → PTY execution, BashCard in chat
ask_user     → AskCard in chat, blocks until answered

Philosophy: If there is a terminal, no tool is needed. grep, ls, git → done via terminal.

### Autonomy Toggles (Settings)
- "Auto-approve file writes" (OFF default)
- "Auto-run bash commands" (OFF default)

### Chat Cards
- `BashCard` → command + live output
- `DiffCard` → unified diff green/red + Approve / Reject
- `AskCard` → question + input or option buttons
- `ReadFileCard` → file preview (@ mention long press)

### Terminal Tab
- User-only (agent cannot write here)
- Opens in project directory
- Agent bash output → BashCard in chat, NOT terminal tab

### Navigation

[ 📁 Projects ]  [ + Create ]  [ ⚙️ ]

### Slash Menu

/models    → model switcher
/auth      → API key management
/info      → token/session info card
/new       → new session
/history   → session history
/git       → git bottom sheet
/mcp       → MCP server list
/settings  → settings screen

### Command Palette
- Tap project name in toolbar → bottom sheet
- Fuzzy search
- Short press → @ mention in chat
- Long press → file content preview

### Compaction Pipeline
- Auto-triggered by token threshold (~75% of context window)
- Cheap model call to summarize
- User sees no interruption

### MCP Client
- HTTP/SSE based
- Managed in Settings
- Agent uses automatically
- v1.2 milestone

### IPC (Terminal → UI)
- Unix socket server in app
- `/open_settings`, `/open_diff`, etc. from terminal
- v1.2 milestone

---

## Onboarding (First Launch Only)

1. Welcome — logo + tagline
2. API Key — Gemini, Google AI Studio link, validation call
3. First project — folder picker or create new
4. Chat screen — placeholder: "What do you want Iris to do?"
5. Tooltip: "You can open the command menu with /"

---

## MVP Scope

### v1.0 — In
- Project list + create
- Agent chat (Gemini only)
- Tool use loop: read_file, write_file, bash, ask_user
- Chat cards: BashCard, DiffCard, AskCard
- Diff / Approve flow
- Terminal tab (user only)
- Slash menu
- Command palette (fuzzy file search)
- @ mention + file preview
- Compaction pipeline
- Autonomy toggles (2)
- /info token card
- API key settings
- Dark/light theme
- Onboarding flow

### v1.1 — Multi-provider
- Anthropic Claude
- OpenAI GPT
- Provider abstraction layer

### v1.2 — Git + MCP + IPC
- /git slash command → Git bottom sheet
- MCP HTTP/SSE client
- Unix socket IPC

### v2.0 — Power
- Ollama local model
- SSH remote project
- Parallel agent sessions
- F-Droid repo

---

## Reference Repositories

| System | Reference |
|--------|-----------|
| PTY / Terminal | `termux/termux-app`, 'AndroidCSOfficial/android-code-studio (very important) |
| Agent loop / Tool use | `anomalyco/opencode` — `packages/opencode/src/` |
| MCP client | `anomalyco/opencode` — MCP integration |
| Diff | `java-diff-utils`, `git diff` |

---

## Session Log

### Session 1 — 2026-06-28: Project Setup (1.1)
- GitHub repo created: https://github.com/mmuhofy/IrisCode (public, MIT)
- `ci.yml` — Android CI workflow (build + lint + APK upload)
- `changelog.yml` + `cliff.toml` — auto-generate CHANGELOG.md on release via git-cliff
- `LICENSE` — MIT License
- Initial commit pushed to `main`

### Session 2 — 2026-06-28: Onboarding (1.2)
- `OnboardingPreferences`: DataStore (state) + EncryptedSharedPreferences (API key)
- `OnboardingViewModel`: 3-step state machine (Welcome → ApiKey → ProjectSetup → Complete)
- `WelcomeScreen`: fade-in animations, app name + tagline, "Get Started" button
- `ApiKeyScreen`: text input, Gemini models endpoint validation via OkHttp, skip option
- `ProjectSetupScreen`: SAF `OpenDocumentTree` folder picker, path display
- `MainActivity`: `@AndroidEntryPoint`, `AnimatedContent` transitions
- Manifest: `INTERNET` permission, `MainActivity` as launcher activity
- Updated `gradle/libs.versions.toml` with full dependency catalog (Compose, Hilt, Room, Navigation, etc.)
- Root `build.gradle.kts`: added Hilt, KSP, Compose Compiler plugins
- `app/build.gradle.kts`: Compose enabled, all dependencies wired
- Created directory structure: `ui/`, `domain/`, `data/`, `agent/`, `terminal/`, `di/`, `util/`
- `IrisCodeApp.kt` — `@HiltAndroidApp` Application class
- `util/Constants.kt` — visual identity constants
- `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — Material 3 Dark Purple/Violet theme
- `di/AppModule.kt` — empty Hilt module scaffold
- `AndroidManifest.xml` — added `android:name=".IrisCodeApp"`
- `.gitignore` added
- KSP version fixed to `2.1.0-1.0.29` (Kotlin 2.1.0 compatible)
- Build blocked by missing Android SDK (Termux environment)

### Open Decisions

- GitHub repo URL: https://github.com/mmuhofy/IrisCode
- Lottie animation: commission or self-design?