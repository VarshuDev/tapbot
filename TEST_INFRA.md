# E2E Test Infra: TapBot 20 Flagship Bots Expansion

## Test Philosophy
- **Opaque-box & Requirement-driven**: Derived directly from `ORIGINAL_REQUEST.md` and user-facing specifications, with zero coupling to private internal implementation details.
- **Zero-External-Network Dependency**: 100% of test suites execute deterministically offline using simulated Telegram network doubles (`FakeTelegramApiClient`), local `.botpkg` asset verification, and in-memory databases.
- **Methodology**: Systematic 4-tier testing hierarchy combining Category-Partition (Tier 1), Boundary Value Analysis & Negative Testing (Tier 2), Pairwise Combinatorial Testing (Tier 3), and Real-World Mobile Workload Testing (Tier 4).

## Feature Inventory
| # | Feature | Source (requirement) | Tier 1 | Tier 2 | Tier 3 |
|---|---------|---------------------|:------:|:------:|:------:|
| F1 | 20 Flagship Bot Packaging (`tools/package_all_bots.mjs`) | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| F2 | Offline Mock Catalog & Asset Verification (`MockCatalogApi.kt`) | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| F3 | Cloudflare D1 Sync Migration (`0012_twenty_flagship_bots.sql`) | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| F4 | DailyLifestyleBotRuntime (`bot_habit_forge`, `bot_remind_me`, `bot_fit_track`) | ORIGINAL_REQUEST §R1, §R3 | 5 | 5 | ✓ |
| F5 | PersonalFinanceBotRuntime (`bot_penny_wise`, `bot_coin_radar`) | ORIGINAL_REQUEST §R1, §R3 | 5 | 5 | ✓ |
| F6 | ProductivityBotRuntime (`bot_pomo_focus`, `bot_tap_note`) | ORIGINAL_REQUEST §R1, §R3 | 5 | 5 | ✓ |
| F7 | DailyUtilityBotRuntime (`bot_sky_pulse`, `bot_link_drop`, `bot_poly_glot`, etc.) | ORIGINAL_REQUEST §R1, §R3 | 5 | 5 | ✓ |
| F8 | DeveloperBotRuntime (`bot_git_notifier`) | ORIGINAL_REQUEST §R1, §R3 | 5 | 5 | ✓ |
| F9 | BotRuntimeFactory Router & Alias Precedence | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| F10 | TapBotTheme 20 Visual Identities | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F11 | Enhanced Material Typography (`tnum`, tabular numerals) | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F12 | BotCommandCatalog 20 Flagship Commands | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F13 | Play Store Curated Shelves & Search | ORIGINAL_REQUEST §R5 | 5 | 5 | ✓ |
| F14 | Play Store Detail Header & Command Cheatsheet | ORIGINAL_REQUEST §R2, §R5 | 5 | 5 | ✓ |
| F15 | Comprehensive Verification & Hardening | ORIGINAL_REQUEST §Acceptance | 5 | 5 | ✓ |

## Test Architecture
- **Test Runner Invocation**:
  - Full project test suite: `.\gradlew.bat testDebugUnitTest`
  - Network & Package archive verification: `.\gradlew.bat :core:network:testDebugUnitTest --tests "com.tapbot.core.network.BundledPackagesCatalogTest"`
  - UI, Command Catalog & Theme verification: `.\gradlew.bat :core:ui:testDebugUnitTest --tests "com.tapbot.core.ui.BotCommandCatalogTest"`
  - Runner, Factory & Domain runtime verification: `.\gradlew.bat :core:runner:testDebugUnitTest`
  - Catalog Screen & ViewModel verification: `.\gradlew.bat :feature:catalog:testDebugUnitTest`
  - Deterministic Package Generator: `node tools/package_all_bots.mjs`
  - Cloudflare D1 SQL Migration Syntax check: SQLite in-memory execution / AST check
- **Pass/Fail Semantics**:
  - Gradle test runner exits with code 0 on 100% test pass.
  - Assertions strictly enforce exact byte matching, SHA-256 matching, non-null visual palettes, correct domain runtime class mappings, and error resilience without crashes.
- **Directory Layout**:
  ```
  tapbot/
  ├── app/src/main/assets/packages/              # 20 deterministic .botpkg archives
  ├── backend/migrations/
  │   └── 0012_twenty_flagship_bots.sql          # Cloudflare D1 edge migration
  ├── core/network/src/test/java/.../network/
  │   └── BundledPackagesCatalogTest.kt          # Tier 1 & 2 Package and Catalog tests
  ├── core/ui/src/test/java/.../ui/
  │   └── BotCommandCatalogTest.kt               # Tier 1, 2, 3 Command and Theme tests
  ├── core/runner/src/test/java/.../runner/
  │   ├── BotRuntimeFactoryTest.kt               # Tier 1, 2, 3 Runtime routing and domain tests
  │   └── MultiBotBenchmarkTest.kt               # Tier 3 & 4 Multi-bot concurrency tests
  ├── feature/catalog/src/test/java/.../catalog/
  │   └── CatalogViewModelTest.kt                # Tier 1 & 2 Shelves and Search tests
  └── tools/
      └── package_all_bots.mjs                   # Deterministic packaging script
  ```

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Complexity |
|---|----------|--------------------|------------|
| 1 | The Executive Daily Kickoff & Habit Routine | F4, F6, F7, F10, F12 | High |
| 2 | Personal Wealth Management & Market Watchdog | F5, F7, F10, F12 | High |
| 3 | DevOps On-Call Incident Response & Fleet Recovery | F1, F8, F9, F12, F15 | High |
| 4 | Global Multilingual Community Guard & Engagement | F7, F9, F10, F12 | High |
| 5 | Offline Local-Only Commute & Hardware Sentinel | F2, F6, F7, F13, F15 | High |

## Coverage Thresholds
- **Tier 1 (Feature Coverage)**: 75 test cases (5 per feature across all 15 features / 20 bots). Minimum threshold: ≥5 × 15 = 75.
- **Tier 2 (Boundary & Corner Cases)**: 75 test cases (5 per feature across all 15 features). Minimum threshold: ≥5 × 15 = 75.
- **Tier 3 (Cross-Feature Combinations)**: 25 pairwise interaction test cases covering 6 major subsystem boundaries. Minimum threshold: ≥15.
- **Tier 4 (Real-World Application Scenarios)**: 5 realistic, multi-bot daily mobile user workflows. Minimum threshold: max(5, 15 ÷ 2) = 8 application scenarios or 5 rich composite multi-bot scenarios.
- **Total Test Cases Specified**: 180 test cases.
