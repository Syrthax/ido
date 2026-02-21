# iDo — Cross-Platform Task Manager

> Minimalist, privacy-first task management synced to your Google Drive. No servers. No tracking. Your data stays yours.

[![Build Status](https://github.com/Syrthax/ido/actions/workflows/build.yml/badge.svg)](https://github.com/Syrthax/ido/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows%20%7C%20Linux%20%7C%20Android%20%7C%20Web-lightgrey)](https://github.com/Syrthax/ido/releases)

---

## Overview

iDo is a lightweight, cross-platform task manager that uses **Google Drive** as its backend — no custom servers, no databases, no analytics. Authenticate once with Google, and your tasks are stored as a JSON file inside your own Drive. Everything syncs automatically across every platform you use.

---

## Features

| Feature | Details |
|---------|---------|
| **Google Drive Sync** | Tasks stored in `iDo/ido-data.json` inside your own Drive |
| **Google Calendar Integration** | Create, edit, and delete calendar events directly from iDo |
| **Calendar Views** | Week view (Mon–Sat) and Day view |
| **Task Management** | Priority, Today, Later, and Unscheduled task sections |
| **Zero Backend** | OAuth 2.0 only — no intermediate server, no data collection |
| **Privacy First** | No analytics, no cookies, no server logs |
| **Cross-Platform** | macOS, Windows, Linux (Tauri), Android (Kotlin), Web |
| **Minimal UI** | Clean, distraction-free design focused on productivity |

---

## Download

| Platform | Download |
|----------|----------|
| **macOS** | [iDo.dmg](https://github.com/Syrthax/ido/releases/latest) |
| **Windows** | [iDo Setup.exe](https://github.com/Syrthax/ido/releases/latest) |
| **Linux** | [iDo.AppImage](https://github.com/Syrthax/ido/releases/latest) |
| **Android** | [iDo.apk](https://github.com/Syrthax/ido/releases/latest) |
| **Web** | [Launch App](https://syrthax.github.io/ido/web/) |

---

## Architecture

iDo uses a **serverless-first** design:

| Component | Implementation |
|-----------|----------------|
| Identity | Google OAuth 2.0 (PKCE) |
| Task Storage | Google Drive JSON (`iDo/ido-data.json`) |
| Event Storage | Google Calendar API |
| Conflict Resolution | Last-write-wins (`updatedAt` timestamp) |
| Desktop Shell | Tauri 2 (Rust + WebView) |
| Mobile | Kotlin, Jetpack Compose, Material 3 |
| Backend | **None** |

---

## Project Structure

```
/
├── index.html          # Landing page
├── styles.css          # Landing page styles
├── script.js           # Landing page JS + download OS detection
├── web/                # Web app (SPA)
│   ├── app.js
│   ├── calendar.js
│   ├── drive.js
│   └── config.js
├── Tauri/ido-desktop/  # Desktop app (Tauri 2)
│   ├── src/            # React/TypeScript frontend
│   └── src-tauri/      # Rust backend + tauri.conf.json
├── android/            # Android app (Kotlin / Jetpack Compose)
├── macosapp/           # Prebuilt macOS distribution
│   ├── iDo.app
│   └── iDo.dmg
└── .github/
    └── workflows/
        └── build.yml   # CI — builds all platforms on push/tag
```

---

## Build Instructions

### Prerequisites

- Node.js 18+  https://nodejs.org/
- Rust stable   https://rustup.rs/
- Tauri CLI: `cargo install tauri-cli`

### Web App

```bash
cd web
cp config.example.js config.js
# Add your Google OAuth Client ID to config.js
npx serve .
```

### Desktop App (Tauri 2)

```bash
cd Tauri/ido-desktop
npm install

# Development with hot-reload
npm run tauri dev

# Production build
npm run tauri build
```

Build output locations:

| Platform | Path |
|----------|------|
| macOS .app | `src-tauri/target/release/bundle/macos/iDo.app` |
| macOS .dmg | `src-tauri/target/release/bundle/dmg/iDo_*.dmg` |
| Windows .msi | `src-tauri/target/release/bundle/msi/iDo_*.msi` |
| Windows .exe | `src-tauri/target/release/bundle/nsis/iDo_*-setup.exe` |
| Linux .AppImage | `src-tauri/target/release/bundle/appimage/iDo_*.AppImage` |
| Linux .deb | `src-tauri/target/release/bundle/deb/iDo_*.deb` |

### Android App

1. Open `android/` in Android Studio (Hedgehog or later)
2. Add your OAuth Client ID to the config
3. Build and run on a device or emulator (API 26+)

---

## Configuration — Google OAuth

1. Open [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project and enable **Google Drive API** and **Google Calendar API**
3. Create OAuth 2.0 credentials (Web Application)
4. Add your redirect URI
5. Set Client ID in `web/config.js`:

```js
export const CLIENT_ID = 'YOUR_CLIENT_ID.apps.googleusercontent.com';
```

> **Note:** The app is in OAuth testing mode. Contact via [Instagram](https://instagram.com/i._._.sarthak) or [email](https://contact.sarthakg.tech) to be added to the allowlist.

---

## CI/CD

Automated builds run via [GitHub Actions](.github/workflows/build.yml) on every push to `main` and on `v*` version tags.

| Platform | Artifacts |
|----------|-----------|
| macOS (Apple Silicon + Intel) | `.dmg` |
| Windows | `.msi`, `.exe` |
| Linux | `.AppImage`, `.deb` |

Artifacts are uploaded to GitHub Releases automatically when a `v*` tag is pushed.

---

## Supported Platforms

| Platform | Min. Version | Status |
|----------|-------------|--------|
| macOS | 11.0 Big Sur | Supported |
| Windows | 10 (64-bit) | Supported |
| Linux | Ubuntu 20.04+ | Supported |
| Android | 8.0 API 26 | Supported |
| Web | Modern browser | Supported |
| iOS | — | Planned |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit: `git commit -am 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

## Author

**Sarthak Ghosh**
- Portfolio: [sarthakg.tech](https://sarthakg.tech/)
- GitHub: [@Syrthax](https://github.com/Syrthax)
- Contact: [contact.sarthakg.tech](https://contact.sarthakg.tech)
