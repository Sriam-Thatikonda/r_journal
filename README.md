**R-Journal** — Your Personal Daily Journal (Android, Jetpack Compose)

R-Journal is a modern journaling app built entirely with Jetpack Compose, designed for fast writing, clean UI, local privacy, and native Android features.

It includes a WhatsApp-style chat interface, mood tracking, reply system, image attachments, and complete offline storage using Room Database.

🚀 Features
📝 Chat-Style Journal Entries

Each day is a self-contained chat thread.
Write entries like conversations — simpler, faster, more natural.

📎 Send Messages with:

Text

Images (Camera / Gallery)

Mixed content

Automatic compression & private storage

💬 Swipe-to-Reply (WhatsApp style)

Swipe any message right to reply

Shows reply preview above the input box

Reply metadata is saved in Room

Replies remain after restarting the app

🔗 Tap Reply → Scroll to Original

Tap on reply preview inside a bubble

Auto-scrolls to the original message

Message briefly highlights with a border

Smooth animations with Compose

😊 Mood Picker

Select up to 3 moods for the day

Emoji-based UI

Animated scale bounce effect

Mood syncs with entry tags

🖼️ Full Image Viewer

Tap on any image

Opens full-screen image viewer

Local-only (no internet required)

🔒 Secure Local Storage

All data saved in Room DB

Custom JSON Converters preserve reply metadata

Private image storage under Android/data/.../files/Pictures

🌙 Smart Day Detection

Messages added after midnight are marked “Added later”

You can navigate to past entries without breaking rules

Editing & deleting allowed only for today’s messages

✅ Advanced Task Management

Swipe-to-complete or delete tasks with fluid animations.

Filter tasks by status (Active, Completed, Overdue, Today).

Sort tasks by Due Date, Priority, Created Date, or Alphabetically.

Real-time statistics summary (Active, Completed Today, Overdue).

🌱 Habit Tracker

7-Day heatmap grid view for quick tracking.

Context menu to quickly edit or delete habits.

Compact habit tracker widget for immediate toggling.

Tracks yearly overview and daily completion statuses.

📚 Journal Archive

Beautiful grid view of past entries.

Summarized cards displaying mood emojis, message count, and image count.

Event indicators (Birthdays 🎉, Anniversaries 💍) directly on the cards.

Custom thematic backgrounds based on the active App Theme.

🧱 Architecture
app/

│

├── data/

│   ├── local/

│   │   ├── entity/

│   │   │   ├── ChatMessage.kt

│   │   │   └── JournalEntry.kt

│   │   ├── converters/Converters.kt

│   │   └── JournalDatabase.kt

│   └── repository/JournalRepository.kt

|

├── ui/

│   ├── screens/ChatInputScreen.kt

│   ├── screens/ImageViewerScreen.kt

│   ├── components/ChatBubble.kt

│   └── components/CompactMoodPicker.kt

│

└── viewmodel/JournalViewModel.kt


Core Technologies

Kotlin

Jetpack Compose (Material3)

Room Database

Compose Navigation

Coil (Image loading)

SwipeToDismiss (Material 1 inside M3 UI)

Coroutines + StateFlow

FileProvider for image access

🏛️ Data Model
ChatMessage.kt
```kotlin
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val replyToMessageId: String? = null,
    val replyPreview: String? = null
)```    
```
JournalEntry.kt

One entry per day.

Stores a list of messages + mood tags.

💾 Room Storage
✔️ Custom JSON Converters

Your updated Converters.kt preserves all ChatMessage fields, including:

replyToMessageId

replyPreview

imageUri

This ensures reply chains survive app restarts.

🧠 ViewModel Logic (JournalViewModel)

Main responsibilities:

Load today’s entry

Load past entries

Add messages with/without images

Swipe-to-reply integration

Highlight target message on quote tap

Edit & delete rules only for today

Mood picker logic

Auto-sorting messages before saving

It exposes UI-ready state via:

- currentEntry

- isLoading: StateFlow<Boolean>

- isMessageAddedLater(message)

- isCurrentEntryToday

- canEditMood

🖌️ UI Design
✔️ Modern Material 3

Rounded bubbles

Soft shadows

Smooth animations

✔️ WhatsApp-like Interaction Model

Swipe to reply

Tap reply preview → auto-scroll

Animated highlight

Long-press → Edit/Delete

Fade-in message animations

✔️ Optimized Layout

LazyColumn with stable keys

Auto-scroll to bottom on new message

Handles image height, full-width text wrapping

📸 Images & Media

Images are:

Compressed on save (max dimension = 1024px)

Stored privately

Previewed inline

Openable in full screen

🔁 Reply System (How It Works)
When swiping a message:
```kotlin
replyToMessage = message
```
When sending a message:
```kotlin
viewModel.addMessageWithImage(
    text,
    imageUri?.toString(),
    replyTo = replyToMessage
)
```
ViewModel stores:

- replyToMessageId

- replyPreview

UI displays:

A preview bubble above input box

A quoted bubble inside messages

Scroll-to-original on tap

👇 Highlight Logic

When a reply quote is tapped:
```kotlin
highlightedMessageId = originalMessage.id
delay(1500)
highlightedMessageId = null
```

Then:
```kotlin
isHighlighted = (message.id == highlightedMessageId)
```

In ChatBubble, border changes automatically.

📦 Build & Run
1. Clone repo
git clone https://github.com/yourname/r_journal.git

2. Open in Android Studio Flamingo+/Koala+
3. Build + Run on device/emulator

Minimum SDK recommended: 26+

🧪 Testing Checklist

Text message sending

Attach from gallery

Take photo

Swipe-to-reply

Reply preview

Scroll to original

Highlight disappears after timeout

Past entries lock editing

Mood picker selection limit (3)

Saved entry persists after relaunch

Image viewer opens correctly

🌐 Optional: Server Sync (If enabled)

Auto-merge today's entry from your local Flask server

Sends updated messages on save

Handles offline mode gracefully

📌 Next Planned Features

Dark Mode (Nothing Phone 2 optimized)

Export entry as PDF

Daily reminders

Emoji reactions

Cloud sync (optional toggle)

❤️ Credits

Developed by Ram Thatikonda
Built for fast, secure, personal journaling — powered by Kotlin + Compose.
