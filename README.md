# 📱 ScrollOrStudy

**ScrollOrStudy** is a smart Android productivity ecosystem designed to help students track their study time versus scrolling habits, stay focused, and build consistent learning routines powered by advanced AI integrations.

---

## 🚀 Features Implemented 

### 🧠 Real-Time App Detection
* Detects which application the user is actively engaging with.
* Uses an Android **Accessibility Service** for continuous, low-latency background monitoring.

### ⏱️ Time Tracking System
* Tracks **Scroll Time** (social media apps like Instagram, TikTok).
* Tracks **Study Time** (educational apps like Google Classroom, GitHub, or manual focus mode).
* Timer counts accurately even during app-switching, continuously synced to Firebase.

### 📱 App Categorization System
* Apps are classified into three core behavior categories:  
  * **Distracting Apps:** Actively increments *Scroll Time*.
  * **Study Apps:** Actively increments *Study Time*.  
  * **Neutral Apps:** Ignored to prevent false positives.

### 🚨 Smart AI Alert System & Motivation Engine
* Triggers a real-time overlay notification popup when distraction thresholds are exceeded.
* **Powered by Google Gemini AI:** Instead of generic quotes, the app fetches dynamically generated, highly personalized motivational advice based on the user's exact daily study-to-scroll ratio.

### 📊 Professional Developer Dashboard
* A clean, modern UI featuring today’s live statistics:
  * 📚 Study Time  
  * 📱 Scroll Time
  * 🔥 Live Streak Counter  
* Features a unified "Start/Stop Study Session" architecture for manual focus overriding.

### 🏆 Mathematical Focus Leaderboard
* Processes historical user data through a custom algorithm [(Study / Total Time) * Streak](cci:1://file:///c:/Users/sailo/AndroidStudioProjects/ScrollOrStudy2/app/src/main/java/com/example/scrollorstudy/AppAccessibilityService.kt:64:12-106:13).
* Assigns dynamic, competitive global ranks (Bronze, Silver, Gold, Diamond, Grandmaster) displayed prominently on the student dashboard.

### 🤖 AI Binge Predictor (Python Engine)
* A remote Google Colab data engine analyzes historical usage timestamps to mathematically predict upcoming distraction windows.
* Delivers preemptive warnings directly to the Android client (e.g., "You usually scroll at 8 PM. Start a study session now.").

### 👨‍👩‍👧 Dedicated Parent Dashboard & Reports
* Separate login infrastructure specifically for Parents, securely linked to Student accounts via unique authorization keys.
* **Automated Data Visualization:** Integrates with the QuickChart API to automatically generate and display weekly visual bar charts comparing Study vs. Scroll habits over the last 7 days.

### ☁️ Cloud Architecture (Firebase Integration)
* **Firebase Realtime Database** fully connected.
* Daily Study and Scroll matrices are synced to the cloud seamlessly.
* Highly structured JSON tree separating `user_data`, `user_stats`, `ai_insights`, and `leaderboard` nodes.

---

## 🧩 How It Works (System Flow)
1. The Android client actively monitors the foreground application state via the Accessibility Service.
2. The logic matches the active app against predefined categories, updating local state variables.
3. Every 10 seconds, state vectors (Study Time, Scroll Time) are synchronized to the Firebase Realtime Database.
4. A remote Python Cloud Engine (Colab) digests this data to trigger Gemini AI prompts and calculate mathematical rankings.
5. AI insights, weekly chart URIs, and leaderboard ranks are streamed back to the Android client in real-time, instantly updating the Jetpack Compose UI.

---

## 🛠️ Tech Stack
* **Language:** Kotlin (Android), Python (AI Engine)
* **UI Framework:** Jetpack Compose (Material 3)
* **Platform:** Android (Android Studio)  
* **Core APIs:** Accessibility Service, Foreground Service, Intent Handling
* **Cloud Solutions:** Firebase Realtime Database, Firebase Authentication
* **Third-Party APIs:** Google Gemini LLM API, QuickChart.io API

---

## 🎯 Project Goal
* Reduce unconscious social media consumption.
* Increase structured, focused study blocks.
* Build healthy, consistent learning habits through advanced gamification and AI-driven accountability.

---

## 💡 Inspiration
Inspired by digital wellbeing tools and the necessity for a truly uncompromising, intelligent productivity ecosystem for students struggling to maintain focus in the modern attention economy.

---

## 👨‍💻 Author
*k
