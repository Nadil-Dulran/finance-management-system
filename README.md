# FlowLedger

FlowLedger is an Android personal finance app built with Kotlin and Jetpack Compose. It helps users track income and expenses, manage savings goals, review transaction history, and keep data available locally with optional Firebase-backed sync.

## Features

- Email/password authentication with Firebase
- Add, edit, and delete income and expense records
- Recurring bill tracking
- Savings goal management with monthly contribution planning
- Dashboard with finance summaries and charts
- Recent transaction history and insights
- Preferred display currency support
- Notification-based bank alert detection
- Offline-first local storage with sync support
- In-app finance assistant for quick money insights

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Hilt
- Room
- DataStore
- WorkManager
- Firebase Authentication
- Firebase Firestore

## Requirements

- Android Studio
- JDK 17
- Android 8.0+ (API 26+)
- Firebase project with:
  - Authentication enabled
  - Email/Password sign-in enabled
  - Cloud Firestore enabled

## Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/Nadil-Dulran/finance-management-system.git
   cd finance-management-system
Add your Firebase config file:

Download google-services.json from Firebase
Place it in:
app/google-services.json
Open the project in Android Studio and allow Gradle sync to finish.

Run the app from Android Studio, or build from terminal:

./gradlew :app:assembleDebug
Project Structure
app/src/main/java/com/example/finance_management_system/
├── assistant/
├── data/
├── di/
├── model/
├── navigation/
├── repository/
├── ui/
└── viewmodel/
Notes
The app uses Room for local persistence.
Firebase is used for authentication and cloud sync.
Notification access is optional and used for detecting bank transaction alerts.
