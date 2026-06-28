# Iris Code — TODO
_Last updated: 2026-06-28_

---

## Phase 1 — Foundation (v1.0 MVP)

### 1.1 Project Setup
- [ ] Create GitHub repo (public, MIT)
- [ ] `README.md` — vision, placeholder screens, setup instruction
- [ ] Create GitHub Action Workflow
- [ ] Set up a system that auto-generates CHANGELOG.md.
- [x] Create Android Studio project (Kotlin, Compose, min SDK 23)
- [x] `gradle/libs.versions.toml` — all dependencies
- [x] Hilt setup + `@HiltAndroidApp` Application class
- [x] Directory structure: `ui/`, `domain/`, `data/`, `agent/`, `terminal/`, `di/`, `util/`
- [x] Create `util/Constants.kt`
- [x] Material 3 theme — Dark Purple/Violet color scheme
- [x] Lottie dependency

### 1.2 Onboarding
- [ ] Onboarding state machine (one-time)
- [ ] Screen 1: Welcome + Lottie animation
- [ ] Screen 2: API key input + Gemini validation call
- [ ] Screen 3: First project (SAF file picker or new folder)
- [ ] DataStore encrypted API key write
- [ ] Onboarding completed flag

### 1.3 Project Management
- [ ] Room — `Project` entity (id, name, path, lastSessionId, createdAt)
- [ ] `ProjectRepository` interface + impl
- [ ] Projects screen (list + empty state)
- [ ] Create project bottom sheet
- [ ] Delete project (confirmation dialog)

### 1.4 Agent Core
- [ ] `IrisTool` sealed interface
- [ ] `ToolResult` sealed class: `Success`, `Error`, `Cancelled`, `AwaitingApproval`
- [ ] `ToolRegistry` — registration and dispatching
- [ ] Gemini GenAI SDK — streaming + function calling
- [ ] Agent loop: send → receive → tool call → result → repeat
- [ ] `read_file` tool
- [ ] `write_file` tool (triggers DiffApproveEvent)
- [ ] `bash` tool (PTY → output stream)
- [ ] `ask_user` tool (AskCard → await response)

### 1.5 Chat UI
- [ ] Chat screen (LazyColumn + input bar)
- [ ] `BashCard` — command + live output
- [ ] `DiffCard` — unified diff + Approve/Reject
- [ ] `AskCard` — question + input/options
- [ ] Slash menu (triggered by `/`)
- [ ] @ mention picker
- [ ] User / agent message bubbles
- [ ] Work Mode chip in toolbar (PLAN / BUILD / AUTO)
- [ ] Work Mode state in ViewModel — gates tool execution
- [ ] PLAN mode: disable write_file + bash tools, agent informed via system prompt
- [ ] AUTO mode: force both autonomy toggles ON for session duration
- [ ] `/mode` slash command support
- [ ] Model name chip in toolbar (short name, e.g. "flash")
- [ ] Tap model chip → opens model bottom sheet (same as /models)

### 1.6 Command Palette
- [ ] Bottom sheet — smooth open
- [ ] Fuzzy search (Kotlin, lightweight)
- [ ] Project directory indexing
- [ ] Short press → @ mention
- [ ] Long press → file preview

### 1.7 Terminal Tab
- [ ] PTY impl (termux/termux-app reference)
- [ ] Terminal emulator UI (monospace, scroll, input)
- [ ] Open in project directory
- [ ] NO agent access

### 1.8 Diff / Approve Flow
- [ ] `DiffEngine` — git diff first, java-diff-utils fallback
- [ ] Unified diff parse → DiffCard data
- [ ] Approve → write to file
- [ ] Reject → notify agent

### 1.9 Compaction Pipeline
- [ ] Token counter
- [ ] Threshold: ~75% context window
- [ ] Compaction trigger → cheap model call
- [ ] Compressed context → write to session
- [ ] `/info` card UI

### 1.10 Settings
- [ ] API key management
- [ ] Default model selection
- [ ] Autonomy toggle: "Auto-approve file writes"
- [ ] Autonomy toggle: "Auto-run bash commands"
- [ ] Theme: dark / light
- [ ] About: version, license, GitHub

---

## Phase 2 — Multi-Provider (v1.1)

- [ ] `LLMProvider` interface (abstraction)
- [ ] Anthropic Claude integration
- [ ] OpenAI GPT integration
- [ ] Update model selection UI
- [ ] Session history screen
- [ ] Error + retry mechanism

---

## Phase 3 — Git + MCP + IPC (v1.2)

- [ ] `/git` bottom sheet (status, stage, commit, push, branch, log)
- [ ] MCP HTTP/SSE client
- [ ] MCP server management (Settings)
- [ ] Automatic agent MCP tool use
- [ ] Unix socket IPC server
- [ ] Terminal slash commands (`/open_settings`, `/open_diff`, etc.)

---

## Phase 4 — Power Features (v2.0)

- [ ] Ollama local model
- [ ] SSH remote project
- [ ] Parallel agent sessions
- [ ] F-Droid repo setup

---

## Technical Notes

- PTY is the riskiest part — try in a separate spike branch, start early
- Gemini model string → keep in `Constants.kt`, easily updatable
- API key encryption → `androidx.security:security-crypto`
- Compaction edge case: what happens if a new message arrives during session compaction?