# R-Journal — Personal Daily Journal & Life Suite (Android, Jetpack Compose)

**R-Journal** is a modern, privacy-focused personal journal and productivity suite built entirely with **Jetpack Compose**, **Kotlin**, and **Android Room Database**. Designed with local-first privacy, fluid micro-animations, and a WhatsApp-inspired messaging interface, R-Journal helps you log thoughts, track habits, manage tasks, monitor life challenges, track cravings, manage events, and securely store credentials.

---

## 🌟 Comprehensive Feature Set

### 📝 Chat-Style Daily Journal
* **Conversational Interface**: Log daily entries like conversations—simpler, faster, and more intuitive.
* **Rich Attachments**: Support for text, gallery images, camera photos, and voice notes.
* **Swipe-to-Reply**: Swipe any message right to reply with inline quote previews.
* **Tap-to-Scroll & Highlight**: Tap a reply preview to smoothly auto-scroll back to the original message with animated highlight borders.
* **Voice Notes**: Record and play back audio notes with inline duration tracking.
* **Smart Day Detection**: Messages added retroactively or past midnight are tagged cleanly while preserving historical data integrity.
* **Calendar & Search**: Calendar view for past journals and app-wide search across journal messages.

### 🌟 "Mine" Isolated Thoughts Stream
* **Distraction-Free Stream**: A dedicated, date-independent persistent stream (`MineScreen`) for instant notes, voice memos, and quick thoughts.
* **WhatsApp-Style Date Dividers**: Dynamic date headers separate entries chronologically.
* **Complete Isolation**: Mine entries are kept separate from daily archive logs while remaining fully supported during backup, export, and PDF generation.

### 📌 Quick Notes Manager
* **Categorized Notes**: Create, edit, and organize standalone markdown-supported quick notes.
* **Pin & Archive**: Pin important notes to top and archive completed notes.

### 🏆 Challenge Tracker & Statistics
* **Goal Challenges**: Set multi-day habits and challenges with custom icons and daily targets.
* **Summary Dashboard**: Overview bar tracking active challenges, completed today count, and average progress percentage.
* **Detailed Performance Analytics**: Track completion gauge (`%`), days completed vs remaining, last activity date, and daily reminder schedules.
* **Challenge History**: Archive completed or abandoned challenges for long-term review.

### 📊 Life Trackers & Counter History
* **Custom Metric Counters**: Track water intake, pushups, books read, or custom goals with configurable increment steps.
* **Scheduled Resets**: Support for Daily, Weekly, or manual reset frequencies.
* **History Logs & Metrics**: Automatically logs previous period totals into history logs for trend analysis.

### 🎯 Craving Quest (Impulse Control)
* **Urge Logging**: Track cravings, triggers, and intensity ratings to build impulse control.
* **Streak & Milestones**: Monitor abstinence streaks and review craving history logs over time.

### 📅 Events & Countdowns
* **Event Tracker**: Track birthdays, anniversaries, and key milestones with countdown timers.
* **Archive Displays**: Event indicators displayed directly on daily journal archive cards.

### 🔐 Password Manager & Security Vault
* **Hardware Encryption**: Passwords encrypted using Android KeyStore AES (`AES/GCM/NoPadding`).
* **Password Generator**: Built-in strong passphrase generator with custom character set options.
* **Portable Export / Import**: Safely export standalone password backups with automatic key normalization on restore.
* **Biometric Guard**: Lock the application using Android Biometric Prompt (Fingerprint / Face Unlock).

### ✅ Advanced Task Management
* **Task Lists & Categories**: Organize tasks into color-coded categories with priorities (High, Medium, Low).
* **Filtering & Sorting**: Filter active, completed, overdue, or today's tasks; sort by due date, priority, or title.
* **Home Screen Widget**: Dedicated Android widget to manage tasks directly from your home screen.

### 🌱 Habit Tracker & Year in Pixels
* **Habit Heatmap**: 7-Day grid heatmap view for quick habit checking.
* **Year in Pixels**: Visual annual mood and habit tracking grid for yearly reflection.

### 💬 Daily Quotes & Widgets
* **Daily Inspiration**: Curated motivational quotes refreshed daily.
* **Home Screen Quote Widget**: Configurable Glance app widget for daily quotes.

### 📦 Data Portability & PDF Export
* **Full ZIP Backup / Restore**: Export & import all app data (Journal entries, Mine stream, tasks, habits, passwords, trackers, challenges, craving logs, events, quotes, images, and voice notes).
* **PDF Export**: Generate formatted PDF documents including daily journals, mood logs, and the Mine stream.
* **Local Database Backup**: Quick SQLite database snapshot backup and restoration.

---

## 🏛️ Architecture & Tech Stack

```text
d:\r_journal\app\src\main\java\com\baverika\r_journal\
├── data/
│   ├── local/
│   │   ├── dao/           # Room Data Access Objects
│   │   ├── database/      # JournalDatabase configuration
│   │   ├── entity/        # Room Database Entities
│   │   └── security/      # KeyStore & Security utilities
│   └── repository/        # Data Repositories
├── quotes/                # Motivational Quotes module & Glance Widget
├── ui/
│   ├── challenge/         # Challenge Tracker screens & ViewModels
│   ├── components/        # Reusable UI components & ChatBubbles
│   ├── screens/           # Jetpack Compose Screens
│   ├── theme/             # Material3 Theme & Color tokens
│   └── viewmodel/         # Android ViewModels
├── utils/                 # Export, Import, PDF, and Security Utilities
├── widget/                # Home Screen Widgets (Task, Habit, Tracker Providers)
└── MainActivity.kt        # App Entry Point & Navigation Host
```

### Core Technologies
* **Language**: Kotlin 1.9+
* **UI Framework**: Jetpack Compose with Material 3
* **Database**: Room Database (SQLite) with custom JSON type converters
* **Architecture**: MVVM with Repository Pattern, StateFlow & Coroutines
* **Image Loading**: Coil
* **Security**: Android KeyStore System (AES/GCM) & Biometric Prompt API
* **Exporting**: Gson & Android PdfDocument API

---

## 💾 Security & Privacy

1. **100% Local Storage**: All data, images, voice notes, and credentials reside strictly on device.
2. **Encrypted Passwords**: Passwords stored in Room DB are encrypted using AES/GCM keys stored in the hardware-backed Android KeyStore.
3. **Biometric Guard**: Biometric authentication restricts access to sensitive screens and app startup.

---

## 🛠️ Build & Setup

### Prerequisites
* Android Studio (Koala / Ladybug or newer recommended)
* JDK 17+
* Android SDK (API Level 26 / Android 8.0 Minimum)

### Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/SSRam1919/r_journal.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle project files.
4. Run on a physical Android device or emulator (`API 26+`).

---

## 📄 License & Credits

Developed by **Ram Thatikonda**. Built for fast, secure, personal daily journaling—powered by Kotlin & Jetpack Compose.
