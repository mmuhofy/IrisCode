# Iris Code — Memory Bank
_Last updated: 2026-07-01_

---

## Project Identity

| Field | Value |
|-------|-------|
| App name | Iris Code |
| Package | `com.iris.iriscode` |
| Tagline | "Code anywhere. Agent-powered." |
| License | MIT |
| Distribution | F-Droid first, Play Store later |
| Repo | github.com/mmuhofy/IrisCode |

---

## Vision

Android-native, standalone agentic coding environment.
Claude Code / OpenCode'un mobil versiyonu — ama bağımsız agent core ile.
Termux power user ve mobil geliştiriciler için.
Dışarıda hiçbir server çalışmadan, telefonun kendisinde çalışır.
Kullanıcı talimat verir → agent yazar → kullanıcı diff'i approve eder.
Kod editörü yok. Kullanıcı reviewer rolünde.

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
| Session Search | SQLite FTS5 (Room) |

---

## Visual Identity

| Element | Value |
|---------|-------|
| Color scheme | Near-black + Warm Gold |
| Background | `#0C0C0C` |
| Surface | `#141414` |
| Surface 2 | `#1A1A1A` |
| Border | `#1E1E1E` |
| Border subtle | `#232323` |
| Primary accent | `#E8C547` (warm gold) |
| Text primary | `#EEEEEE` |
| Text secondary | `#888888` |
| Text muted | `#666666` |
| Text disabled | `#444444` |
| Success | `#27AE60` |
| Error | `#C0392B` |
| Warning | `#C9A84C` |
| Code font | JetBrains Mono |
| UI font | Inter / system |
| Corner radius | 14dp cards, 12dp buttons, 8dp chips |
| Loading | Lottie — geometric eye mid-blink, gold iris glow, "Iris Code" fade in |
| App icon | Stylized "I" + terminal cursor concept (TBD final) |
| Theme | Dark only (Light v1.1, Theme Store v2) |

---

## Architecture Layers

```text
ui/        → Compose screens, ViewModels
domain/    → Use cases, interfaces, IrisTool — pure Kotlin
data/      → Room, Gemini API client, repo implementations
agent/     → Agent loop, ToolRegistry, compaction, streaming
terminal/  → PTY session, terminal emulator bridge
di/        → Hilt modules
util/      → Constants, extensions
```

---

## Navigation Architecture

No bottom navigation bar. Linear hierarchy:

```text
Projects (Home)
  → [project card]  → Session List
                       → [session]  → Chat (tabs: Chat / Terminal / Files) ↔ Terminal
                       → [+]        → Create → Chat
  → [+]             → Create → Chat
  → [⚙️]            → Settings (top right, every screen)
```

- `←` back always returns to previous screen
- `[+]` top right = new session, available on every screen
- Tab bar (Chat / Terminal / Files) inside Chat screen, top position, seamless background
- Overflow menu `[⋮]` top right on Chat screen: Settings, New Session, Export Session
- Settings icon top right on every screen

---

## Screen Inventory

### Projects (Home)
- Project cards: name, path, last session summary, branch, session count, timestamp
- Gold left border accent on each card
- Long press card → context menu: Rename, Delete
- Empty state: blinking `█` cursor + "No projects yet."

### Session List
- Grouped by date (Today, Yesterday, older)
- Each row: session summary, tool call count, duration, cost, timestamp
- Long press → Export session (markdown)

### Chat
- Toolbar: `←` · project name · branch · `[⋮]` · `[⚙️]`
- Model chip centered (pill, border + press animation + caret rotation)
- Status: inline agent message in chat ("◐ Reading auth.ts...")
- Tab bar top (inside rounded content area): `[💬 Chat]` `[💻 Terminal]` `[📁 Files]`
- Full-width messages, no bubbles
- User message: right-aligned card, gold left border
- Agent message: left-aligned, no card, text on background
- Cards: BashCard, DiffCard, AskCard, TodoCard, CodeCard, ReadFileCard

### Terminal
- User-only (agent cannot write here)
- Opens in project directory
- Gold prompt color
- Agent bash output → BashCard in chat, NOT here

### Create
- Accessible from `[+]` on any screen
- Optional path input top
- Large multiline input center: "What do you want to build?"
- Send → session starts, Chat opens

### Settings
- API key management (per provider)
- Default model selection
- Work Mode default
- Auto-approve writes toggle (OFF default)
- Auto-run bash toggle (OFF default)
- Voice input language
- Global Rules editor
- Theme: Dark / Light
- About: version, license, GitHub

---

## Chat Cards

