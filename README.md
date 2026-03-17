# 📱 ScrollOrStudy

**ScrollOrStudy** is a smart Android productivity app designed to help students track their study time versus scrolling habits, stay focused, and build consistent learning routines.  

---

## 🚀 Features Implemented (Completed)

### 🧠 Real-Time App Detection
* Detects which app the user is actively using  
* Uses Accessibility Service for continuous background monitoring  

### ⏱️ Time Tracking System
* Tracks **Scroll Time** (social media apps like Instagram)  
* Tracks **Study Time** (educational apps like Google Classroom, GitHub, or manual study)  
* Timer counts accurately even if the user switches apps  
* Verified live via Logcat and in-app UI  

### 📱 App Categorization System
* Apps classified into three categories:  
  * **Distracting Apps:** counted as *Scroll Time*  
  * **Study Apps:** counted as *Study Time*  
  * **Neutral Apps:** ignored in tracking  

### 🚨 Smart Alert System
* Shows a notification popup after 15 seconds of continuous scrolling  
* Encourages users to return to study mode  
* Works in real-time without crashing  

### 📊 Daily Tracking Dashboard
* Clean UI showing today’s stats:
  * 📚 Study Time  
  * 📱 Scroll Time  
* Counts match logcat times  
* UI includes “Welcome”, “Stay Focused”, live date, and **Start Study** button  

### 🔥 Streak System
* Tracks daily study consistency  
* Displays streak count in the UI  

### ☁️ Firebase Integration (Basic)
* Firebase Realtime Database connected  
* Daily Study and Scroll data synced to the cloud  
* Data stored under `user_data/<date>` structure  
* Live sync works every 10 seconds  
* Day-end sync implemented for previous day  
* Database rules currently in **test mode**  

---

## 🧩 How It Works (Current Implementation)
1. App continuously monitors foreground app using Accessibility Service  
2. Matches the app against defined categories:  
   * Distracting → increments Scroll Time  
   * Study → increments Study Time  
   * Neutral → ignored  
3. Timer updates every second  
4. Alerts trigger when distraction exceeds 15 seconds  
5. Data is stored locally and synced to Firebase in real-time  
6. UI displays live stats and streak  

---

## 🛠️ Tech Stack
* **Language:** Kotlin  
* **Platform:** Android (Android Studio)  
* **Core APIs:** Accessibility Service, Foreground Service, Handlers  
* **Storage:** SharedPreferences (local), Firebase Realtime Database (cloud)  

---

## 📌 Current Status
✅ Core app tracking system completed  
✅ Real-time app detection implemented  
✅ Study and scroll timers working perfectly  
✅ Alerts functioning after 15 seconds of scrolling  
✅ UI dashboard implemented (Welcome, Stats, Streak, Start Study button)  
✅ Firebase Realtime Database connected and syncing data  
🔄 Streak system partially implemented (UI shows streak, logic partially done)  

---

## 🚀 Upcoming Features
* 🎯 Motivation Engine – daily quotes, fun reminders, encouragement messages  
* 🔥 Advanced Streak System – automated daily reset, long-term tracking  
* 👨‍👩‍👧 Parent Dashboard – monitor student study habits  
* 🤖 AI-based insights using Google Colab  
* 📊 Weekly & Monthly Reports – visualize study vs scrolling patterns  

---

## 🎯 Project Goal
* Reduce unnecessary scrolling  
* Increase focused study time  
* Build healthy, consistent learning habits  

---

## 💡 Inspiration
Inspired by digital wellbeing tools and productivity apps for students struggling to maintain focus.  

---

## 👨‍💻 Author
**Kavya Sahithi Balusapati**
