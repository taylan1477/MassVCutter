# Mass Video Cutter Tool (Toplu Video Kırpma Aracı)

## 1. PROJENİN AMACI
Bu proje, film, dizi ve anime arşivlerini düzenleyerek zamandan ve depolamadan tasaaruf etmeyi amaçlar.
Intro, outro ve gereksiz kısımların otomatik tespiti ve kırpılmasını sağlamayı amaçlıyor.
JavaFX ile arayüz sağlanacak, videolar FFmpeg ile işlenecek ve farklı algoritmalarla otomatik tespit yapılacaktır.
---

## 2. ANA ÖZELLİKLER
✅ YAPILACAKLAR:
1. **Otomatik Intro/Outro Algılama**
   - Manuel Zaman Aralığı Belirleme
   - Ses Enerjisi Analizi
   - Görüntü Analizi (Gerekirse)
   - JLibrosa ile Müzik Analizi (Gerekirse)
   - PySceneDetect ile Sahne Geçiş Analizi (Gerekirse)

2. **Kullanıcı Kontrollü Video Kırpma**
   - Kullanıcı, videonun **thumbnail önizlemesi üzerinden başlangıç ve bitiş noktalarını işaretleyebilecek**.
   - İşaretlenen kesim bilgileri **.txt formatında saklanacak** ve daha sonra kullanılabilecek.

3. **Gerçek Zamanlı İşlem Takibi**
   - Video işlenirken ilerleme çubuğu göstergesi (Progress Bar) olacak.
   - Çoklu dosya işlemleri **Thread Pool** ile yönetilecek.

4. **Sürükle-Bırak ile Dosya Seçimi**
   - Kullanıcı **birden fazla video dosyasını** sürükleyerek programa ekleyebilecek.

5. **FFmpeg CLI Entegrasyonu**
   - Videoların kırpılması **FFmpeg** ile gerçekleştirilecek.
---

## 3. TEKNİK YAPI
├───Core
│   ├── **FFmpegWrapper** (Facade Pattern) - FFmpeg komutlarını sarmalar.
│   ├── **AudioAnalyzer** (Ses Enerjisi Hesaplama)
│   ├── **SceneDetector** (PySceneDetect ile sahne geçiş analizi)
│   ├── **SpectralAnalyzer** (JLibrosa ile frekans analizi)
│   ├── **TaskManager** (Thread Pool ile çoklu işlem yönetimi)
│   └── **ManualTrimHandler** (Manuel kesim noktalarını yönetir)
├───UI
│   ├── **MainController** (Observer Pattern)
│   ├── **ThumbnailGenerator** (FFmpeg + JavaFX ImageView)
│   ├── **VideoPreview** (JavaFX içinde video önizleme)
│   ├── **DragDropHandler** (Dosya sürükleme desteği)
│   ├── **CutPointSelector** (Kullanıcının işaretlediği noktaları yönetir)
│   └── **FileExporter** (Kesim bilgilerini .txt formatında kaydeder)
└───Util
    ├── **ProgressUpdater** (Runnable + Observer ile ilerleme çubuğu yönetimi)
    ├── **FFmpegBinaryLoader** (Platforma özel FFmpeg yükleme)
    └── **DataSerializer** (.txt formatında veri kaydetme/yükleme)
---

## 4. KULLANILAN DESIGN PATTERNLER
1. **FACADE**: FFmpeg komutlarını sarmalayan sınıf.
2. **OBSERVER**: Progress bar ve UI güncellemeleri için.
3. **FACTORY**: FFmpeg komut builder’ı (MP4/MKV gibi farklı formatlar için).
4. **STRATEGY**: Farklı intro/outro tespit yöntemleri arasında geçiş yapılmasını sağlar.
---

## 5. OTOMATİK TESPİT YÖNTEMLERİ
### ✅ 1. SES ENERJİSİ ANALİZİ (ÖNCELİKLİ)
- Ses seviyelerini analiz ederek belirli bir müzik veya yüksek sesli intro/outro bölümlerini algılar.
- **Avantajları:** Hızlı ve basit.
- **Dezavantajları:** Sessiz veya düşük sesli introlarda başarısız olabilir.

### ✅ 2. MANUEL ZAMAN ARALIĞI BELİRLEME (ÖNCELİKLİ)
- Kullanıcı, videonun belirli bir kısmını işaretleyerek intro/outro noktalarını manuel belirleyebilir.
- **Avantajları:** En güvenilir yöntem.
- **Dezavantajları:** Kullanıcıdan manuel giriş gerektirir.

### 🔄 3. GÖRÜNTÜ ANALİZİ (OPSİYONEL)
- Siyah ekran, büyük metinler veya belirli sahne geçişlerini tespit ederek intro/outro bölgelerini bulur.
- **Avantajları:** Netflix gibi sistemlere daha yakın bir yaklaşım sağlar.
- **Dezavantajları:** Hesaplama açısından ağırdır.

### 🔄 4. JLIBROSA İLE Müzik ANALİZİ (OPSİYONEL)
- Ses dosyasını frekans bileşenlerine ayırarak belirli müzikleri tanımlamak için kullanılır.
- **Avantajları:** Daha karmaşık analiz yapılmasını sağlar.
- **Dezavantajları:** Uygulaması zor ve işlem gücü gerektirir.

### 🔄 5. PYSCENEDETECT İLE SAHNE GEÇİŞ ANALİZİ (OPSİYONEL)
- PySceneDetect kullanılarak keskin sahne değişiklikleri algılanır.
- **Avantajları:** Özellikle büyük metin geçişleri olan One Piece gibi içeriklerde etkili olabilir.
- **Dezavantajları:** Python entegrasyonu gerektirir.
---

## 6. ÇALIŞMA SIRASI
✅ **1. Aşama (Temel İşlevler)**
   - [ ] **Ses Analizi** ile otomatik tespit
   - [ ] **Manuel Kesim** noktaları işaretleme
   - [ ] **FFmpeg ile kırpma ve dışa aktarma**
   - [ ] **Kullanıcı dostu UI tasarımı**

🔄 **2. Aşama (Gelişmiş Algoritmalar)**
   - [ ] **Görüntü Analizi** ekleme
   - [ ] **JLibrosa spektrum analizi** ekleme
   - [ ] **PySceneDetect entegrasyonu**
   - [ ] **Toplu işlem hız optimizasyonları**
---



## 7. YAZILIM GELİŞTİRME METODU
- XP’yi tercih etmemizin nedeni, küçük bir ekip olarak yoğun kod geliştirme odaklı çalışmamızdır. Scrum daha çok süreç yönetimine odaklanırken,
  XP doğrudan kod kalitesini arttıran teknik pratiklerle donatılmıştır ve bu bizim ihtiyaçlarımıza daha uygundur.

  2 kişiyiz ve yoğun kod odaklı çalışıyoruz bu durumda Scrum'ın süreç yönetimine odaklanan pratikleri yerine 
  XP'nin doğrudan kod kalitesini arttıran teknik pratiklerini tercih ediyoruz.