### BashCard
- Header: "bash" label gold monospace + copy icon
- Command line: gold monospace
- Output: gray monospace, success green, error red
- Short (≤8 lines): show fully
- Long (>8 lines): collapse + "Show X more lines ↓"
- Very long (50+ lines): "Open in full screen ↑"

### DiffCard
- Header: "write_file" gold + filename + "+X −Y" green/red
- Small diff (≤20 lines): inline full
- Medium (20–50 lines): collapse + "Show X more ↓"
- Large (50+ lines): "Open full diff ↑" → full screen bottom sheet
- Full screen: scrollable diff + Approve/Reject bottom bar
- Approve → card fades green → "✓ Applied" → collapses
- Reject → card flashes red → "✕ Rejected" → agent notified

### AskCard
- Question text
- Options listed vertically (radio style)
- Free text input always available below options
- Select option → fills input, user can edit
- Confirm → card locks, shows "✓ [answer]"

### TodoCard
- Header: checklist icon + "Plan" + "3/5" counter + mini dots
- Steps with status icons: ✓ ◐ ○ ✕ ⊘
- Active step: gold pulse animation, warm row tint
- Progress bar: gold fill, percentage right
- 6+ steps: collapsed by default, tap to expand
- Completed: green header + "Plan completed" + collapses to summary
- Error: red ✕ + error message inline + "⚠ Stopped" if halted

### CodeCard
- Language label top left gold monospace
- Copy button top right
- Syntax highlighted code
- Collapse if long

### ReadFileCard
- Triggered by @ mention long press
- Filename header + close button
- Scrollable syntax highlighted content
- "X lines · Language" footer

---

## Agent Core

### Tool Set (MVP — minimal by design)

```text
read_file      → reads file into agent context
write_file     → triggers DiffApproveEvent
bash           → PTY execution → BashCard in chat
ask_user       → AskCard in chat, blocks until answered
plan           → creates TodoCard, called first on complex tasks
update_todo    → updates TodoCard step status live
```

Philosophy: terminal varsa tool'a gerek yok. grep, ls, git → terminal'den.

### Work Mode

| Mode | Behavior |
|------|----------|
| PLAN | Read-only. Agent suggests only. write_file + bash disabled. |
| BUILD | Full tool use. Diff/Approve flow active. Default. |
| AUTO | BUILD + both autonomy toggles forced ON. Session-scoped only. |

Switchable: input bar chip or `/mode` slash command.

### Autonomy Toggles (Settings)
- "Auto-approve file writes" (OFF default)
- "Auto-run bash commands" (OFF default)
- AUTO mode forces both ON for session duration, not persisted

### Agent While Working — Input Bar Behavior
- Input bar stays active (user can type next message)
- Send button becomes swipe-up gesture → queues message
- "⏱ 1 message queued" indicator shown
- Stop button appears replacing send
- Stop → agent finishes current tool, then stops → "● Stopped by user"

### Compaction Pipeline
- Auto-triggered at ~75% context window
- Cheap model call to summarize old context
- User sees no interruption
- `/info` shows remaining tokens before next compaction

### Error Handling
- Model logic error → agent self-recovers, no retry button
- Server/connection error → exponential backoff + retry button
- Agent timeout (no response) → retry button
- Message undo → available as feature

### Session Behavior
- Background: session pauses, resumes on return
- Export: markdown format
- Search: SQLite FTS5 across all sessions

---

## Input Bar

Two-layer layout, always expanded (2–3 line height):

```text
┌─────────────────────────────────────┐
│  Message Iris...                    │
│  (multiline, expands upward)        │
├─────────────────────────────────────┤
│  [/]  [+]  [🎤]      [Effort] [▲] │
└─────────────────────────────────────┘
```

Send button swipe-up → queue message while agent working.

"+" expands panel upward:
- Session info strip (tokens, cost, compaction remaining) — tappable for /info
- Mode selector: PLAN / BUILD / AUTO pills
- Effort selector: Low / Med / High pills (disabled when Thinking OFF)
- Thinking toggle
- Web Search toggle
- Attach: File / Image / Camera

### Slash Menu (`/`)

```text
/models    → model switcher bottom sheet
/auth      → API key management
/info      → token/session info card in chat
/new       → new session
/history   → session list
/git       → git bottom sheet
/mcp       → MCP server list
/settings  → settings screen
/mode      → work mode switcher
```

### Voice Input
- Mic icon `[🎤]` in input bar
- Hold to record (push-to-talk)
- Release → Whisper API → transcript in input field
- Auto-send option in settings
- Language: settings configurable

---

## Keyboard Shortcuts (External Keyboard)

```text
Cmd+Enter  → send message
Cmd+K      → command palette
Escape     → stop agent
```

---

