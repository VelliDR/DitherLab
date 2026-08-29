# 🎨 DitherLab Ultra

![DitherLab Ultra](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![ML Kit](https://img.shields.io/badge/AI-Google_ML_Kit-4285F4?style=for-the-badge&logo=google)
![License](https://img.shields.io/badge/License-GPL_3.0-blue?style=for-the-badge)

**DitherLab Ultra**, orijinal Retro-Anomalies-PWA projesinden ilham alınarak geliştirilmiş; nostaljik, retro ve siberpunk görsel estetiğini modern Android cihazların gücüyle buluşturan **yüksek performanslı bir fotoğraf ve VİDEO düzenleme** uygulamasıdır.

Tüm sihir tamamen **cihazınızda (on-device offline)** gerçekleşir. İnternete veya buluta ihtiyaç duymadan, çok çekirdekli asenkron (multi-core concurrency) mimarisiyle saniyeler içinde büyüleyici sonuçlar alırsınız! 🚀

---

## ✨ Öne Çıkan Özellikler

- 🎞️ **YENİ! Video İşleme Desteği:** Artık sadece fotoğraflara değil, videolara da efekt uygulayabilirsiniz! Asenkron kare işleme (Coroutine concurrency) ve FFmpeg entegrasyonu sayesinde videolarınız ses kayması yaşamadan ve donanımı yormadan hızla render edilir.
- 🧠 **Yapay Zeka Destekli Özne Ayrımı (Subject Segmentation):** Google ML Kit kullanılarak fotoğraftaki/videodaki insan veya nesneler saniyeler içinde algılanır. Efektleri ister sadece özneye, ister arka plana uygulayın!
- 🔒 **Gizlilik Odaklı & Çevrimdışı:** Görüntüleriniz hiçbir bulut sunucusuna gitmez, %100 lokal olarak cihazınızda işlenir.
- ⚡ **Yüksek Performans:** Özel Kotlin `VideoProcessor` ve Dispatcher mimarisi ile CPU'yu verimli kullanarak ısınmayı engeller ve işlem süresini minimize eder.

---

## 🚀 Görsel Motorlar (Engines)

DitherLab, her biri ayrı bir sanat akımını ve retro dönemi yansıtan çok sayıda motora sahiptir:

* 🌌 **Glitch Engine:** Spider-Verse estetiği! RGB/CMYK kanallarını ayırır, VHS bozulmaları ekler.
* 🎨 **Van Gogh Engine:** Vektörel akış (flow fields) ile kareleri empresyonist tablolara çevirir.
* 📺 **CrtTv Engine:** Eski tüplü televizyonların tarama çizgilerini (scanlines) ve fosfor piksellerini simüle eder.
* 🕹️ **PixelArt & Minecraft:** Görselleri nostaljik 8-bit veya blok tabanlı yapılara dönüştürür.
* 🌡️ **FlirThermal & ThermalPaper:** Termal kamera görüntüsü ve eski yazarkasa fişi estetiği.
* 📰 **BayerDither & HalftoneMatrix:** Klasik gazete baskısı dokuları.
* 💥 **ColorClash, Postcard & PunkFanzine:** Renk zıtlıkları, eski kartpostal ve isyankar fanzin dergisi tarzı efektler.
* 📟 **AsciiMatrix & TextGlitch:** Görüntüleri The Matrix tarzı düşen yazılara dönüştürür.
* 📳 **SensorCorrupt:** Cihaz sensörlerini kullanarak hareket ettikçe veri moshinq (data mosh) oluşturur.

---

## 🛠 Kurulum ve Derleme

Projeyi derleyip kendi cihazınızda denemek çok kolay:

1. Depoyu klonlayın:
   ```bash
   git clone https://github.com/VelliDR/DitherLab.git
   ```
2. **Android Studio** ile açın.
3. Gradle senkronizasyonunu bekleyin.
4. Cihazınızda çalıştırın veya `./gradlew assembleDebug` ile `DitherLab.apk` oluşturun.

---

## 💻 Teknoloji Yığını

- **Dil:** Kotlin
- **UI:** Jetpack Compose (Modern, deklaratif tasarım)
- **AI/ML:** Google ML Kit (Yüksek performanslı özne segmentasyonu)
- **Video Processing:** FFmpeg & Kotlin Coroutines (`async/awaitAll` ile çok çekirdekli kare işleme)
- **Graphics:** Android Canvas API & ColorMatrix işlemleri

---

## 📄 Lisans

DitherLab Ultra, açık kaynak ruhuna uygun olarak **GNU General Public License v3.0 (GPL-3.0)** lisansı altında dağıtılmaktadır. Daha detaylı bilgi için `LICENSE` dosyasına göz atabilirsiniz.
