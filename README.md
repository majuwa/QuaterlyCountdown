# 🕒 Quarterly Countdown (Wear OS)

[![CI](https://github.com/majuwa/QuaterlyCountdown/actions/workflows/ci.yml/badge.svg)](https://github.com/majuwa/QuaterlyCountdown/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/majuwa/QuaterlyCountdown)](https://github.com/majuwa/QuaterlyCountdown/releases/latest)
[![codecov](https://codecov.io/gh/majuwa/QuaterlyCountdown/graph/badge.svg)](https://codecov.io/gh/majuwa/QuaterlyCountdown)

A sleek, high-visibility countdown timer designed specifically for Android smartwatches. This app provides a **3-minute countdown** divided into four distinct visual quarters, perfect for HIIT training, public speaking, or productivity sprints.

---

## 🚀 Key Features

* **Fixed 3-Minute Timer:** No-fuss, one-tap start.
* **Visual Progress Ring:** A circular ring divided into four 90-degree segments.
* **Active Quarter Blinking:** The current segment pulses/blinks to give you an at-a-glance status of your progress.
* **Haptic Feedback:** Three short pulses on each quarter transition; three long buzzes when the countdown ends.
* **Runs When Screen Off:** A foreground service with a wake lock keeps the timer ticking accurately even when the watch display turns off.
* **Wear OS Optimized:** High-contrast UI designed for readability on small circular and square displays.

---

## 🛠 How It Works

The app divides the total duration of 180 seconds into four equal "Quarters." 

### The Math
The logic follows a simple division:
$$\text{Quarter Duration} = \frac{180\text{ seconds}}{4} = 45\text{ seconds}$$

### Gesture Reference

| Gesture | When | Action |
| :--- | :--- | :--- |
| **Tap** | IDLE or Paused | Start / Resume |
| **Tap** | Running | Pause |
| **Tap** | Finished | Reset to 03:00 |
| **Long Press** | Any (except IDLE) | Reset to 03:00 immediately |

### Visual State Table
| Quarter | Time Elapsed | Visual Feedback |
| :--- | :--- | :--- |
| **1st Quarter** | 0s - 45s | Segment 1 **Blinks** |
| **2nd Quarter** | 46s - 90s | Seg 1 Solid; Seg 2 **Blinks** |
| **3rd Quarter** | 91s - 135s | Seg 1-2 Solid; Seg 3 **Blinks** |
| **4th Quarter** | 136s - 180s | Seg 1-3 Solid; Seg 4 **Blinks** |

---

## 📱 Visual Design

> **Note:** The UI is built using Jetpack Compose for Wear OS to ensure smooth animations and battery efficiency.

* **Central Display:** Large digital countdown (MM:SS).
* **The Ring:** A `Canvas`-drawn arc system. 
* **Animation:** Uses an `InfiniteTransition` to handle the alpha-pulsing effect on the active segment.

---

## 💻 Tech Stack & Requirements

* **Language:** Kotlin
* **Framework:** Jetpack Compose for Wear OS
* **Min SDK:** API 36 (Wear OS 3.0+)
* **Tooling:** Android Studio Jellyfish or newer

---

## 📥 Installation

1. **Clone the repo:**
   ```bash
   git clone [https://github.com/yourusername/quarterly-countdown.git](https://github.com/yourusername/quarterly-countdown.git)
