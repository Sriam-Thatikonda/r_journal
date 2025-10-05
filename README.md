**📓 R-Journal**

R-Journal is a modern journaling and quick notes application built with Jetpack Compose, Room Database, and Material 3. It provides a simple, elegant interface to record daily thoughts, manage quick notes, and explore insights with a dashboard.


**✨ Features**

📝 Journal Archive – Maintain daily journal entries with easy editing.

⚡ Quick Notes – Create and manage small notes instantly.

🔍 Search – Search across your journals and notes.

📊 Dashboard – Visual overview of your writing habits.

⬆️ Export – Export your journal and notes data.

⬇️ Import – Import existing data into the app.

🖼️ Image Viewer – View attached images in fullscreen mode.

🎨 Modern UI – Built using Material 3, Jetpack Compose, and Navigation.

💾 Local Storage – Data stored securely using Room Database.


**📱 Screens**

Journal Archive – List of all journal entries.

Chat Input Screen – Add/edit journal entries in a chat-like interface.

Quick Notes – Manage lightweight notes.

Search – Search journals and notes.

Dashboard – Visualize statistics of your writing.

Export/Import – Backup and restore your data.


**🛠️ Tech Stack**

Language: Kotlin

UI: Jetpack Compose, Material 3

Navigation: Navigation-Compose

Database: Room (SQLite)

Architecture: MVVM (ViewModel + Repository)

Coroutines: For async tasks


**🚀 Getting Started**
Prerequisites

Android Studio Ladybug (or newer)

JDK 17+

Gradle 8+

Setup

Clone this repo:

git clone https://github.com/your-username/r_journal.git


Open in Android Studio.

Sync Gradle & run the app on an emulator or device.

📂 Project Structure
app/

 ├─ src/main/java/com/baverika/r_journal/
 
 │   ├─ data/local/database/      # Room database
 
 │   ├─ repository/               # Repositories (Journal, QuickNotes)
 
 │   ├─ ui/screens/               # All UI Screens
 
 │   ├─ ui/viewmodel/             # ViewModels + Factories
 
 │   └─ MainActivity.kt           # App entry point
 
 │
 ├─ res/
 
 │   ├─ drawable/                 # Images & icons
 
 │   ├─ mipmap/                   # App launcher icons
 
 │   └─ values/                   # Themes, colors, strings
 
 │
 └─ AndroidManifest.xml
 

**📸 Screenshots**

Will update soon...


**🔮 Roadmap**

 Dark mode support

 Cloud backup & sync

 Widgets for quick journaling

 Rich text formatting


📜 License

This project is licensed under the MIT License – see the LICENSE
 file for details.
