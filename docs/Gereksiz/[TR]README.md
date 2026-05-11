# Mass Video Cutter Tool (Toplu Video Kırpma Aracı)

## Projenin Amacı ve Çözülen Problem

Bu proje, film, dizi ve anime arşivlerini düzenleyerek **zamandan ve depolamadan tasarruf etmeyi** amaçlar.

### 🎯 Hedefler
- Intro, outro ve gereksiz kısımların **otomatik tespiti** ve kırpılması
- JavaFX ile modern arayüz
- FFmpeg ile profesyonel video işleme
- Farklı algoritmalarla otomatik tespit (ses analizi, sahne tespiti)

### 💾 Tasarruf Potansiyeli

| Dizi | Bölüm Sayısı | Intro+Outro Süresi | Toplam Tasarruf |
|------|--------------|---------------------|-----------------|
| One Piece | 1000+ | ~3 dk/bölüm | **~50 saat / 90+ GB** |
| Naruto | 720 | ~2.5 dk/bölüm | **~30 saat / 50+ GB** |
| Attack on Titan | 87 | ~3 dk/bölüm | **~4 saat / 8+ GB** |

---

## ✨ Özellikler

### Mevcut (Tamamlanan)
- ✅ **Manuel Kırpma** - Sürüklenebilir timeline marker'ları ile başlangıç/bitiş noktası belirleme
- ✅ **Ses Analizi** - Sessizlik tespiti ile intro/outro sınırlarını otomatik bulma
- ✅ **Toplu İşleme** - Aynı ayarlarla birden fazla videoyu kırpma
- ✅ **Sürükle-Bırak** - Dosyaları uygulamaya sürükleyerek ekleme
- ✅ **Waveform Görselleştirme** - Timeline üzerinde ses seviyelerini görme
- ✅ **Modern Koyu Tema** - Turuncu aksan renkli şık arayüz
- ✅ **Gelişmiş Liste Görünümü** - Çoklu seçim, durum renkleri ve dosya adı ipuçları (tooltip)
- ✅ **Sağ Tık Menüsü** - Seçili öğeleri toplu işaretleme/sıfırlama
- ✅ **İngilizce Arayüz** - Tüm UI elementleri İngilizce

### Geliştirme Aşamasında
- 🚧 **Sahne Tespiti** - AI tabanlı sahne değişikliği algılama
- 🚧 **Referans Görüntü Eşleştirme** - Intro/outro'yu görüntü benzerliği ile eşleştirme

---

## Group 4: TrimDB (Topluluk Kırpma Veritabanı) 🌐

### Tamamlanan ✅
- [x] **Çevrimdışı MVP** - JSON `.trimrecipe` dosyalarını dışa/içe aktarma
- [x] **Bölüm Eşleştirme** - Süre tabanlı otomatik eşleştirme
- [x] **Backend Altyapısı** - Spring Boot 3 + PostgreSQL sunucu kurulumu ve Docker entegrasyonu

### Planlanan 📋
- [ ] **TrimDB Tarayıcı Arayüzü** - Uygulama içinden online arama ve uygulama
- [ ] **Bulut Depolama** - Cloudflare R2 / S3 entegrasyonu
- [ ] **Topluluk Özellikleri** - Oylama, raporlama ve yorum sistemi

---

## 🖼️ Arayüz Bileşenleri

```
┌─────────────────────────────────────────────────────────────────┐
│  File  Edit  Help                                    [Progress] │
├────────┬─────────────────────────────────────────────┬──────────┤
│        │                                             │          │
│ DOSYA  │          VİDEO OYNATICI                     │  BİLGİ   │
│ LİSTESİ│                                             │          │
│        ├─────────────────────────────────────────────┤  LOG     │
│        │  [S]▂▄█▃▅█▂▄██▃▅█▂▄█▃▅█▂▄██▃▅█▂▄█[E]      │          │
│ KIRPMA │     BAŞLANGIÇ: 00:00  01:32 / 25:32  BİTİŞ │ [KIRP]   │
│ METODU │  [INTRO] [✂] [⏪▶⏩] [✂] [OUTRO]            │[HEPSİNİ] │
└────────┴─────────────────────────────────────────────┴──────────┘
```

---

## 📦 Teknoloji Yığını

