---
name: mindfora-webapps
description: Workflow agent untuk membangun & memelihara aplikasi Android (Capacitor WebView) Mindfora di folder mindfora-webapps, termasuk integrasi sisi Laravel (safe-area, native bridge, download, push FCM). Gunakan setiap kali mengerjakan app Android Mindfora.
---

# Mindfora Android App (Capacitor WebView)

## Konteks
- Backend/web: `D:\PROJECT\FREELANCE\mindfora` (Laravel 12 + Livewire 4). Perubahan di sana WAJIB ikut skill `mindfora` (Service/Policy/Permission/komponen Blade, dsb.).
- App Android: `D:\PROJECT\FREELANCE\mindfora-webapps` (repo terpisah).
- Target: **Android saja** (APK untuk testing, AAB untuk Play Store).
- Stack: **Capacitor (versi stable terbaru, rilis >= 7 hari)** + Vite + TailwindCSS untuk halaman shell lokal (offline/error). Dipilih karena bisa di-improve bertahap (plugin native, custom Java/Kotlin di MainActivity) tanpa ganti stack, dan siap iOS kalau nanti dibutuhkan.
- Arsitektur: WebView memuat web Mindfora remote (`server.url`). Halaman lokal yang di-bundle cuma `offline.html` (`server.errorPath`). Semua UI aplikasi tetap dari Laravel — JANGAN duplikasi fitur web di app.

## Aturan Wajib
1. **URL tidak boleh di-hardcode.** URL web diambil dari env (`MINDFORA_URL`) di `capacitor.config.ts`, dengan file `.env.development` / `.env.production`. Ganti URL = ubah env + `npx cap sync android` + rebuild. Host SSO/eksternal yang harus tetap di dalam WebView masuk `server.allowNavigation` (juga dari env).
2. **Wajib HTTPS** di production. `cleartext`/`allowMixedContent` hanya boleh di build development.
3. **Deteksi app di Laravel lewat User-Agent**: `appendUserAgent: 'MindforaApp/<versi>'`. Laravel cukup punya SATU helper (mis. `is_native_app()`); JANGAN sebar pengecekan UA mentah di Blade.
4. **Native bridge di sisi web** tinggal di SATU file `resources/js/native-bridge.js` di Laravel, cuma aktif kalau `window.Capacitor?.isNativePlatform()`. Akses plugin lewat `window.Capacitor.Plugins.*` (bridge di-inject Capacitor) — JANGAN tambah dependency npm Capacitor ke project Laravel.
5. **Folder `android/` hasil generate**: yang boleh diedit manual cuma `MainActivity`, `AndroidManifest.xml`, `res/` (ikon, warna, file_paths), `build.gradle` (versi/signing). Setiap edit manual dicatat di `AGENTS.md` mindfora-webapps.
6. **Secret tidak boleh di-commit**: keystore (`*.jks`), `keystore.properties`, `google-services.json`, file service-account Firebase. Semuanya masuk `.gitignore`; kredensial di backend lewat `.env`.
7. **Desain**: ikuti brand Mindfora (tema hijau landing page, font Figtree, dark-mode aware). Shell lokal pakai Tailwind dengan token warna yang SAMA dengan `tailwind.config.js` Laravel.
8. **Edge-to-edge** (Android 15+ memaksanya): layout Laravel wajib `viewport-fit=cover` + padding `env(safe-area-inset-*)` di header/sidebar/bottom bar; status bar & navigation bar ikut tema (light/dark).
9. Shell PowerShell 5.1: chain command pakai `;`, bukan `&&`.

## Fase Kerja (urut — jangan loncat; tiap fase ada DoD)

### Fase 0 — Prasyarat & Scaffolding
- Cek/instal: Node >= 20, **JDK 21**, **Android Studio + SDK** (platform & build-tools sesuai targetSdk yang diwajibkan Play Store saat ini), set `ANDROID_HOME`/`JAVA_HOME`.
- `npm init` + Capacitor core/cli/android, Vite + Tailwind untuk `src/` (shell), `npx cap add android`.
- Tentukan `appId` (reverse-domain, permanen setelah publish) & `appName` "Mindfora".
- Spike: pastikan plugin Capacitor bisa dipanggil dari halaman remote (`server.url`) dan `errorPath` muncul saat offline.
- DoD: `npx cap sync android` + `cd android; .\gradlew assembleDebug` sukses, APK membuka web Mindfora di emulator/HP.

