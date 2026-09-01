# 🎨 DitherLab Ultra v3.0

![DitherLab Ultra](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![ML Kit](https://img.shields.io/badge/AI-Google_ML_Kit-4285F4?style=for-the-badge&logo=google)
![License](https://img.shields.io/badge/License-GPL_3.0-blue?style=for-the-badge)

**DitherLab Ultra**, orijinal DitherLab PWA projesinden ilham alınarak geliştirilmiş; nostaljik, retro ve siberpunk görsel estetiğini modern Android cihazların gücüyle buluşturan **yüksek performanslı bir fotoğraf ve VİDEO düzenleme** uygulamasıdır.

Tüm sihir tamamen **cihazınızda (on-device offline)** gerçekleşir. İnternete veya buluta ihtiyaç duymadan, çok çekirdekli asenkron (multi-core concurrency) mimarisiyle saniyeler içinde büyüleyici sonuçlar alırsınız! 🚀

---

## 🌟 v3.0 - Çift Motorlu Stüdyo & Lightroom Deneyimi

V3.0 güncellemesiyle DitherLab Ultra, profesyonel bir karanlık oda laboratuvarına dönüşüyor!
- 🎛️ **Lightroom Tarzı Şeffaf Menü:** Sağdan açılan dinamik laboratuvar menüsü. Ayarları kaydırırken (slider) arayüz şeffaflaşır ve görseli tam ekran görerek en ince detayları düzenleyebilirsiniz.
- 🎭 **Çift Motorlu Harmanlama (Dual-Engine Compositing):** Arka plan için farklı, ML destekli özne (insan/nesne) için farklı bir retro motor seçin! Arka planı glitch yapıp öznenizi pixel art yapabilirsiniz!
- 🔲 **Özel Şekil Maskeleri & Şeffaflık:** Özne kesimlerini Daire, Kalp veya Yıldız gibi maskelere sığdırın; ayrıca *Şekil İçi Şeffaf Olsun* özelliği ile kesilen maskenin içini tamamen şeffaf bırakarak harika kolajlar oluşturun.
- 🌑 **Karanlık Oda Entegrasyonları:** Orijinal *karanlik-oda* projesindeki devrimsel piksel manipülasyonları eklendi. Artık **Chromatic Aberration (Kromatik Sapma)**, **Swirly Bokeh** ve özel **Darkroom** harmanlamaları tek tıkla elinizin altında!

---

## ✨ Öne Çıkan Özellikler

- 🎞️ **GELİŞMİŞ VİDEO DESTEĞİ:** Artık sadece fotoğraflara değil, videolara da 30+ farklı efekt uygulayabilirsiniz! Asenkron kare işleme (Coroutine concurrency) ve FFmpeg entegrasyonu sayesinde videolarınız ses kayması yaşamadan ve donanımı yormadan cihazınızda render edilir.
- 🧠 **Yapay Zeka Destekli Özne Ayrımı (Subject Segmentation):** Google ML Kit kullanılarak fotoğraftaki/videodaki insan veya nesneler saniyeler içinde algılanır.
- 🔒 **Gizlilik Odaklı & Çevrimdışı:** Görüntüleriniz hiçbir bulut sunucusuna gitmez, %100 lokal olarak cihazınızda işlenir.
- ⚡ **Yüksek Performans:** Özel Kotlin `VideoProcessor` ve Dispatcher mimarisi ile CPU'yu verimli kullanarak ısınmayı engeller ve işlem süresini minimize eder.

---

## 🚀 Görsel Motorlar (Engines)

DitherLab, her biri ayrı bir sanat akımını ve retro dönemi yansıtan çok sayıda motora sahiptir:

* 🌌 **Glitch & Chromatic:** Spider-Verse estetiği! RGB kanallarını ayırır, VHS bozulmaları ekler.
* 🎨 **Van Gogh & Swirly Bokeh:** Vektörel akış (flow fields) ve döner bokeh efektleriyle empresyonist dokular.
* 📺 **CrtTv & Darkroom:** Eski tüplü televizyonların tarama çizgileri, fosfor pikselleri ve özel laboratuvar filtreleri.
* 🕹️ **PixelArt & Minecraft:** Görselleri nostaljik 8-bit veya blok tabanlı yapılara dönüştürür.
* 🌡️ **FlirThermal & ThermalPaper:** Termal kamera görüntüsü ve eski yazarkasa fişi estetiği.
* 📰 **BayerDither & HalftoneMatrix:** Klasik gazete baskısı dokuları.
* 💥 **ColorClash, Postcard & PunkFanzine:** Renk zıtlıkları, eski kartpostal ve isyankar fanzin dergisi tarzı efektler.
* 📟 **AsciiMatrix & TextGlitch:** Görüntüleri The Matrix tarzı düşen yazılara dönüştürür.

---

## 🛠 Kurulum ve Derleme

Projeyi derleyip kendi cihazınızda denemek çok kolay:

1. Depoyu klonlayın:
   ```bash
   git clone https://github.com/VelliDR/retro-anomalies-android.git
   ```
2. **Android Studio** ile açın.
3. Gradle senkronizasyonunu bekleyin.
4. Cihazınızda çalıştırın veya `./gradlew assembleDebug` ile APK oluşturun.

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