| Bileşen | Teknoloji |
|---------|-----------|
| Arayüz Çerçevesi | JavaFX 23 |
| Video İşleme | FFmpeg |
| Derleme Aracı | Maven |
| Programlama Dili | Java 23 |

---

## 🚀 Başlarken

### Gereksinimler
- Java 23+ (JavaFX dahil)
- FFmpeg kurulu ve erişilebilir olmalı

### IDE'den Çalıştırma
1. Projeyi IntelliJ IDEA'da açın
2. `Main.java` dosyasını çalıştırın

### Terminalden Çalıştırma
```bash
mvn javafx:run
```

---

## 📁 Proje Yapısı

```
src/main/java/com/example/massvideocutter/
├── Main.java                    # Uygulama giriş noktası
├── core/
│   ├── TrimFacade.java         # FFmpeg kırpma orkestrasyonu
│   ├── TrimStrategy.java       # Strateji arayüzü
│   ├── ManualTrimStrategy.java # Kullanıcı tanımlı kırpma noktaları
│   ├── AudioAnalyzerStrategy.java # Sessizlik tabanlı kırpma
│   ├── AudioAnalyzer.java      # FFmpeg sessizlik tespiti
│   ├── BatchProcessFacade.java # Çoklu dosya işleme
│   ├── TaskManager.java        # Thread havuzu yönetimi
│   └── ffmpeg/
│       ├── FFmpegWrapper.java  # FFmpeg komut çalıştırma
│       └── FFmpegCommandFactory.java
└── ui/
    ├── MainController.java     # Arayüz mantığı ve bağlantıları
    ├── TimelineControl.java    # Özel timeline + waveform bileşeni
    └── WaveformView.java       # Ses görselleştirme
```

---

## 🎯 Yol Haritası

### Faz 1 ✅ (Tamamlandı)
- [x] İngilizce yerelleştirme
- [x] Sürükle-bırak dosya ekleme
- [x] Modern CSS stilleri
- [x] Hap şeklinde metod seçici butonlar

### Faz 2 ✅ (Tamamlandı)
- [x] Sürüklenebilir timeline markerları (BAŞLANGIÇ/BİTİŞ)
- [x] FFmpeg ile waveform görselleştirme
- [x] Dinamik arayüz (intro/outro yuvalarını göster/gizle)

### Faz 3 🚧 (Devam Ediyor)
- [ ] Sahne tespiti entegrasyonu
- [ ] Referans görüntü eşleştirme
- [ ] Ayarlar/tercihler paneli

### Gelecek
- [ ] Çoklu dil desteği (TR, EN, JP)
- [ ] GPU hızlandırmalı FFmpeg
- [ ] Kırpma ön ayarları için bulut senkronizasyonu

### Aşama 3: Bulut Backend ve Çevrimiçi Kütüphane (TrimDatabase) 🚀 Devam Ediyor 🌐

#### Aşama 1: Yerel Dışa Aktarma/İçe Aktarma (Çevrimdışı) ✅ Tamamlandı** - JSON `.trimrecipe` dosyalarını dışa/içe aktarma
-### Aşama 2: Bölüm Eşleştirme Sistemi ✅ Tamamlandı** - Süre tabanlı otomatik eşleştirme
- [x] **Backend Altyapısı** - Spring Boot 3 + PostgreSQL sunucu kurulumu ve Docker entegrasyonu

#### Planlanan 📋
- [ ] **TrimDB Tarayıcı Arayüzü** - Uygulama içinden online arama ve uygulama
- [ ] **Bulut Depolama** - Cloudflare R2 / S3 entegrasyonu

---

## 🔧 Teknik Detaylar

### Waveform Çıkarımı
FFmpeg kullanılarak video dosyasından ses verisi çıkarılır ve 400 bar halinde görselleştirilir:
- Mono kanal, 8kHz örnekleme
- PEAK + RMS kombinasyonu ile hassas görselleştirme
- Logaritmik ölçekleme ile daha iyi dinamik aralık

### Timeline Kontrolü
Özel `TimelineControl` bileşeni:
- Canvas tabanlı verimli render
- Sürüklenebilir START/END markerları
- Görsel aralık vurgusu
- Playhead senkronizasyonu

---

## 📄 Lisans

MIT Lisansı - Özgürce kullanın ve değiştirin.

---

☕ ve JavaFX ile yapıldı
