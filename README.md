# 📱 Scroll or Study

A smart Android productivity app that helps students track their study time vs screen scrolling habits and build better focus.

---

## 🚀 Features Implemented

### 🧠 1. Real-Time App Detection

* Detects which app the user is currently using
* Uses Accessibility Service for continuous monitoring

---

### ⏱️ 2. Time Tracking System

* Tracks **Scroll Time** (social media usage)
* Tracks **Study Time** (educational apps + manual study)
* Updates time in real-time

---

### 📱 3. App Categorization System

Apps are categorized into:

* **Distracting Apps**

  * Instagram, YouTube, Facebook, etc.
  * Counted as *Scroll Time*

* **Study Apps**

  * Google Classroom, GitHub, learning platforms
  * Counted as *Study Time*

* **Neutral Apps**

  * Settings, system apps
  * Ignored (no tracking)

---

### 🚨 4. Smart Alert System

* Detects continuous scrolling
* Triggers alert after 15 seconds
* Encourages user to return to studying

---

### 📊 5. Daily Tracking Dashboard

* Displays:

  * 📚 Study Time
  * 📱 Scroll Time
* Clean and simple UI for user awareness

---

### 🎯 6. Study Mode Support

* Manual "Start Study" option
* Allows tracking even when not using study apps

---

### 🔥 7. Streak System (In Progress / Implemented)

* Tracks daily consistency
* Increases streak when:

  * Study Time > Scroll Time
* Resets streak when:

  * Scroll Time exceeds study time

---

## 🧩 How It Works

1. App continuously monitors foreground app
2. Matches app with category:

   * Distracting → Scroll Time
   * Study → Study Time
   * Neutral → Ignore
3. Timer updates every second
4. Data is stored locally
5. UI displays real-time stats
6. Alerts trigger when excessive scrolling is detected

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Platform:** Android (Android Studio)
* **Core APIs:** Accessibility Service, Handler
* **Storage:** SharedPreferences

---

## 📌 Current Status

✅ Core tracking system completed
✅ UI Dashboard implemented
✅ Alerts working
🔄 Streak system in progress

---

## 🚀 Upcoming Features

* 🎯 Motivation Engine (quotes, funny alerts)
* 🔥 Advanced Streak System (daily reset automation)
* ☁️ Firebase Integration (cloud sync)
* 👨‍👩‍👧 Parent Dashboard
* 🤖 AI-based insights using Google Colab
* 📊 Weekly & Monthly Reports

---

## 🎯 Project Goal

To help students:

* Reduce unnecessary scrolling
* Increase study focus
* Build consistent learning habits

---

## 💡 Inspiration

Inspired by digital wellbeing tools and student productivity challenges.

---

## 👨‍💻 Author
Kavya Sahithi Balusapati
