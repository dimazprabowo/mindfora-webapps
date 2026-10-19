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
| Strip abu/hijau di atas & bawah layar, keyboard menyisakan blok kosong | Server belum punya `viewport-fit=cover` (perubahan Laravel belum di-deploy) -> Capacitor fallback mem-padding WebView | Deploy repo `mindfora` ke server; cek: `view-source` halaman login harus berisi `viewport-fit=cover` & `class="native-app"` |
| `gradlew` error `JAVA_HOME is not set` | Terminal dibuka sebelum env di-set | Buka ulang terminal (env user sudah ada) |

## 8. Edit Manual di Folder `android/`

Folder `android/` di-generate oleh `npx cap add android`, tapi DI-COMMIT. Setiap edit manual WAJIB dicatat di sini:

| File | Perubahan | Alasan |
|------|-----------|--------|
| `app/src/main/java/id/mindfora/app/MainActivity.java` | `EdgeToEdge.enable(this)` **sesudah** `super.onCreate` | Layout edge-to-edge konsisten di semua versi Android; web mengatur inset via `env(safe-area-inset-*)`. JANGAN dipindah ke sebelum `super.onCreate`: decor view jadi terbuat dengan tema launch -> muncul title bar "Mindfora" bergambar splash di atas web |
| `app/src/main/java/id/mindfora/app/MainActivity.java` | `WebViewListener.onPageCommitVisible` -> `SplashScreen.hide()` (sekali) | Splash animasi tampil sampai halaman pertama (web / `offline.html`) benar-benar ter-render; `launchShowDuration` (8 dtk) cuma batas atas |
| `app/src/main/java/id/mindfora/app/MainActivity.java` | `setDownloadListener` -> `DownloadManager` + cookie sesi WebView (skip `blob:`) | WebView tidak punya handler download bawaan — respons `Content-Disposition: attachment` sebelumnya gagal diam-diam lalu Capacitor menampilkan `errorPath` |
| `app/src/main/res/values/styles.xml` | `windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon=@drawable/splash_icon` + `windowSplashScreenAnimationDuration` di `AppTheme.NoActionBarLaunch` | Splash Android 12+ bertema brand dengan ikon animasi |
| `app/src/main/res/values/styles.xml` | `android:windowBackground=@color/splash_background` di `AppTheme.NoActionBar` | Area di belakang WebView (mis. saat Capacitor mem-padding status/nav bar karena WebView < 140) berwarna brand, bukan abu-abu |
| `app/src/main/res/values/colors.xml`, `values-night/colors.xml` | Warna `splash_background` (primary-600 / primary-950) | Splash terang & gelap |
| `app/src/main/res/drawable/*`, `mipmap-*/*` | **GENERATED** oleh `npm run assets` — jangan edit manual | Ikon adaptif + themed icon, splash, PNG legacy |
| _(dihapus)_ `drawable-v24/`, `drawable-land-*/`, `drawable-port-*/`, `mipmap-*/ic_launcher_foreground.png`, `values/ic_launcher_background.xml` | Aset default Capacitor | Diganti aset brand hasil generator |

## 9. Aset Brand (ikon & splash)

Sumber tunggal: path logo lotus + warna brand di `scripts/generate-assets.mjs` (sama dengan `public/favicon.svg` & `tailwind.config.js` Laravel).
```powershell
npm run assets   # tulis ulang ikon adaptif/monochrome, splash (+ splash_icon.xml animasi), PNG legacy, assets/play-store-icon.png, src/logo.svg
npm run apk:debug
```

## 10. Integrasi Laravel (repo `mindfora`)

| Bagian | Lokasi | Fungsi |
|--------|--------|--------|
| Deteksi app | `app/helpers.php` -> `is_native_app()` | UA berisi `MindforaApp/`. SATU-SATUNYA tempat cek UA |
| Bridge JS | `resources/js/native-bridge.js` | Warna ikon system bar ikut dark mode, tombol back (tutup modal/sidebar -> history -> minimize), pull-to-refresh, haptic `[data-haptic]` |
| Safe-area | `resources/css/app.css` (`pt-safe`, `pb-safe`, `mt-safe`, `.native-app .app-main`) | Navbar, sidebar, toast, auth shell tidak tertutup status/gesture bar |
| Bottom nav | `App\Livewire\Layout\BottomNav` + `livewire/layout/bottom-nav.blade.php` | Tab bar khas app (< lg), item dari `HasMenuItems` (maks 4 + "Lainnya" = buka sidebar) |
| Root `/` | `routes/web.php` | Di app langsung ke login/dashboard (landing page hanya untuk browser) |
| Test | `tests/Feature/NativeAppTest.php` | Redirect root, class native-app, bottom nav hanya di app |

**PENTING:** perubahan sisi Laravel baru terlihat di app setelah di-deploy ke server `MINDFORA_URL` (`git pull` + `npm run build` di server). APK cukup di-build ulang kalau yang berubah ada di repo ini.

## 11. Status Fase (lihat SKILL.md)

- [x] Fase 0 — Prasyarat & scaffolding (diuji di HP: online + offline page OK)
- [x] Fase 1 — Native shell & UX dasar (ikon/splash brand, edge-to-edge, system bars, back button, pull-to-refresh, haptic, bottom nav) — tunggu uji HP setelah deploy Laravel
- [ ] Fase 2 — Upload & download file
- [ ] Fase 3 — Push notification (FCM)
- [ ] Fase 4 — Polish & QA
- [ ] Fase 5 — Release (keystore, AAB)
