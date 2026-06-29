# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- (nothing yet)

## [0.2.0] - 2026-06-29

### Features
- **agent**: implement AgentLoop — streaming tool call loop with approval suspension
- **agent**: add ToolRegistry + 4 tool implementations (read_file, write_file, bash, ask_user)
- **data**: add GeminiClient with Interactions API SSE streaming + function calling
- **domain**: add IrisTool interface, ToolResult, AgentEvent sealed classes
- **ui**: chat UI with cards, slash menu, toolbar
- **data**: project management — Room, repository, UI
- **ui**: terminal icon support with powerline-style rendering

### Style & UI
- Redesign chat screen: Claude-style input bar, pill tabs, model context menu
- Update theme to near-black (#0C0C0C) + gold (#E8C547) palette
- Polish across all screens — onboarding, projects, chat
- Migrate icon set from Material Outlined to Lucide via composables/icons-lucide
- Uniform IrisBackground throughout — remove TopBar bg override
- Input bar double-border fix: OutlinedTextField borders transparent
- Replace AnimatedVisibility ExpandedPanel with ModalBottomSheet
- Status bar: remove IrisBackground override, use enableEdgeToEdge transparent
- Add statusBarsPadding/navigationBarsPadding for proper edge-to-edge

### Fixes
- Sealed class syntax (`data object` → `object`)
- KeyboardActions type annotation
- Missing imports: AnimatedVisibility, background, KeyboardActions
- Java imports for keystore properties in Gradle KTS
- LottieCompositionSpec import path
- Lucide dependency switch from android to cmp variant for ImageVector API
- Dependencies: add missing deps to app/build.gradle.kts

### Chores
- Add keystore and versioning configuration
- Remove auto changelog action and cliff config (manual changelog)
