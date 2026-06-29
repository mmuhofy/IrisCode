# Iris Code — TODO
_Last updated: 2026-06-29_
_Repo: https://github.com/mmuhofy/IrisCode_

---

## Phase 1 — Foundation (v1.0 MVP)

### 1.1 Project Setup
- [x] GitHub repo oluştur (public, MIT) — https://github.com/mmuhofy/IrisCode
- [ ] `README.md` — vizyon, placeholder ekranlar, kurulum
- [x] GitHub Actions CI — `ci.yml` (build + lint + APK)
- [x] Auto CHANGELOG sistemi — `changelog.yml` + `cliff.toml`
- [x] Android Studio projesi (Kotlin, Compose, min SDK 23)
- [x] `gradle/libs.versions.toml` — tüm bağımlılıklar
- [x] Hilt + `@HiltAndroidApp` Application sınıfı
- [x] Klasör yapısı: `ui/`, `domain/`, `data/`, `agent/`, `terminal/`, `di/`, `util/`
- [x] `util/Constants.kt`
- [x] Material 3 tema — renk şeması (güncellenecek: Dark Purple → Near-black + Gold)
- [x] Lottie bağımlılığı

### 1.2 Onboarding
- [x] Onboarding state machine (tek seferlik) — `OnboardingViewModel` 3 adım
- [x] Ekran 1: Welcome + fade-in animasyonları
- [x] Lottie animasyonu — göz ikonu (`.lottie` format)
- [x] Ekran 2: API key girişi + Gemini REST validation
- [x] Ekran 3: İlk proje (SAF file picker veya yeni klasör)
- [x] DataStore encrypted API key write
- [x] Onboarding tamamlandı flag

### 1.3 Project Management
- [x] Room — `Project` entity (id, name, path, lastSessionId, createdAt)
- [x] `ProjectRepository` interface + impl
- [x] Projects ekranı (liste + boş state)
- [x] Create project bottom sheet
- [x] Proje silme (onay dialog)
- [ ] Session List ekranı (proje kartına basınca açılır)
- [ ] Session kartları: summary, tool call count, duration, cost, timestamp
- [ ] Session gruplandırma: Today / Yesterday / older
- [ ] Navigation mimarisi güncelle — bottom nav kaldır, linear hierarchy

### 1.4 Agent Core
- [x] `IrisTool` sealed interface
- [x] `ToolResult` sealed class: `Success`, `Error`, `Cancelled`, `AwaitingApproval`
- [x] `ToolRegistry` — kayıt ve dispatch
- [x] Gemini REST API — streaming + function calling (GenAI SDK yok, direkt REST)
- [x] Agent loop: send → receive → tool call → result → repeat
- [x] `read_file` tool
- [x] `write_file` tool (DiffApproveEvent tetikler)
- [x] `bash` tool (PTY → output stream)
- [x] `ask_user` tool (AskCard → await response)
- [ ] `plan` tool — TodoCard oluşturur, karmaşık görevlerde ilk çağrılır
- [ ] `update_todo` tool — TodoCard adım durumunu canlı günceller
- [ ] Exponential backoff + retry butonu (server/connection hatalarında)
- [ ] Agent timeout detection + retry butonu
- [ ] Message undo özelliği
- [ ] Message queue — agent çalışırken mesaj sıraya alınır