## Command Palette
- Triggered by tapping project name in toolbar
- Bottom sheet, smooth open, always focused
- Fuzzy search ("mago" → "main.go")
- Gold cursor, always active border
- Short press → add as @ mention
- Long press → preview file (ReadFileCard)
- Shows recent files when empty

---

## Haptic Feedback

```text
Approve        → success haptic (medium)
Reject         → error haptic (short, sharp)
Agent done     → light haptic
Queue added    → light tick
Send           → minimal tick
```

---

## Security & Privacy

### .irisignore
- Syntax identical to .gitignore
- Auto-created on project creation with defaults: `.env`, `.env.*`
- Protected files: agent cannot delete or overwrite
- Shows warning in DiffCard/BashCard if attempted

### Deletion Protection
- Any `rm` / `unlink` / `rmdir` → always shows special confirm card
- Even in AUTO mode
- Lists files to be deleted
- .irisignore protected files shown greyed, excluded
- Delete button always #C0392B regardless of mode

### API Keys
- Stored encrypted via androidx.security:security-crypto
- Never logged, never sent except to provider
- Per-provider storage

---

## Project Memory (.iris/)

```text
.iris/
  memory.md        → agent reads + updates each session
                     project context, tech stack, decisions
  rules.md         → user-defined agent rules
                     project-level, overrides global rules
  sessions/
    2026-06-28-001.md  → session summary (auto-generated)
```

Global rules → Settings → "Global Rules" editor
Project rules → .iris/rules.md
Priority: project rules > global rules

---

## MCP Client
- HTTP/SSE based
- Managed in Settings
- Agent uses automatically
- v1.2 milestone

## IPC (Terminal → UI)
- Unix socket server in app
- `/open_settings`, `/open_diff` etc. from terminal
- v1.2 milestone

---

## Onboarding (3 Steps, First Launch Only)
1. Splash / Welcome — logo + Lottie animation + tagline
2. API Key — Gemini key entry, Google AI Studio link, validation
3. Create first session — lands on Create screen

---

## Notifications
- "Iris finished — 3 files changed. Review needed."
- Approve / Reject actions directly from notification
- Agent working in background → foreground service

---

## MVP Scope

### v1.0 — In
- Projects screen
- Session List screen
- Chat screen + Terminal tab
- Create screen
- Agent core: Gemini 2.5 Flash, tool use loop
- Tools: read_file, write_file, bash, ask_user, plan, update_todo
- All chat cards: Bash, Diff, Ask, Todo, Code, ReadFile
- Diff / Approve flow + deletion protection
- .irisignore
- Work Mode (PLAN/BUILD/AUTO)
- Autonomy toggles
- Input bar: slash menu, voice input, attach, effort, thinking, web search
- Command palette (fuzzy file search)
- @ mention + file preview
- Compaction pipeline
- Session export (markdown)
- Session search (FTS5)
- Haptic feedback
- Keyboard shortcuts
- Notifications + background agent
- .iris/ project memory
- Global + project rules
- Settings screen
- Dark theme
- Onboarding (3 steps)

### v1.1
- Anthropic + OpenAI providers
- Light theme
- Session search improvements

### v1.2
- Git UI (/git bottom sheet)
- MCP HTTP/SSE client
- IPC terminal shortcuts

### v2.0
- Ollama local model
- SSH remote project
- Theme Store
- WearOS companion

---

## Reference Repositories

| System | Reference |
|--------|-----------|
| PTY / Terminal | `termux/termux-app` |
| Agent loop | `anomalyco/opencode` — `packages/opencode/src/` |
| MCP client | `anomalyco/opencode` — MCP integration |
| Diff | `java-diff-utils`, `git diff` |

---

## Current Status (2026-07-01)

### Terminal Bootstrap
- [OK] Vendored termux emulator + view modules
- [OK] Pre-built libtermux.so in jniLibs
- [OK] `TermuxBootstrap.kt` downloads bootstrap from GitHub releases
- [FIXED] Improved exception handling: added `Log.e` with full stack trace + exception class name in Failed message
- [FIXED] Added required `User-Agent` header to GitHub API requests

### Known Issues
- Bootstrap `Permission denied` — fixed: `setExecutable(true, false)` (owner+group+other), plus post-extraction walk hardening all bin/ files
- Need to test end-to-end on actual Android device

## Open Decisions
- App icon: "I" + cursor concept, final design TBD
- Lottie animation: commission or self-design
- GitHub repo URL: TBD
- Notification Approve/Reject: write_file only or ask_user too (TBD)
- Dosya silme güvenliği trash mekanizması: postponed
- Projects screen layout: kararsız (session list ayrı ekran olarak netleşti)
