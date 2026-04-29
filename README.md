# Finance Management System

A personal finance tracking Android application built with Kotlin and Jetpack Compose. The app is designed to help users manage their income, expenses, and budgets with both local and cloud-backed storage.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Kotlin** | 2.0.0 | Primary language |
| **Jetpack Compose + Material3** | BOM 2025.08.00 | Declarative UI |
| **Navigation Compose** | 2.9.3 | In-app navigation |
| **ViewModel + Lifecycle** | 2.9.2 | MVVM architecture |
| **Room** | 2.7.2 | Local SQLite database |
| **DataStore Preferences** | 1.1.7 | Local key-value storage |
| **Firebase Auth** | (BOM 34.4.0) | User authentication |
| **Firebase Firestore** | (BOM 34.4.0) | Cloud data sync |
| **Kotlin Coroutines** | 1.9.0 | Async operations |
| **KSP** | 2.0.0-1.0.21 | Kotlin Symbol Processing (Room codegen) |
| **Android Gradle Plugin** | 8.9.1 | Build tooling |

---

## Architecture

The project follows **MVVM (Model-View-ViewModel)** architecture:

- **UI Layer** — Jetpack Compose screens and components
- **ViewModel Layer** — State holders and business logic per screen
- **Data Layer** — Dual storage strategy:
  - **Room** for offline-first local persistence
  - **Firebase Firestore** for cloud synchronization
- **Authentication** — Firebase Auth for user identity

---

## Requirements

- Android Studio Meerkat or newer
- Android device or emulator running **Android 8.0+ (API 26)**
- A Firebase project with **Authentication** and **Firestore** enabled

---

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/Nadil-Dulran/finance-management-system.git
   cd finance-management-system
   ```

2. **Set up Firebase**
   - Create a project at [Firebase Console](https://console.firebase.google.com/)
   - Enable **Email/Password** authentication
   - Enable **Cloud Firestore**
   - Download `google-services.json` and place it in the `app/` directory

3. **Open in Android Studio**
   - Open the project root folder
   - Let Gradle sync complete

4. **Run the app**
   - Select a device or emulator (API 26+)
   - Click **Run ▶**

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
└── java/com/example/finance_management_system/
    ├── MainActivity.kt          # App entry point
    └── ui/theme/
        ├── Color.kt             # Custom color palette
        ├── Theme.kt             # Light/dark MaterialTheme
        ├── Type.kt              # Typography
        └── Shape.kt             # Shape definitions
```

---

## License

This project is for personal and educational use.
