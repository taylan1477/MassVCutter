# TrimDB Uygulama Yol Haritası

> **Mass Video Cutter için Topluluk Destekli Trim Veritabanı**

Bu belge, kullanıcıların intro/outro algılama sonuçlarını toplulukla paylaşmasına ve yeniden kullanmasına olanak tanıyan TrimDB sisteminin teknik uygulama planını özetlemektedir.

---

## 🎯 Vizyon

**Sorun:** 1000'den fazla bölümün intro/outro kısımlarını algılamak saatlerce işlem süresi gerektirir.

**Çözüm:** Bir kullanıcı bir seriyi işlediğinde, "Trim Reçetesini" (Trim Recipe) paylaşabilir; böylece diğerleri herhangi bir analiz yapmadan aynı trim noktalarını anında uygulayabilir.

---

## 📊 Sistem Mimarisi

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            TRIMDB MİMARİSİ                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────────┐   │
│   │   İstemci   │◄───────►│    REST API     │◄───────►│  Veritabanı     │   │
│   │  (JavaFX)   │  HTTPS  │  (Spring Boot)  │   SQL   │  (PostgreSQL)   │   │
│   └─────────────┘         └─────────────────┘         └─────────────────┘   │
│         │                         │                           │             │
│         │                         │                           │             │
│         ▼                         ▼                           ▼             │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────────┐   │
│   │ Yerel Önbellek│       │ Kimlik Doğrulama│         │ Dosya Depolama  │   │
│   │  (SQLite)   │         │  (OAuth2/JWT)   │         │   (S3/MinIO)    │   │
│   └─────────────┘         └─────────────────┘         └─────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Veri Modelleri

### TrimRecipe (Ana Varlık)

```java
public class TrimRecipe {
    Long id;
    String seriesName;           // "One Piece"
    String description;          // "Wano Arc", "Sezon 1", "Bölüm 1-100"
    String contributor;          // "@taylan1477"
    String contributorId;        // UUID
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    int episodeCount;
    double averageRating;        // 0.0 - 5.0
    int downloadCount;
    int reportCount;
    RecipeStatus status;         // BEKLEMEDE, ONAYLANDI, RAPORLANDI
    List<EpisodeTrim> episodes;
}
```

### EpisodeTrim (Bölüm Trim Bilgisi)

```java
public class EpisodeTrim {
    int episodeNumber;
    String episodeTitle;         // İsteğe bağlı
    double videoDuration;        // Eşleştirme için (±2s tolerans)
    double introStart;
    double introEnd;
    double outroStart;
    double outroEnd;
}
```

### UserRating (Kullanıcı Değerlendirmesi)

```java
public class UserRating {
    Long recipeId;
    String userId;
    int rating;                  // 1-5
    String comment;
    LocalDateTime createdAt;
}
```

---

## 🛠️ Uygulama Aşamaları

### Aşama 1: Yerel Dışa Aktarma/İçe Aktarma (Çevrimdışı) ✅ Tamamlandı

**Hedef:** Kullanıcıların herhangi bir backend olmadan trim reçetelerini JSON dosyası olarak dışa/içe aktarmasına izin vermek.

| Görev | Öncelik | Efor |
|-------|----------|--------|
| `TrimRecipe` veri modelini oluştur | P0 | 2s |
| Arayüze "Reçeteyi Dışa Aktar" butonu ekle | P0 | 3s |
| Algılama sonuçlarından JSON oluştur | P0 | 2s |
| "Reçeteyi İçe Aktar" butonu ekle | P0 | 3s |
| JSON'u ayrıştır ve yüklü videolara uygula | P0 | 4s |
| Süreye göre bölüm eşleştirme | P0 | 3s |
| UI: İçe aktarma önizleme diyaloğu | P1 | 4s |

**Teslimat:** Kullanıcılar `.trimrecipe` JSON dosyalarını manuel olarak paylaşabilir (Discord, GitHub, vb.).

---

### Aşama 2: Bölüm Eşleştirme Sistemi ✅ Tamamlandı

