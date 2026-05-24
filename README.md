# Finance Management System

An Android personal finance application built with Kotlin and Jetpack Compose for tracking income, expenses, goals, and account-level preferences with local persistence and Firebase-backed authentication.

## Overview

The app is designed around an offline-first finance flow:

- Users can create an account, sign in, and reset their password through Firebase Authentication
- Income and expense records are stored locally with Room
- Display preferences are stored with DataStore
- Selected user data can be synchronized with Firebase Firestore
- Dashboard charts and summaries give a quick view of current finance status
- Goal tracking supports monthly contributions and emergency withdrawals
- Notification parsing support exists for detecting bank transaction alerts
- Automated recurring transaction generation and proactive due reminders

## Features

### Authentication

- Email/password registration
- Email/password sign in
- Password reset email flow
- Session-aware profile handling

### Dashboard

- Summary cards for finance overview
- Expense chart
- Income chart
- Spending split chart
- Spend-vs-left status
- Recent transaction preview
- Featured goal summary

### Income and Expense Tracking

- Add income with source, amount, currency, and note
- Add expense with category, amount, currency, spending type, recurrence, payment method, and note
- Edit transactions
- Delete transactions
- Recurring expense refresh support

### Goal Management

- Add and update savings goals
- Track current saved amount and target amount
- Monthly contribution planning
- Contribution day of month support
- Emergency withdrawal support
- Progress and remaining amount tracking

### History and Detection

- Recent transaction history
- Detected bank-alert transaction queue
- Confirm detected transactions
- Ignore detected transactions
- Insight items from stored finance data

### Profile and Settings

- Preferred display currency
- Notification access shortcut
- Sign out flow

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.0.0 | Primary language |
| Jetpack Compose | BOM 2025.08.00 | UI toolkit |
| Material 3 | BOM 2025.08.00 | Compose design system |
| Navigation Compose | 2.9.3 | Screen navigation |
| Lifecycle + ViewModel | 2.9.2 | State and lifecycle-aware UI |
| Room | 2.7.2 | Local database |
| DataStore Preferences | 1.1.7 | User preferences |
| Firebase Auth | BOM 34.4.0 | Authentication |
| Firebase Firestore | BOM 34.4.0 | Cloud sync |
| Kotlin Coroutines | 1.9.0 | Async operations |
| KSP | 2.0.0-1.0.21 | Room code generation |
| Android Gradle Plugin | 8.9.1 | Build tooling |

## Architecture

The project follows an MVVM-style structure:

- `ui/`
  Compose screens, shared components, theming, and UI state models
- `viewmodel/`
  Screen-level state holders and action handling
- `repository/`
  Contracts and concrete local/Firebase-backed implementations
- `data/`
  Room database, DAOs, entities, preferences, notification parsing, sync services, and app container wiring
- `navigation/`
  App routes and navigation host
- `model/`
  Shared domain and presentation models

## Core Modules

### UI Screens

- `LoginScreen`
- `RegisterScreen`
- `ResetPasswordScreen`
- `DashboardScreen`
- `AddIncomeScreen`
- `AddExpenseScreen`
- `TransactionsScreen`
- `RecurringBillsScreen`
- `GoalScreen`
- `SettingsScreen`
- `ProfileScreen`

### Data and Domain

- `FinanceRepository` for income, expense, history, and detection features
- `GoalRepository` for goal tracking features
- `FirebaseAuthRepository` for auth flows
- `LocalFinanceRepository` and `LocalGoalRepository` for local persistence logic
- `FirestoreSyncService` for Firebase synchronization
- `BillReminderWorker` for background due date scanning

## Requirements

