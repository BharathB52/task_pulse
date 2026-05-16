# TaskPulse 🚀

TaskPulse is a modern, responsive Android task management application built with Kotlin. It helps users organize their daily routines with features like task scheduling, priority management, and real-time reminders.

## ✨ Features

- **Task Management**: Create, edit, and delete tasks with ease.
- **Categorization**: Group tasks into categories like Work, Study, or Personal.
- **Prioritization**: Set High, Medium, or Low priorities to stay focused on what matters.
- **Smart Filtering**: Filter tasks by status (Completed/Pending) or category.
- **Search**: Quickly find specific tasks using the real-time search bar.
- **Deadlines & Reminders**: Set specific dates and times for tasks with integrated system notifications.
- **Persistent Storage**: All data is saved locally using a Room database.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **Annotation Processing**: KSP (Kotlin Symbol Processing)
- **Asynchronous Logic**: Coroutines & Flow
- **UI Components**: Material Design, ViewBinding, RecyclerView
- **Notifications**: AlarmManager & BroadcastReceiver

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17+
- Android SDK 35 (Compile SDK)

### Installation
1. Clone the repository or download the source code.
2. Open the project in **Android Studio**.
3. Wait for the Gradle sync to complete.

### Running the App
1. Connect an Android device or start an emulator.
2. Click the **Run** button in Android Studio or press `Shift + F10`.
3. Alternatively, run via command line:
   ```powershell
   .\gradlew installDebug
   ```

## 📂 Project Structure

- `com.example.task_pulse.model`: Data entities (Room).
- `com.example.task_pulse.database`: Room Database, DAO, and Repository.
- `com.example.task_pulse.viewmodel`: ViewModel for UI state management.
- `com.example.task_pulse.ui`: Activities and Adapters.
- `com.example.task_pulse.utils`: BroadcastReceivers and Helper classes.

## 📄 License
This project is for educational purposes.