**Hedef:** Farklı dosya adlarına sahip olsalar bile kullanıcının video dosyalarını reçetedeki bölümlerle güvenilir bir şekilde eşleştirmek.

| Eşleştirme Yöntemi | Uygulama | Doğruluk |
|-----------------|----------------|----------|
| Süre Eşleştirme | Video süresini ±2s toleransla karşılaştır | %85 |
| Bölüm Regex | Dosya adından bölüm numarasını çıkar (örn. E01, - 05 -) | %80 |
| Birleşik Skor | Süre + Regex'in ağırlıklı kombinasyonu | %99 |

*(Not: Uygulamayı hafif ve hızlı tutmak için Ses Parmak İzi - Audio Fingerprinting - yöntemi bilinçli olarak atlanmıştır).*

```java
public class EpisodeMatcher {
    
    public MatchResult match(File videoFile, List<EpisodeTrim> candidates) {
        double duration = getVideoDuration(videoFile);
        int episodeNum = extractEpisodeNumber(videoFile.getName());
        
        // Her adayı puanla
        for (EpisodeTrim candidate : candidates) {
            double score = 0;
            score += durationMatch(duration, candidate.duration) * 0.5;
            score += episodeMatch(episodeNum, candidate.episodeNumber) * 0.5;
            // ...
        }
    }
}
```

---

### Aşama 3: Bulut Backend ve Çevrimiçi Kütüphane (TrimDatabase) 🚀 Devam Ediyor

**Hedef:** Kullanıcıların trim verilerini çevrimiçi olarak paylaşabileceği, oylayabileceği ve yeniden kullanabileceği, video kırpma verilerinden oluşan açık kaynaklı bir topluluk kütüphanesi oluşturmak.

#### API Uç Noktaları

| Yöntem | Uç Nokta | Açıklama |
|--------|----------|-------------|
| `GET` | `/api/recipes?series={name}` | Çevrimiçi kütüphanede seriye göre reçete ara |
| `GET` | `/api/recipes/{id}` | Reçete detaylarını al ve indir |
| `POST` | `/api/recipes` | Kütüphaneye yeni bir reçete yükle ve paylaş |
| `PUT` | `/api/recipes/{id}` | Reçeteni güncelle |
| `DELETE` | `/api/recipes/{id}` | Kendi reçeteni sil |
| `POST` | `/api/recipes/{id}/rate` | Topluluk kürasyonuna yardımcı olmak için puan ver (1-5 yıldız) |
| `POST` | `/api/recipes/{id}/report` | Yanlış zamanlamaları bildir |
| `GET` | `/api/recipes/popular` | En çok puan alan ve indirilen reçeteler |

#### Teknoloji Yığını

| Bileşen | Teknoloji | Mantık |
|-----------|------------|-----------|
| JSON Formatı| **Jackson** | Açık kaynaklı Java projeleri için endüstri standardı |
| Backend | Spring Boot 3 | Java ekosistemi, kolay entegrasyon, sağlam yapı |
| Veritabanı | PostgreSQL | Güvenilir, hızlı sorgular için JSON desteği |
| Kimlik Doğrulama | OAuth2 (GitHub/Google) | Şifre yönetimi gerektirmez, güvenli |
| Depolama | PostgreSQL JSONB (Başlangıç) -> Cloudflare R2 / S3 (Ölçekli) | Hızlı ve ücretsiz ölçeklendirme |
| Hosting | Render / Railway / Fly.io (Ücretsiz Katman) | 0$ başlangıç maliyeti |

#### Altyapı Evrimi
1. **MVP (0-1000 kullanıcı):** 0$/ay. Render (Backend) + Supabase (Veritabanı). JSON reçeteleri PostgreSQL'de doğrudan `JSONB` olarak saklanır. Kullanıcılar OAuth2 (GitHub/Google/Discord) üzerinden kimlik doğrular.
2. **Büyüme (1000+ kullanıcı):** 5-10$/ay. Hetzner/DigitalOcean VPS'e geçiş. Reçete sayısı milyonlara ulaşırsa depolama Cloudflare R2'ye taşınır.

