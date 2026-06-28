# AGENT.md — Iris Code Dev Agent Instructions

---

## ANTI-HALLUCINATION PROTOCOL (HIGHEST PRIORITY)

These rules override everything else:

- **NEVER invent API names, method signatures, class names, or library features.**
  If you are not certain a method exists in the exact version being used, say so explicitly.
- **NEVER assume a dependency version is compatible.**
  Always reference the exact version from `gradle/libs.versions.toml`.
- **If you don't know something, say "I don't know" or "I need to verify this."**
  Do not fill gaps with plausible-sounding fabrications.
- **When referencing Jetpack Compose or any Jetpack API:** always explicitly state the
  target version. If uncertain about a class or method existing in that version, flag it.
- **When referencing external APIs (Gemini, etc.):** always explicitly state the API
  version/endpoint. If uncertain, flag it and ask Muhofy to confirm via official docs.
- **Code that has not been tested must be labeled:**
  `// UNTESTED — verify before use` on any non-trivial logic block.
- **Do not silently rename or refactor existing code** unless explicitly asked.
  Muhofy's existing code is canonical.

---

## INSPIRATION-FIRST PROTOCOL (CRITICAL SYSTEMS)

For any critical or complex system, **never write from scratch without first studying
real-world reference implementations.**

### What counts as a critical system:
- PTY / terminal emulator implementation
- Agent tool use loop / streaming response handling
- Compaction pipeline
- MCP client protocol
- Diff engine integration
- Any IPC / socket mechanism

### Process:
1. **Identify the best open-source reference** for the system being built.
   Default references for this project:
   - PTY/Terminal → `termux/termux-app` (GitHub)
   - Agent loop / tool use → `anomalyco/opencode` — `packages/opencode/src/`
   - MCP client → `anomalyco/opencode` — MCP integration source
   - Diff → `java-diff-utils` or `git diff` integration examples
2. **Ask Muhofy for the specific raw file URL** from the reference repo, OR
   **fetch it directly** if the file path is known and publicly accessible.
3. **Read and study the reference** before writing any code.
4. **Adapt and translate** to Kotlin/Android — original code informed by reference.
5. **Document the inspiration source** in a comment at the top of the file:
   `// Inspired by: github.com/anomalyco/opencode/packages/opencode/src/...`

### What does NOT need a reference:
- UI/UX components (Compose screens, layouts, animations)
- ViewModels, Use Cases, Repository pattern boilerplate
- Room entities/DAOs
- Standard Android navigation, theming, settings screens

---

## GIT RULES

- **Never output a commit message for Muhofy to run manually.**
- **Always commit and push directly** using available git tools after every implementation.
- Commit message format:
  <type>(<scope>): <short description>

| Type | When |
|------|------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Restructure, no behavior change |
| `perf` | Performance improvement |
| `style` | Formatting only |
| `docs` | Documentation only |
| `test` | Tests added/updated |
| `chore` | Tooling, config, build |

- Imperative mood, all lowercase, no period, max 72 chars.
- Scope = module: `ui`, `domain`, `data`, `agent`, `terminal`, `git`, `mcp`, `util`, `di`

---

## CODING STANDARDS

- All code comments in **English**
- No magic numbers — use named constants in `util/Constants.kt`
- No hidden side effects
- Explicit error handling — no silent failures
- Use `sealed class` / `sealed interface` for UI state, tool results, agent events
- Prefer immutable data (`val` over `var`, immutable collections)
- Use Kotlin `data class` for all model/entity types
- All `suspend` functions called from appropriate coroutine scopes
- Never access database, network, or PTY APIs directly from a ViewModel — goes through Use Cases

### Layer Responsibilities

ui/        → Compose screens, components, ViewModels. No direct data access.
domain/    → Use cases, business logic, repository interfaces, IrisTool interface.
             Pure Kotlin only. Zero Android imports unless unavoidable — flag explicitly.
data/      → Repository implementations, Room DAOs, Gemini API client.
agent/     → Agent loop, ToolRegistry, compaction pipeline, streaming handler.
             Depends on domain/ interfaces only.
terminal/  → PTY session management, terminal emulator bridge.
di/        → Hilt modules only. No logic.
util/      → Constants, extension functions, shared helpers.

### Data Flow

Compose Screen
      ↓ UI events
ViewModel (ui/)
      ↓ calls
Use Case (domain/)
      ↓ calls
Repository Interface (domain/)
      ↓ implemented by
Repository Impl (data/) / Agent (agent/) / Terminal (terminal/)
      ↓
Room DAO / Gemini API / PTY

### Agent Tool Flow

Gemini function_call response
      ↓
ToolRegistry.execute(name, args)
      ↓
IrisTool implementation
      ↓ (if write_file)
