# 🔔 AbsenAlarm - Alarm Absensi ASN

Aplikasi Android alarm pengingat absensi masuk dan pulang untuk ASN.
Otomatis mengarahkan ke halaman login STAR-ASN untuk melakukan absensi.

## ✨ Fitur

- ⏰ **Alarm Absen Masuk & Pulang** - Setting waktu terpisah
- 📅 **Pilih Hari** - Aktifkan alarm hanya di hari tertentu (Senin-Jumat, dll)
- 🎵 **Custom Ringtone** - Gunakan nada dering, lagu, atau suara apapun
- 📱 **Full Screen Alert** - Tampil di atas semua aplikasi & layar kunci
- 🔒 **Tidak Bisa Di-close** - Hanya bisa dikonfirmasi dengan tombol ABSEN
- 🔗 **Auto Redirect** - Langsung buka halaman login STAR-ASN
- 🔄 **Auto Start** - Alarm aktif kembali setelah HP restart

## 📲 Cara Install

### Opsi 1: Download APK dari GitHub Actions
1. Buka tab **Actions** di repository ini
2. Klik workflow **Build APK** yang terakhir berhasil (✅)
3. Scroll ke bawah, klik **AbsenAlarm-debug** untuk download APK
4. Transfer ke HP, install (aktifkan "Install dari sumber tidak dikenal")

### Opsi 2: Build Sendiri dengan Android Studio
1. Clone repository ini
2. Buka dengan Android Studio
3. Klik **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK ada di `app/build/outputs/apk/debug/`

## ⚙️ Cara Penggunaan

1. Buka aplikasi **AbsenAlarm**
2. Aktifkan toggle **Alarm Masuk** dan set waktunya (misal 07:25)
3. Pilih hari-hari yang diinginkan (Senin-Jumat)
4. Pilih nada dering yang diinginkan
5. Ulangi untuk **Alarm Pulang** (misal 15:55)
6. Berikan semua izin yang diminta
7. Selesai! Alarm akan berbunyi sesuai jadwal

## 🔐 Izin yang Diperlukan

- **Alarm & Pengingat** - Untuk menjadwalkan alarm tepat waktu
- **Notifikasi** - Untuk menampilkan alarm
- **Tampil di Atas Aplikasi Lain** - Untuk alarm fullscreen
- **Autostart** - Agar alarm aktif setelah HP restart

## 📋 URL Absen Default

```
https://star-asn.kemenimipas.go.id/authentication/login
```

URL bisa diubah di halaman pengaturan aplikasi.

## 🛠️ Tech Stack

- Java (Android SDK)
- Material Design 3
- AlarmManager + Foreground Service
- Min SDK 26 (Android 8.0 Oreo)
- Target SDK 34 (Android 14)

