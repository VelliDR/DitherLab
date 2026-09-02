# 🎨 DitherLab v3.1

![DitherLab Ultra](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![ML Kit](https://img.shields.io/badge/AI-Google_ML_Kit-4285F4?style=for-the-badge&logo=google)
![License](https://img.shields.io/badge/License-GPL_3.0-blue?style=for-the-badge)

**DitherLab**, orijinal Retro-anomalies PWA ve Karanlık Oda PWA projelerinden ilham alınarak geliştirilmiş; nostaljik, retro, gazete ve siberpunk görsel estetiğini modern Android cihazların gücüyle buluşturan **yüksek performanslı bir fotoğraf ve VİDEO düzenleme** uygulamasıdır.

Tüm sihir tamamen **cihazınızda (on-device offline)** gerçekleşir. İnternete veya buluta ihtiyaç duymadan, çok çekirdekli asenkron (multi-core concurrency) mimarisiyle saniyeler içinde büyüleyici sonuçlar alırsınız! 🚀

---

## 🌟 v3.1 - Spider-Noir, Video Timeline & Ultra Performans Güncellemesi

V3.1 güncellemesiyle DitherLab, profesyonel video kırpma araçları ve gelişmiş Spider-Verse estetiği ile yenilendi!

- 🕷️ **Spider-Noir & Renk Popu Motoru:** *Spider-Man Noir* estetiğinden esinlenilerek baştan tasarlanan Noir motoru!
  - 🎨 **Spider-Red Pop:** Görseldeki kırmızı vurguları canlı koruyarak geri kalan alanı yüksek kontrastlı gazete noktaları ve Noir siyah-beyaza dönüştürür.
  - 🔴 **Özel Nokta Renkleri:** Siyah, Kırmızı ve Lacivert nokta desenleri.
  - 📄 **Gazete Dokusu & Yağmur Çizikleri:** Ayarlanabilir gazete dokusu, yağmur ve çizik grenleri.
  - 🛡️ **Bileşik Katman İzolasyonu:** Özne ve arka plan harmanlamalarında arka plan renklerinin özneye sızması engellendi.
- 🎬 **Dikey Video Zaman Çizelgesi (Timeline Sidebar):** Videosu yüklenen içeriklerde sol tarafa sabitlenen kaydırılabilir dikey zaman çubuğu. Başlangıç ve bitiş noktalarını milisaniye hassasiyetinde belirleyin, FFmpeg ile ses senkronizasyonu bozulmadan anında kırpın.
- 📐 **20+ Kadraj Şekil Maskesi:** Akışkan Nehir (Fluid River), Pul Kenarı (Stamp), Yırtık Kağıt (Torn Paper), Şehir Kemeri (Arch), Sinema Çerçevesi (Cinema Frame) ve Bilet (Ticket) gibi 20'den fazla özel geometrik maske.
- ⚡ **Zero-OOM & Akıcı Shader Performansı:** 
  - Bitmap bellek sızıntıları ve çökme sorunları tamamen çözüldü.
  - Android 13+ AGSL `RuntimeShader` uniform güncellemeleri optimize edildi; slider kaydırmalarında kasma ve yeniden derleme gecikmeleri giderildi.

---

## 🌟 v3.0 - Çift Motorlu Stüdyo & Lightroom Deneyimi

- 🎛️ **Lightroom Tarzı Şeffaf Menü:** Sağdan açılan dinamik laboratuvar menüsü. Ayarları kaydırırken (slider) arayüz şeffaflaşır.
- 🎭 **Çift Motorlu Harmanlama (Dual-Engine Compositing):** Arka plan ve ML destekli özne (insan/nesne) için bağımsız iki retro motor seçin!
- 🌑 **Karanlık Oda Entegrasyonları:** **Chromatic Aberration (Kromatik Sapma)**, **Swirly Bokeh** ve özel **Darkroom** harmanlamaları.

---

## ✨ Öne Çıkan Özellikler

- 🎞️ **GELİŞMİŞ VİDEO DESTEĞİ:** Fotoğraflara ve videolara 30+ farklı efekt uygulayabilirsiniz. Asenkron kare işleme (Coroutine concurrency) ve FFmpeg entegrasyonu sayesinde videolarınız ses kayması yaşamadan render edilir.
- 🧠 **Yapay Zeka Destekli Özne Ayrımı (Subject Segmentation):** Google ML Kit ile insan/nesne maskeleme.
- 🔒 **Gizlilik Odaklı & Çevrimdışı:** %100 lokal işlem, 0 bulut bağımlılığı.

---

## 🚀 Görsel Motorlar (Engines)

* 🕷️ **NoirComic:** Spider-Man Noir tarzı siyah-beyaz gazete baskısı, Spider-Red Pop ve renkli halftone dokuları.
* 🌌 **Glitch & Chromatic:** Spider-Verse estetiği, RGB kanal kaymaları ve VHS bozulmaları.
* 🎨 **Van Gogh & Swirly Bokeh:** Vektörel akış (flow fields) ve döner bokeh efektleri.
* 📺 **CrtTv & Darkroom:** Tüplü televizyon tarama çizgileri, fosfor pikselleri ve karanlık oda laboratuvar filtreleri.
* 🕹️ **PixelArt & Minecraft:** Nostaljik 8-bit ve blok yapılara dönüştürme.
* 🌡️ **FlirThermal & ThermalPaper:** Termal kamera görüntüsü ve eski yazarkasa fişi estetiği.
* 📰 **BayerDither & HalftoneMatrix:** Gazete baskı matrisleri.
* 💥 **ColorClash, Postcard & PunkFanzine:** Renk zıtlıkları, nostaljik kartpostal ve fanzin dergisi tarzı.
* 📟 **AsciiMatrix & TextGlitch:** Matrix düşen kod ve metin bozulması efektleri.

---

## 🛠 Kurulum ve Derleme

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

DitherLab, açık kaynak ruhuna uygun olarak **GNU General Public License v3.0 (GPL-3.0)** lisansı altında dağıtılmaktadır. Daha detaylı bilgi için `LICENSE` dosyasına göz atabilirsiniz.