- Android Studio Meerkat or newer
- JDK 17
- Android device or emulator running Android 8.0+ (API 26+)
- A Firebase project with:
  - Authentication enabled
  - Email/Password provider enabled
  - Firestore enabled

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/Nadil-Dulran/finance-management-system.git
cd finance-management-system
```

### 2. Open the project

- Open the project root in Android Studio
- Allow Gradle sync to complete

### 3. Firebase configuration

Create or use an existing Firebase project, then:

1. Add an Android app with package name:
   `com.example.finance_management_system`
2. Enable `Authentication`
3. Inside Authentication, enable `Email/Password`
4. Enable `Cloud Firestore`
5. Download `google-services.json`
6. Place it in:

```text
app/google-services.json
```

### 4. Build and run

Use Android Studio Run, or:

```bash
./gradlew :app:assembleDebug
```

## APK Installation Guide

### Requirements

- Android `8.0 (API 26)` or above
- Internet connection for first-time sign-in and sync
- APK file provided by the project team

### Steps to Install

1. Transfer the APK to your Android device.
2. Open the APK file.
3. If Android blocks the installation, allow `Install unknown apps` for the app you are using to open the APK.
4. If `Play Protect` shows a warning, temporarily disable the scan:
   - Open `Google Play Store`
   - Tap your profile icon
   - Go to `Play Protect`
   - Tap the `Settings` icon
   - Turn off `Scan apps with Play Protect`
5. Return to the APK and continue the installation.
6. After the app is installed, open it and sign in / register.

### Why Play Protect May Show a Warning

This app is currently distributed as a sideloaded academic/demo APK instead of through the Google Play Store. It also includes a notification-listener feature used to detect bank transaction alerts from notifications. Because notification access is considered a sensitive Android capability, Play Protect may warn users before installation even when the app is intended for legitimate demo use.

In this project, notification access is used only for finance-related alert detection inside the app. Users may need to disable Play Protect temporarily during installation because Android treats sideloaded apps with notification-listener features more cautiously than apps installed directly from Google Play.

### Important Note

- If Play Protect was turned off for installation, it is recommended to turn it `back on` after the app has been installed.
- The app is intended for academic/demo use.

### First Launch

- Register a new account or log in with an existing account.
- Make sure internet is available for authentication and Firebase sync.
- After first sync, the app can continue working with local cached data.

## Password Reset Flow

The reset password feature uses Firebase Authentication email recovery.

1. User taps `Forgot Password?` on the login screen
2. App opens the reset-password screen
3. User enters their email
4. App calls `FirebaseAuth.sendPasswordResetEmail(...)`
5. Firebase sends a reset email to that account
6. User resets the password through the email link

Important notes:

- The actual password change is handled by Firebase, not inside the app UI
- Email/Password auth must be enabled in Firebase for this to work

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/finance_management_system/
│   ├── MainActivity.kt
│   ├── MyFinancialTrackerApplication.kt
│   ├── data/
│   │   ├── AppContainer.kt
│   │   ├── currency/
│   │   ├── local/
│   │   ├── notification/
│   │   ├── preferences/
│   │   ├── remote/
│   │   ├── session/
│   │   └── storage/
│   ├── model/
│   ├── navigation/
│   ├── repository/
│   │   ├── firebase/
│   │   └── local/
│   ├── ui/
│   │   ├── components/
│   │   ├── screens/
│   │   ├── state/
│   │   └── theme/
│   └── viewmodel/
└── res/
    ├── values/
    └── xml/
```

## Testing

This project is best tested using a mix of unit, UI, and integration-focused checks.

### Recommended testing types

- Functional testing
- Validation testing
- UI testing
- Persistence testing
- Firebase/auth integration testing
- Regression testing

### Suggested framework usage

- `JUnit`
  For unit testing business logic, validation rules, and ViewModel behavior
- `Compose UI Test`
  For testing Jetpack Compose screens and interactions
- `Espresso`
  For Android instrumentation support where needed, especially around activity launch and non-Compose interactions

### What should be tested

#### Authentication

- Register with valid data
- Register with invalid email
- Register with weak password
- Login with correct credentials
- Login with incorrect credentials
- Reset password with valid email
- Reset password with empty email
- Verify success and error messaging

#### Navigation

- Login to Register navigation
- Register back to Login navigation
- Login to Reset Password navigation
- Successful login to Dashboard navigation
- Bottom navigation behavior across major screens

#### Finance flows

- Add income
- Add expense
- Edit transaction
- Delete transaction
- Detected transaction confirmation and ignore flows

#### Goals

- Add goal
- Update goal
- Delete goal
- Emergency withdrawal
- Progress updates after changes

#### Settings and Profile

- Change preferred currency
- Verify UI reflects stored currency
- Open notification settings
- Sign out

#### Persistence

- Data remains after app restart
- Preferences remain after app restart

## Example Test Case Ideas

| ID | Scenario | Expected Result |
|---|---|---|
| TC01 | Register with valid email/password | Account created successfully |
| TC02 | Login with valid credentials | Dashboard opens |
| TC03 | Login with wrong password | Error message shown |
| TC04 | Reset password with registered email | Reset email sent successfully |
| TC05 | Add income with valid data | Income saved and shown in UI |
| TC06 | Add expense with valid data | Expense saved and shown in UI |
| TC07 | Edit transaction | Updated values appear in history |
| TC08 | Delete transaction | Transaction removed from history |
| TC09 | Add goal | Goal appears with correct progress |
| TC10 | Change preferred currency | Values update in supported screens |

## Known Setup Notes

- `google-services.json` must match the Android package name used by the app
- Firebase Authentication Email/Password provider must be enabled
- If Firebase is configured incorrectly, auth flows may show configuration or network-style errors even when the app builds correctly

## License

This project is intended for educational and academic use.