### Fase 1 — Native Shell & UX Dasar
- Splash screen (Android 12+ SplashScreen API via `@capacitor/splash-screen`) + adaptive icon. Aset digenerate oleh `npm run assets` (`scripts/generate-assets.mjs`, sumber tunggal logo + warna) — `@capacitor/assets` TIDAK dipakai (masih bergantung ke CLI v5 & sharp lama).
- Status/navigation bar: plugin `SystemBars` bawaan `@capacitor/core` (JANGAN pasang `@capacitor/status-bar`). `insetsHandling: 'native'` + `EdgeToEdge.enable()` di MainActivity; web pakai `env(safe-area-inset-*)`.
- `offline.html` (desain brand, ilustrasi, tombol "Coba lagi", auto-retry saat koneksi kembali).
- Tombol back Android: `history.back()`; di halaman root -> "Tekan sekali lagi untuk keluar".
- Link eksternal (`wa.me`, `mailto:`, `tel:`, domain lain di luar allowNavigation) dibuka di app/browser eksternal.
- Status bar/navigation bar sinkron dengan dark mode (class `dark` di `<html>` -> native-bridge -> `SystemBars.setStyle`). Halaman auth mobile (gradien hijau) ditandai `data-system-bars="dark"` di `<body>` guest layout.
- Bottom navigation khas app (`App\Livewire\Layout\BottomNav`, data dari `HasMenuItems`, hanya render bila `is_native_app()`).
- Pull-to-refresh (aktif cuma saat scroll di posisi paling atas; jangan bentrok dengan container `.custom-scrollbar`).
- Sisi Laravel: helper `is_native_app()`, `viewport-fit=cover` + safe-area, sembunyikan elemen yang tidak relevan di app (mis. CTA "download app" di landing).
- DoD: semua alur di atas lolos di HP fisik light & dark mode.

### Fase 2 — Upload & Download File
- Upload: `<input type="file">` (materi audio/video, thumbnail) memunculkan file chooser native; izin media sesuai versi Android.
- Download dari route (`training.download`, dsb.): ditangani native (DownloadListener + DownloadManager) dengan **cookie sesi WebView** supaya route yang butuh auth tetap jalan.
- Export Excel/PDF Livewire mengirim file sebagai **blob URL** (DownloadManager tidak bisa memproses `blob:`). Tangani di `native-bridge.js`: blob -> base64 -> `Filesystem.writeFile` -> notifikasi/opsi buka atau share file.
- Audio/video: playback + fullscreen video jalan, audio tidak putus saat layar mati (kalau dibutuhkan).
- DoD: upload materi sampai status "completed", download materi, export Excel & PDF semuanya berhasil dari app.

### Fase 3 — Push Notification (FCM)
- Prasyarat dari user: project Firebase, `google-services.json`, service account (backend). Paket composer baru WAJIB dikonfirmasi user dulu.
- Backend (ikut skill `mindfora`): tabel `device_tokens` (user_id, token unik, platform, last_seen_at), Service registrasi/hapus token, registrasi token lewat **komponen Livewire tanpa UI** di app layout (bukan controller baru), hapus token saat logout, kirim push lewat channel Notification Laravel yang sudah ada (via queue). Token invalid dari FCM dibersihkan otomatis.
- App: `@capacitor/push-notifications`, izin `POST_NOTIFICATIONS` (Android 13+) diminta dengan konteks (setelah login, bukan saat app pertama dibuka), notification channel, tap notifikasi -> buka URL tujuan (deep link di dalam WebView).
- Event yang memicu push: notifikasi sistem yang sudah ada + pesan konsultasi/chat baru + test minggu berikutnya terbuka (konfirmasi daftar finalnya dengan user).
- DoD: push diterima saat app foreground/background/tertutup; tap membuka halaman yang benar; token tidak dobel per device.

### Fase 4 — Polish & QA
- Matriks uji: Android 10, 13, 14/15+, HP layar kecil & tablet, light/dark, koneksi lambat/terputus.
- Sesi tidak gampang logout (review `SESSION_LIFETIME` / remember-me untuk pengguna app), cookie di-flush saat app di-pause.
- Reverb realtime lewat `wss://` domain produksi (chat konsultasi).
- Performa: tidak ada layar putih (backgroundColor = warna brand), transisi `wire:navigate` mulus.
- DoD: checklist QA di `AGENTS.md` lulus semua.

### Fase 5 — Release
- Keystore release (dibuat & disimpan oleh user, di luar repo), `keystore.properties`, signing config di Gradle.
- `versionCode`/`versionName` naik setiap rilis.
- `.\gradlew bundleRelease` (AAB) + `assembleRelease` (APK sideload).
- Siapkan aset Play Store: ikon 512, feature graphic, screenshot, kebijakan privasi (wajib karena ada login & push). Risiko kebijakan "Minimum Functionality" untuk app WebView dikurangi lewat fitur native (push, offline, download, splash).
- DoD: AAB ter-sign, terinstal bersih di HP, siap upload.

## Verifikasi Standar (setiap perubahan)
- App: `npm run build; npx cap sync android; cd android; .\gradlew assembleDebug`
- Laravel (kalau disentuh): `npm run build`, `./vendor/bin/pint`, test terkait, `php artisan migrate:fresh --seed` kalau schema berubah.
- Uji manual di HP: alur yang terdampak + light/dark + offline.

## Definition of Done (lampirkan di setiap akhir fase)
- [ ] URL dari env, tidak ada yang di-hardcode; HTTPS di production
- [ ] Build debug sukses; tidak ada secret yang ter-commit
- [ ] Perubahan Laravel ikut skill `mindfora` (Service/Policy/komponen/Pint/test)
- [ ] Native bridge cuma di `resources/js/native-bridge.js`; deteksi app cuma lewat `is_native_app()`
- [ ] Safe-area & dark mode benar di HP fisik
- [ ] Edit manual di `android/` tercatat di `AGENTS.md`
- [ ] Alur fase ini lolos uji manual (online, offline, light, dark)