---

### Aşama 4: Topluluk Özellikleri

| Özellik | Açıklama | Öncelik |
|---------|-------------|----------|
| **Puanlama Sistemi** | Reçete başına 1-5 yıldız oylama | P0 |
| **İndirme Sayacı** | Popülerliği takip et | P0 |
| **Rapor Sistemi** | Yanlış zamanlamaları işaretle | P0 |
| **Yorumlar** | Reçete doğruluğunu tartış | P1 |
| **Katılımcı Profilleri** | Tüm reçeteleri içeren kullanıcı sayfaları | P1 |
| **Reçete Versiyonlama** | Reçeteleri güncelle, geçmişi tut | P2 |
| **Reçete Birleştirme** | Birden fazla kaynağı birleştir | P2 |
| **Otomatik Gönderim** | Analiz sonrası paylaşım için teşvik | P2 |

---

### Aşama 5: Gelişmiş Eşleştirme

| Özellik | Açıklama |
|---------|-------------|
| **Video Hash** | Kesin eşleşme için ilk 5 karenin hash'ini al |
| **Altyazı Algılama** | "Opening" / "Ending" işaretleri için SRT'yi ayrıştır |
| **MAL/AniList Entegrasyonu** | Seri meta verilerini otomatik doldur |
| **Toplu Doğrulama** | Doğruluk için rastgele bölümleri kontrol et |

---

## 🖥️ Arayüz Taslakları

### Reçete Tarayıcı Diyaloğu

```
┌──────────────────────────────────────────────────────────────────┐
│  TrimDB Tarayıcı                                           [X]  │
├──────────────────────────────────────────────────────────────────┤
│  🔍 [Seri adı ara...                                 ] [Ara]    │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  📺 One Piece (Tamamlandı)                                       │
│     Yükleyen: @anime_master  ⭐ 4.9 (1,234 oy)  📥 15,432       │
│     Bölümler: 1-1200  |  Güncelleme: 10.01.2026                 │
│     [Videolarıma Uygula]  [Önizle]  [Detaylar]                  │
│  ─────────────────────────────────────────────────────────────  │
│  📺 One Piece (Wano Arc)                                        │
│     Yükleyen: @wano_fan  ⭐ 4.7 (342 oy)  📥 2,341              │
│     Bölümler: 890-1088  |  Güncelleme: 20.12.2025                 │
│     [Uygula]  [Önizle]  [Detaylar]                              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📅 Zaman Çizelgesi

| Aşama | Süre | Hedef |
|-------|----------|--------|
| Aşama 1: Yerel İçe/Dışa Aktarma | 2 hafta | v1.1.0 |
| Aşama 2: Bölüm Eşleştirme | 2 hafta | v1.2.0 |
| Aşama 3: Bulut Backend | 4 hafta | v2.0.0 |
| Aşama 4: Topluluk Özellikleri | 3 hafta | v2.1.0 |
| Aşama 5: Gelişmiş Eşleştirme | Süreçte | v2.x |

---

## 🔒 Güvenlik Hususları

1. **Hız Sınırlama (Rate Limiting)** - Spam yüklemeleri önle
2. **İçerik Denetimi** - Raporlanan reçeteleri incele
3. **Kullanıcı Doğrulama** - Yükleme için OAuth girişi zorunluluğu
4. **Veri Doğrulama** - Tüm girdileri temizle
5. **Yalnızca HTTPS** - Tüm API iletişimi şifreli

---

## 🚀 Başlangıç (Geliştirme)

### Aşama 1 Uygulama Sırası

1. `TrimRecipe.java` - Veri modeli
2. `TrimRecipeExporter.java` - JSON serileştirme
3. `TrimRecipeImporter.java` - JSON ayrıştırma
4. `EpisodeMatcher.java` - Süre tabanlı eşleştirme
5. UI diyalogları (Dışa Aktar, İçe Aktar, Uygula)

---

*Bu belge uygulama ilerledikçe güncellenecektir.*