### 1.5 Chat UI
- [x] Chat ekranı (LazyColumn + input bar)
- [x] `BashCard` — komut + live output
- [x] `DiffCard` — unified diff + Approve/Reject
- [x] `AskCard` — soru + input/seçenekler
- [x] User / agent mesaj baloncukları
- [x] Work Mode chip toolbar'da (PLAN / BUILD / AUTO)
- [x] Work Mode ViewModel state — tool execution'ı kısıtlar
- [x] PLAN mode: write_file + bash devre dışı, sistem prompt güncellenir
- [x] AUTO mode: her iki autonomy toggle zorla ON, session-scoped
- [x] `/mode` slash command
- [x] Model chip toolbar'da (kısa isim, ör: "flash")
- [x] Model chip → model seçim bottom sheet
- [ ] @ mention picker (Command Palette'ten gelir)
- [ ] `TodoCard` — canlı güncellenen görev listesi
- [ ] `CodeCard` — syntax highlight + copy butonu
- [ ] `ReadFileCard` — @ mention long press → dosya preview
- [ ] AskCard güncelle: seçenekler alt alta, serbest yazma alanı her zaman görünür
- [ ] BashCard collapse: ≤8 satır tam, >8 "Show X more ↓", 50+ "Open full screen ↑"
- [ ] DiffCard collapse: ≤20 inline, 20–50 "Show more ↓", 50+ full screen bottom sheet
- [ ] DiffCard full screen: scrollable + Approve/Reject bottom bar
- [ ] DiffCard Approve animasyonu: yeşil fade → "✓ Applied" → collapse
- [ ] DiffCard Reject animasyonu: kırmızı flash → "✕ Rejected"
- [ ] TodoCard collapse: 6+ adım varsayılan collapse
- [ ] TodoCard completed: yeşil header → summary mod
- [ ] Agent status — chat içi mesaj olarak ("◐ Reading auth.ts...")
- [ ] Thinking dots animasyonu (···)
- [ ] Tool status satırları: slide in 200ms, renk geçişleri
- [ ] Kart appear animasyonu: opacity + translateY, 250ms
- [ ] Input bar güncelle: 2 katman (yazı alanı + aksiyon toolbar)
- [ ] Input bar aksiyon toolbar: [/] [+] [🎤] [Effort] [▲]
- [ ] Send butonu swipe-up → queue (agent çalışırken)
- [ ] "⏱ 1 message queued" indicator
- [ ] Stop butonu (agent çalışırken send yerine)
- [ ] Agent çalışırken input: aktif ama placeholder değişir
- [ ] "+" expand panel: session info strip, Mode, Effort, Thinking, Web Search, Attach
- [ ] Session info strip: tokens, cost, compaction kalan — tıklanabilir
- [ ] Effort picker: Low / Med / High (Thinking OFF iken disabled)
- [ ] Thinking toggle
- [ ] Web Search toggle
- [ ] Attach: File / Image / Camera
- [ ] Attached file chip: input bar üstünde "[📎 auth.ts ×]"
- [ ] Slash menu güncelle: /git, /mcp, /mode ekle
- [ ] Toolbar güncelle: proje adı + branch (⎇ main) + model chip
- [ ] Shared element transition: proje kartı → toolbar
- [ ] Chat ↔ Terminal tab geçiş animasyonu: horizontal slide

### 1.6 Command Palette
- [ ] Bottom sheet — smooth open, always focused
- [ ] Fuzzy search (Kotlin, lightweight)
- [ ] Proje dizini file indexleme
- [ ] Gold cursor, always active border
- [ ] Short press → @ mention ekle
- [ ] Long press → ReadFileCard preview
- [ ] Boş iken son açılan dosyalar göster
- [ ] Matched text gold highlight

### 1.7 Terminal
- [ ] PTY implementasyonu (termux/termux-app referans)
- [ ] Terminal emülatör UI (monospace, scroll, input)
- [ ] Proje dizininde otomatik aç
- [ ] Gold prompt rengi
- [ ] Agent erişimi YOK — sadece kullanıcı

### 1.8 Diff / Approve Flow
- [ ] `DiffEngine` — git diff önce, java-diff-utils fallback
- [ ] Unified diff parse → DiffCard data
- [ ] Approve → dosyaya yaz
- [ ] Reject → agent'a bildir

### 1.9 Compaction Pipeline
- [ ] Token sayacı
- [ ] Threshold: ~%75 context window
- [ ] Compaction trigger → ucuz model çağrısı ile özetle
- [ ] Compressed context → session'a yaz
- [ ] `/info` card UI

### 1.10 Settings
- [ ] API key yönetimi (provider bazlı)
- [ ] Varsayılan model seçimi
- [ ] Autonomy toggle: "Auto-approve file writes"
- [ ] Autonomy toggle: "Auto-run bash commands"
- [ ] Global Rules editörü
- [ ] Voice input dil seçimi
- [ ] Tema: Dark / Light
- [ ] Hakkında: versiyon, lisans, GitHub

### 1.11 Security & Protection
- [ ] .irisignore parser (gitignore syntax)
- [ ] .irisignore varsayılan oluşturma (proje kurulumunda)
- [ ] rm/unlink/rmdir → özel silme onay kartı (AUTO mode dahil)
- [ ] Silinecek dosyalar listesi silme kartında
- [ ] .irisignore korumalı dosyalar greyed out, silinemez
- [ ] write_file .irisignore kontrolü

### 1.12 Project Memory
- [ ] .iris/ dizini otomatik oluştur
- [ ] memory.md — agent her session başında okur, günceller
- [ ] rules.md — proje bazlı kurallar
- [ ] sessions/ — session summary otomatik kaydı (markdown)
- [ ] Global rules → Settings editörü
- [ ] Kural önceliği: project > global

### 1.13 Voice Input
- [ ] Input bar'da mikrofon ikonu
- [ ] Push-to-talk (basılı tut → kayıt, bırak → gönder)
- [ ] Whisper API entegrasyonu
- [ ] Transcript → input field'a düşer
- [ ] Auto-send seçeneği (Settings)

### 1.14 Session Features
- [ ] Session export (markdown format)
- [ ] Session search (SQLite FTS5)
- [ ] Session search UI

### 1.15 Notifications & Background
- [ ] Foreground service (arka plan agent)
- [ ] "Iris finished — X files changed" bildirimi
- [ ] Approve / Reject notification action'ları

### 1.16 Haptic & Keyboard
- [ ] Approve → success haptic
- [ ] Reject → error haptic
- [ ] Agent done → light haptic
- [ ] Queue added → light tick
- [ ] Send → minimal tick
- [ ] Cmd+Enter → gönder
- [ ] Cmd+K → command palette
- [ ] Escape → stop agent

### 1.17 Polish
- [ ] Renk şeması güncelle: Near-black + Gold (#0C0C0C, #E8C547)
- [ ] README.md yaz
- [ ] App icon final (Stylized "I" + cursor concept)
- [ ] Lottie animasyonu güncelle (geometric eye, gold iris)
- [ ] Empty state'ler: Projects, Session List, Chat
- [ ] Boş Chat: suggestion chips ("Fix a bug", "Explain codebase", "Write tests")

---

## Phase 2 — Multi-Provider (v1.1)

- [ ] `LLMProvider` interface (abstraction katmanı)
- [ ] Anthropic Claude REST entegrasyonu
- [ ] OpenAI GPT REST entegrasyonu
- [ ] Model seçim UI güncelle (provider + model)
- [ ] Session geçmişi ekranı iyileştirmeleri
- [ ] Hata + retry mekanizması iyileştirmeleri
- [ ] Light theme

---

## Phase 3 — Git + MCP + IPC (v1.2)

- [ ] `/git` slash command → Git bottom sheet
  - [ ] Status (değişen dosyalar)
  - [ ] Stage / Unstage
  - [ ] Commit (mesaj gir)
  - [ ] Push / Pull
  - [ ] Branch listesi + oluştur + değiştir
  - [ ] Basit log görünümü
- [ ] MCP HTTP/SSE client implementasyonu
- [ ] MCP server yönetimi (Settings)
- [ ] Agent MCP tool otomatik kullanımı
- [ ] Unix socket IPC server
- [ ] Terminal slash commands: `/open_settings`, `/open_diff` vb.

---

## Phase 4 — Power Features (v2.0)

- [ ] Ollama local model desteği
- [ ] SSH remote proje bağlantısı
- [ ] Theme Store
- [ ] WearOS companion app
- [ ] F-Droid repo kurulumu (Play Store sonra)

---

## Teknik Notlar

- PTY en riskli parça — termux/termux-app referans al, spike branch'te dene
- Gemini model string → `Constants.kt`'de tut
- API key encryption → `androidx.security:security-crypto`
- GenAI SDK kullanılmıyor — direkt REST + OkHttp + SSE
- Renk şeması henüz eski (Dark Purple) — 1.17 polish'te güncelle
- Compaction edge case: session sırasında yeni mesaj gelirse ne olur?
- Notification Approve/Reject: write_file only mu, ask_user da mı? (TBD)