DiffApproveEvent → UI DiffCard shown → User Approve / Reject
      ↓ (if bash)
PTY execution → output streamed → BashCard in chat
      ↓
ToolResult (Success | Error | Cancelled | AwaitingApproval)

---

## FILE & ARTIFACT RULES

- Always provide files as artifacts — never inline as plain text
- One artifact per file, full content always — never truncate
- If updating an existing file, use artifact update mechanism
- Artifact title must match actual filename (e.g., `ToolRegistry.kt`)
- Always include full project path:
  app/src/main/java/com/iris/iriscode/agent/ToolRegistry.kt
- Never refer to a file by name alone — always pair with full path.

---

## WEB RESEARCH PROTOCOL

- If a fetch or search returns empty/failed:
  1. Do not hallucinate the content
  2. Provide the exact URL to Muhofy
  3. Ask Muhofy to paste the relevant content
  4. Only proceed once real content is provided
- For external APIs, if behavior/parameters are uncertain,
  fetch official docs before writing integration code — do not guess.

---

## PROJECT FILE ACCESS RULES

- Do NOT assume project files exist locally.
- Before modifying or referencing project code, request the GitHub raw URL from Muhofy.
- Never reuse previously fetched file contents — always re-fetch from a fresh raw URL.
- If a required file has not been provided: stop, request the URL, do not reconstruct from memory.

---

## TARGET STACK

| Component | Technology | Version |
|-----------|------------|---------|
| Package | `com.iris.iriscode` | — |
| Language | Kotlin | `2.3.20` |
| UI | Jetpack Compose | BOM `2026.04.01` |
| Material | Material 3 | via BOM |
| Min SDK | 23 | — |
| Architecture | MVVM + Clean Architecture | — |
| DI | Hilt | confirm in `libs.versions.toml` |
| Navigation | Navigation Compose | via BOM |
| Local DB | Room | `2.x` stable |
| Async | Kotlin Coroutines + Flow | confirm in `libs.versions.toml` |
| LLM (v1.0) | Gemini 2.5 Flash | Google GenAI SDK |
| LLM (v1.1+) | + Anthropic, OpenAI | — |
| HTTP/Stream | OkHttp + SSE | `4.x` stable |
| Diff | `java-diff-utils` + `git diff` | fallback strategy |
| Build | Gradle KTS + Version Catalog | — |
| Annotation | KSP | match Kotlin version |
| License | MIT | — |
| Theme | Dark Purple / Violet | `#0D0D14` bg, `#7C3AED` primary |

> ⚠️ Always confirm versions against `gradle/libs.versions.toml` before referencing any API.
> ⚠️ Gemini model strings change — verify against `ai.google.dev/gemini-api/docs/models`.

---

## ARCHITECTURE RULES

- `ui/` never imports from `data/` — domain is the boundary
- `domain/` is pure Kotlin — zero Android imports unless unavoidable, flag explicitly
- `data/` implements interfaces from `domain/`
- `agent/` depends only on `domain/` interfaces, connected via DI
- `terminal/` is isolated — only `data/` and `agent/` interact with it
- ViewModels expose `StateFlow<UiState>` — never expose mutable state
- UI collects with `collectAsStateWithLifecycle()`

---

## TOOL SYSTEM RULES

Every tool implements `IrisTool` interface:
- Fields: `name`, `description`, `parameters`
- Method: `suspend fun execute(args: Map<String, Any>): ToolResult`
- `ToolResult` → `sealed class`: `Success`, `Error`, `Cancelled`, `AwaitingApproval`
- `write_file` → always triggers `DiffApproveEvent` before writing
- `bash` → streams output to chat as `BashCard`; respects AutonomyLevel toggle
- New tools added one at a time per Muhofy's spec — no speculative tools

### Core Tool Set (MVP)

read_file    → reads file content, returns to agent context
write_file   → triggers diff/approve flow, writes only after Approve
bash         → executes in PTY, output streams to BashCard in chat
ask_user     → surfaces AskCard in chat, agent blocks until answered

### Work Mode Enforcement
- PLAN mode → `ToolRegistry` must reject `write_file` and `bash` calls silently, returning `ToolResult.Cancelled(reason = "PLAN mode — read only")`
- Agent system prompt must be updated per mode:
  - PLAN: "You are in read-only mode. Analyze and suggest only. Do not write files or run commands."
  - BUILD: standard prompt
  - AUTO: standard prompt, no confirmation needed
- AUTO mode overrides both autonomy toggles — do not persist this to Settings, it is session-scoped only

---

## MEMORY BANK

- Always read `memory-bank.md` at session start
- Always update `memory-bank.md` after every confirmed change
- Never contradict Memory Bank without explicit Muhofy approval
- If Memory Bank is missing or incomplete, ask before proceeding