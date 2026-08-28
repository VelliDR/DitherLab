# DitherLab

DitherLab, güçlü görsel motorları modern ve yüksek performanslı bir Android deneyimiyle birleştiren gelişmiş bir fotoğraf düzenleme uygulamasıdır. 

Tüm görsel işleme süreçleri tamamen **yerel (on-device)** olarak cihazınızda çalışır, böylece internet bağlantısına ihtiyaç duymazsınız ve fotoğraflarınızın gizliliği her zaman korunur. Cihazınızın işlem gücünü maksimum düzeyde kullanarak render işlemlerini saniyeler içinde tamamlar.

## Özellikler

- **Tamamen Yerel İşleme:** Hiçbir görsel buluta yüklenmez. Gizlilik odaklı tasarım.
- **Yapay Zeka Destekli Özne Ayrımı (Subject Segmentation):** ML Kit entegrasyonu sayesinde fotoğraftaki özneyi (insan, nesne) otomatik tanır; efektleri dilerseniz sadece arka plana, dilerseniz sadece özneye veya tüm fotoğrafa uygulayabilirsiniz.
- **Donanım ve Yazılım Hızlandırmalı Render:** Cihaz uyumluluğunu maksimize eden esnek altyapı (Hardware & Software Canvas Fallbacks).

## Görsel Motorlar (Engines)

DitherLab, her biri kendine has bir görsel estetiğe sahip çok sayıda özel render motoru içerir:

1. **Glitch Engine (Spider-Verse Estetiği):** RGB/CMYK kanallarını ayırır, VHS tarzı yatay bozulmalar ve BenDay (halftone) noktalarıyla animasyon filmlerini aratmayan dinamik sahneler yaratır.
2. **Van Gogh Engine (Yağlı Boya):** Vektörel akış alanları (flow fields) kullanarak fotoğrafları tıpkı Van Gogh'un fırça darbeleriyle çizilmiş gibi empresyonist tablolara dönüştürür.
3. **SensorCorrupt Engine:** Cihazın jiroskop (hareket) sensörlerini dinleyerek, telefonu hareket ettirdiğinizde fotoğrafta anlık "data mosh" (veri bozulması) ve sinyal kaybı efektleri oluşturur.
4. **CrtTv Engine:** Eski tüplü televizyonların ekran yapısını, RGB fosfor hücrelerini ve tarama çizgilerini (scanlines) simüle eder.
5. **PixelArt & Minecraft Engines:** Yüksek çözünürlüklü fotoğrafları 8-bit veya blok tabanlı nostaljik sanat eserlerine dönüştürür.
6. **FlirThermal & ThermalPaper Engines:** Isı kamerası veya eski tip termal fiş yazıcı estetiği.
7. **BayerDither & HalftoneMatrix Engines:** Klasik gazete baskısı ve retro bilgisayar dither'lama teknikleri.
8. **ColorClash, Postcard & PunkFanzine Engines:** Zıt renkleri çarpıştıran, retro kartpostal dokusu veren veya isyankar fanzin dergisi tarzı oluşturan estetik filtreler.
9. **AsciiMatrix & TextGlitch Engines:** Fotoğrafı harflerden (ASCII) oluşan matrislere veya bozuk metin dizilimlerine çevirir.

## Kurulum ve Derleme

Bu projeyi derlemek için:
1. Depoyu bilgisayarınıza klonlayın.
2. Android Studio ile açın (Güncel bir JDK ve Android SDK yüklü olmalıdır).
3. Gradle senkronizasyonunu tamamlayın.
4. Cihazınızda veya emülatörde çalıştırın.

Çıktı APK dosyası, derleme sonrasında `DitherLab.apk` olarak oluşacaktır.

## Teknoloji Yığını

- **Android SDK (Kotlin)**
- **Jetpack Compose** (Modern ve deklaratif UI)
- **Google ML Kit** (Özne/Arka plan ayrımı)
- **RenderScript & Canvas API** (İleri düzey pikseller arası işlemler ve donanım ivmelendirme)

## Lisans

Bu proje **GNU General Public License v3.0 (GPL-3.0)** altında lisanslanmıştır. Daha fazla bilgi için kök dizindeki `LICENSE` dosyasına göz atabilirsiniz.
