# Panduan Mindfora Android App (Capacitor)

> Runbook mandiri untuk app Android Mindfora. Workflow & aturan agent ada di `.devin/skills/mindfora-webapps/SKILL.md`.
>
> **Shell:** Windows PowerShell 5.1 — chain command pakai `;`, BUKAN `&&`. (Script `npm run ...` jalan di cmd, jadi `&&` di dalam `package.json` aman.)

## 1. Ringkasan

| Item | Nilai |
|------|-------|
| App ID | `id.mindfora.app` (JANGAN diganti setelah publish ke Play Store) |
| Nama app | Mindfora |
| Web yang dibuka | `MINDFORA_URL` di `.env.production` (saat ini `https://mindfora.id`) |
| Stack | Capacitor 8.5.3 + Vite 7 + Tailwind 3 (shell lokal: `offline.html`) |
| Android | minSdk 24, compile/target SDK 36, JDK 21 |
| User-Agent | `... MindforaApp/<versi package.json>` (dipakai Laravel untuk deteksi app) |

Cara kerja: WebView langsung memuat `MINDFORA_URL` (`server.url`). Kalau gagal dimuat (offline / server down), Android menampilkan `dist/offline.html` (`server.errorPath`), yang auto-retry ke `<URL>/up` lalu kembali ke web.

## 2. Prasyarat (cek sekali)

```powershell
node --version          # >= 22 (Capacitor 8)
java -version           # 21.x (Microsoft OpenJDK 21)
echo $env:JAVA_HOME     # C:\Program Files\Microsoft\jdk-21...
echo $env:ANDROID_HOME  # C:\Users\<user>\AppData\Local\Android\Sdk
```

Kalau belum ada:
```powershell
winget install --id Microsoft.OpenJDK.21 --silent --accept-source-agreements --accept-package-agreements
# Android SDK (tanpa Android Studio): download commandlinetools-win-*_latest.zip dari
# https://developer.android.com/studio#command-line-tools-only, ekstrak ke
# $env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest, lalu:
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-36" "build-tools;36.0.0" "build-tools;35.0.0"
```
Setelah set `JAVA_HOME` / `ANDROID_HOME`, TUTUP & BUKA ULANG terminal.
Kalau download SDK putus ("Connection reset"), jalankan ulang perintah `sdkmanager` yang sama — paket yang sudah selesai dilewati.

## 3. Setup Awal (sekali / setelah clone)

```powershell
cd D:\PROJECT\FREELANCE\mindfora-webapps
npm install
# .env.production & .env.development sudah ada; kalau belum: copy .env.example .env.production
npm run sync            # build shell + generate/update project android/
```

## 4. Ganti URL Web

1. Edit `MINDFORA_URL` di `.env.production` (prod) atau `.env.development` (dev).
2. Host tambahan yang harus tetap di dalam app (mis. SSO) -> `MINDFORA_ALLOW_NAVIGATION` (pisah koma). Host lain otomatis dibuka di browser eksternal.
3. `npm run sync` (atau `npm run sync:dev`) lalu build ulang APK.

## 5. Build APK

```powershell
cd D:\PROJECT\FREELANCE\mindfora-webapps
npm run apk:debug        # production URL (https://mindfora.id)
npm run apk:debug:dev    # development URL (.env.development, boleh http)
```
Hasil: `android\app\build\outputs\apk\debug\app-debug.apk` — kirim ke HP & install (izinkan "Install unknown apps").

Install langsung ke HP via USB (USB debugging aktif):
```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r android\app\build\outputs\apk\debug\app-debug.apk
```

## 6. Development dengan Laravel Lokal

- Emulator: `.env.development` -> `MINDFORA_URL=http://10.0.2.2:8211` (10.0.2.2 = localhost laptop).
- HP fisik: pakai IP LAN laptop (`php artisan serve --host=0.0.0.0 --port=8211`) atau URL cloudflared tunnel (https).
- Debug WebView (build dev): buka `chrome://inspect` di Chrome laptop saat HP tersambung USB.

## 7. Troubleshooting

| Gejala | Penyebab | Solusi |
|--------|----------|--------|
| `.env.production tidak ditemukan` saat `cap sync` | File env belum dibuat | `copy .env.example .env.production` |
| Layar offline terus padahal internet ada | `<URL>/up` tidak bisa dijangkau / bukan https | Cek URL di browser HP, cek SSL domain |
| `SDK location not found` | `ANDROID_HOME` belum terbaca | Buka ulang terminal; atau buat `android\local.properties` berisi `sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` |
| `Unsupported class file major version` / Gradle error Java | JDK bukan 21 | Set `JAVA_HOME` ke JDK 21, buka ulang terminal |
| Halaman http tidak terbuka di build prod | Cleartext diblokir (benar) | Production wajib https; untuk lokal pakai `npm run apk:debug:dev` |

## 8. Edit Manual di Folder `android/`

Folder `android/` di-generate oleh `npx cap add android`, tapi DI-COMMIT. Setiap edit manual WAJIB dicatat di sini:

| File | Perubahan | Alasan |
|------|-----------|--------|
| _(belum ada)_ | | |

## 9. Status Fase (lihat SKILL.md)

- [x] Fase 0 — Prasyarat & scaffolding (build APK sukses; uji di HP fisik: online + mode pesawat -> offline page)
- [ ] Fase 1 — Native shell & UX dasar (+ perubahan Laravel: deteksi app, safe-area)
- [ ] Fase 2 — Upload & download file
- [ ] Fase 3 — Push notification (FCM)
- [ ] Fase 4 — Polish & QA
- [ ] Fase 5 — Release (keystore, AAB)
