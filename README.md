

# 🚀 iDo — A Minimal, Privacy-First To-Do App (Synced with Google Drive)

iDo is a lightweight task manager that keeps your data fully in your control.
No servers, no databases, no tracking — all your tasks are stored securely in your own Google Drive using OAuth 2.0.

✨ Perfect for students, builders, and anyone who wants a frictionless, cross-device to-do list.

---

## ✨ New in This Update

- **Full Calendar integration** — Week View (Mon–Sat) + Day View
- **Create / edit / delete tasks** directly from Calendar
- **Create / edit / delete Google Calendar events** from Calendar view
- **Unified sync** — tap the cloud icon to force sync
- **Task & Event info/edit sheets** — tap any item for full CRUD
- **Serverless architecture** — OAuth + Google Drive JSON only
- **Web ↔ Android feature parity** — same functionality on both platforms

---

## 🧠 Architecture

iDo uses a **serverless-first** design:

| Component | Implementation |
|-----------|----------------|
| Identity | Google OAuth 2.0 |
| Task Storage | Google Drive JSON (`iDo/ido_data.json`) |
| Event Storage | Google Calendar API |
| Conflict Resolution | Last-write-wins (`updatedAt` timestamp) |
| Backend | None — all client-side |

No intermediate servers. Your data stays in your Google account.

---

## 📱 Screens (Android)

| Screen | Features |
|--------|----------|
| **Tasks** | Priority / Today / Later / Unscheduled sections |
| **Calendar** | Week View + Day View, task & event display, CRUD |
| **Settings** | Profile info, sync status, sign out |

---

# 🌟 Features

🔐 **Login with Google (OAuth 2.0)**
- Secure authentication flow
- Uses Google Drive API to store your tasks
- No backend server needed — everything happens on your device

☁️ **Real-Time Sync via Google Drive**
- Tasks are saved inside a dedicated folder in your Drive
- Automatically synced across browser sessions and Android

🧹 **Minimal, Distraction-Free UI**
- Clean layout focused on productivity
- Add, delete, and toggle tasks instantly
- Mobile-responsive design

🔒 **Your Data. Your Drive.**
- iDo stores zero data outside Google Drive
- No analytics, no cookies, no logging
- Perfect for privacy-conscious users

---

# 🖼️ Live Demo

👉 **Web app:** https://syrthax.github.io/ido/web/

---

# 🧰 Tech Stack

| Component | Technology |
|-----------|------------|
| Web Frontend | HTML, CSS, JavaScript |
| Android | Kotlin, Jetpack Compose, Material 3 |
| Auth | Google OAuth 2.0 |
| Storage | Google Drive API, Google Calendar API |
| Hosting | GitHub Pages (web) |

---

# 🛠️ How It Works
1. User signs in with Google
2. OAuth returns a token authorized for Drive + Calendar access
3. iDo checks for a folder named "iDo" in Google Drive
   - If not found, it creates one
4. Tasks are stored in a JSON file: `iDo/ido_data.json`
5. Events are read/written via Google Calendar API
6. Adding/deleting tasks immediately updates the Drive file

This architecture means iDo requires no backend server, making it extremely fast, safe, and free to operate.

<img width="1710" height="982" alt="image" src="https://github.com/user-attachments/assets/5c21034e-16aa-4564-a948-4744eaa59503" />

---

# 🧑‍💻 Run Locally

### Web
1. Clone the repo:
   ```bash
   git clone https://github.com/Syrthax/ido
   cd ido
   ```
2. Create your own Google OAuth credentials and configure `web/config.js`
3. Use Live Server or open `/web/index.html` directly

### Android
1. Open `/android` folder in Android Studio
2. Add your OAuth client ID in the appropriate config
3. Build and run on device/emulator

---

# 📁 Project Structure

```
/ (Landing page)
├── index.html
├── styles.css
├── script.js

/web (Web app)
├── index.html
├── app.js
├── calendar.js
├── drive.js
├── config.js

/android (Android app)
├── app/src/main/java/com/ido/app/
│   ├── data/          # Models, repositories, data sources
│   ├── ui/            # Compose screens and components
│   ├── sync/          # Sync management
│   └── notifications/ # Task reminders
```

---

# 📜 License

This project is open-source under the MIT License.
You are free to modify, distribute, and use it in your own projects.


# 🤝 Contributing

Pull requests are welcome!
If you have ideas for improvements—like reminders, labels, widgets—drop an issue or submit PRs.


# ✨ Author

Sarthak
Portfolio: [Portfolio](https://sarthakg.tech/)
GitHub: https://github.com/Syrthax
