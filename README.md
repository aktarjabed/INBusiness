# INBusiness - Modern Android Business Management App

INBusiness is a comprehensive, production-ready Android application designed for the Indian business market. It provides invoicing, payment processing, and subscription management features with a focus on security and offline capability.

## 🚀 Key Features

### 🔐 Robust Authentication
*   **Google Sign-In**: Seamless one-tap login using Credential Manager.
*   **Phone OTP**: Secure verification via Firebase Auth.
*   **Email/Password**: Standard authentication flow.
*   **Microsoft Sign-In**: Enterprise integration support.

### 💳 Indian Payment Suite
*   **Razorpay Integration**: Full support for UPI, Credit/Debit Cards, Net Banking, Wallets, and EMI.
*   **Subscription Management**: Automated handling of Basic, Pro, and Enterprise plans.

### 🧠 Device-Aware Freemium
*   **Auto-Detection**: Classifies devices (Low/Mid/High Tier) based on hardware specs.
*   **Quota System**: Enforces usage limits for free tier users based on device capabilities.

### 🛡️ Bank-Grade Security
*   **SQLCipher**: Full database encryption.
*   **Android Keystore**: Secure hardware-backed key storage.
*   **Network Security**: Strict HTTPS enforcement and certificate pinning support.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM + Clean Architecture
*   **DI**: Dagger Hilt
*   **Async**: Coroutines & Flow
*   **Local Data**: Room Database (Encrypted) + DataStore
*   **Remote Data**: Retrofit + Firebase (Auth, Remote Config, Crashlytics)
*   **Payments**: Razorpay SDK

## 🏗️ Building the Project

1.  Clone the repository.
2.  Add your `google-services.json` to the `app/` directory.
3.  Configure your API keys in Firebase Remote Config or `local.properties` (for development).
4.  Build the project:
    ```bash
    ./gradlew assembleDebug
    ```

## 📱 Screenshots

*(Add screenshots here)*

## 📄 License

Copyright 2025 INBusiness. All rights reserved.