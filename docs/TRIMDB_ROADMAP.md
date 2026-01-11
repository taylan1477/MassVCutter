# TrimDB Implementation Roadmap

> **Community-Powered Trim Database for Mass Video Cutter**

This document outlines the technical implementation plan for TrimDB - a system that allows users to share and reuse intro/outro detection results across the community.

---

## 🎯 Vision

**Problem:** Detecting intros/outros for 1000+ episodes takes hours of processing time.

**Solution:** Once one user processes a series, they can share their "Trim Recipe" so others can apply the same trim points instantly without any analysis.

---

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            TRIMDB ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────────┐   │
│   │   Client    │◄───────►│   REST API      │◄───────►│   Database      │   │
│   │  (JavaFX)   │  HTTPS  │  (Spring Boot)  │   SQL   │  (PostgreSQL)   │   │
│   └─────────────┘         └─────────────────┘         └─────────────────┘   │
│         │                         │                           │             │
│         │                         │                           │             │
│         ▼                         ▼                           ▼             │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────────┐   │
│   │ Local Cache │         │  Auth Service   │         │  File Storage   │   │
│   │  (SQLite)   │         │  (OAuth2/JWT)   │         │   (S3/MinIO)    │   │
│   └─────────────┘         └─────────────────┘         └─────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Data Models

### TrimRecipe (Main Entity)

```java
public class TrimRecipe {
    Long id;
    String seriesName;           // "One Piece"
    String contributor;          // "@anime_master"
    String contributorId;        // UUID
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    int episodeCount;
    double averageRating;        // 0.0 - 5.0
    int downloadCount;
    int reportCount;
    RecipeStatus status;         // PENDING, APPROVED, FLAGGED
    List<EpisodeTrim> episodes;
}
```

### EpisodeTrim

```java
public class EpisodeTrim {
    int episodeNumber;
    String episodeTitle;         // Optional
    double videoDuration;        // For matching
    String audioFingerprint;     // First 5s audio hash
    double introStart;
    double introEnd;
    double outroStart;
    double outroEnd;
}
```

### UserRating

```java
public class UserRating {
    Long recipeId;
    String oderId;
    int rating;                  // 1-5
    String comment;
    LocalDateTime createdAt;
}
```

---

## 🛠️ Implementation Phases

### Phase 1: Local Export/Import (Offline) 🎯 MVP

**Goal:** Allow users to export/import trim recipes as JSON files without any backend.

| Task | Priority | Effort |
|------|----------|--------|
| Create `TrimRecipe` data model | P0 | 2h |
| Add "Export Recipe" button to UI | P0 | 3h |
| Generate JSON from detection results | P0 | 2h |
| Add "Import Recipe" button | P0 | 3h |
| Parse JSON and apply to loaded videos | P0 | 4h |
| Episode matching by duration | P0 | 3h |
| UI: Import preview dialog | P1 | 4h |

**Deliverable:** Users can share `.trimrecipe` JSON files manually (Discord, GitHub, etc.)

---

### Phase 2: Episode Matching System

**Goal:** Reliably match user's video files to recipe episodes even with different filenames.

| Matching Method | Implementation | Accuracy |
|-----------------|----------------|----------|
| Duration Match | Compare video duration ±2s | 70% |
| Episode Regex | Extract episode number from filename | 60% |
| Audio Fingerprint | Chromaprint hash of first 5 seconds | 95% |
| Combined Score | Weighted combination of methods | 98% |

```java
public class EpisodeMatcher {
    
    public MatchResult match(File videoFile, List<EpisodeTrim> candidates) {
        double duration = getVideoDuration(videoFile);
        String fingerprint = getAudioFingerprint(videoFile, 0, 5);
        int episodeNum = extractEpisodeNumber(videoFile.getName());
        
        // Score each candidate
        for (EpisodeTrim candidate : candidates) {
            double score = 0;
            score += durationMatch(duration, candidate.duration) * 0.3;
            score += episodeMatch(episodeNum, candidate.episodeNumber) * 0.2;
            score += fingerprintMatch(fingerprint, candidate.fingerprint) * 0.5;
            // ...
        }
    }
}
```

---

### Phase 3: Cloud Backend (Online)

**Goal:** Central database for storing and sharing recipes.

#### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/recipes?series={name}` | Search recipes by series |
| `GET` | `/api/recipes/{id}` | Get recipe details |
| `POST` | `/api/recipes` | Upload new recipe |
| `PUT` | `/api/recipes/{id}` | Update recipe |
| `DELETE` | `/api/recipes/{id}` | Delete own recipe |
| `POST` | `/api/recipes/{id}/rate` | Rate a recipe |
| `POST` | `/api/recipes/{id}/report` | Report incorrect recipe |
| `GET` | `/api/recipes/popular` | Top downloaded recipes |

#### Tech Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Backend | Spring Boot 3 | Java ecosystem, easy integration |
| Database | PostgreSQL | Reliable, JSON support |
| Auth | OAuth2 (GitHub/Google) | No password management |
| Storage | MinIO / S3 | For large recipe files |
| Cache | Redis | For popular recipes |
| Hosting | Railway / Render | Free tier for MVP |

---

### Phase 4: Community Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Rating System** | 1-5 star ratings per recipe | P0 |
| **Download Counter** | Track popularity | P0 |
| **Report System** | Flag incorrect timings | P0 |
| **Comments** | Discuss recipe accuracy | P1 |
| **Contributor Profiles** | User pages with all recipes | P1 |
| **Recipe Versioning** | Update recipes, keep history | P2 |
| **Merge Recipes** | Combine multiple sources | P2 |
| **Auto-Submit** | Prompt to share after analysis | P2 |

---

### Phase 5: Advanced Matching

| Feature | Description |
|---------|-------------|
| **Video Hash** | Hash first 5 frames for exact match |
| **Subtitle Detection** | Parse SRT for "Opening" / "Ending" markers |
| **MAL/AniList Integration** | Auto-fill series metadata |
| **Batch Verification** | Spot-check random episodes for accuracy |

---

## 🖥️ UI Mockups

### Recipe Browser Dialog

```
┌──────────────────────────────────────────────────────────────────┐
│  TrimDB Browser                                            [X]  │
├──────────────────────────────────────────────────────────────────┤
│  🔍 [Search series name...                           ] [Search] │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  📺 One Piece (Complete)                                        │
│     By: @anime_master  ⭐ 4.9 (1,234 ratings)  📥 15,432        │
│     Episodes: 1-1200  |  Updated: 2026-01-10                    │
│     [Apply to My Videos]  [Preview]  [Details]                  │
│  ─────────────────────────────────────────────────────────────  │
│  📺 One Piece (Wano Arc)                                        │
│     By: @wano_fan  ⭐ 4.7 (342 ratings)  📥 2,341               │
│     Episodes: 890-1088  |  Updated: 2025-12-20                  │
│     [Apply]  [Preview]  [Details]                               │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Export Recipe Dialog

```
┌──────────────────────────────────────────────────────────────────┐
│  Export Trim Recipe                                        [X]  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Series Name: [One Piece                                     ]  │
│  Your Name:   [@anime_master                                 ]  │
│                                                                  │
│  Episodes to export: 1200                                       │
│  Intro detected: 1198 (99.8%)                                   │
│  Outro detected: 1195 (99.6%)                                   │
│                                                                  │
│  ☑ Include audio fingerprints (recommended)                    │
│  ☐ Upload to TrimDB (requires login)                           │
│                                                                  │
│           [Cancel]  [Export to File]  [Upload & Share]          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📅 Timeline

| Phase | Duration | Target |
|-------|----------|--------|
| Phase 1: Local Import/Export | 2 weeks | v1.1.0 |
| Phase 2: Episode Matching | 2 weeks | v1.2.0 |
| Phase 3: Cloud Backend | 4 weeks | v2.0.0 |
| Phase 4: Community Features | 3 weeks | v2.1.0 |
| Phase 5: Advanced Matching | Ongoing | v2.x |

---

## 🔒 Security Considerations

1. **Rate Limiting** - Prevent spam uploads
2. **Content Moderation** - Review flagged recipes
3. **User Verification** - OAuth login required to upload
4. **Data Validation** - Sanitize all input
5. **HTTPS Only** - All API communication encrypted

---

## 📝 File Format Specification

### `.trimrecipe` File Format (v1)

```json
{
  "version": 1,
  "series": "One Piece",
  "contributor": "@anime_master",
  "created": "2026-01-11T07:00:00Z",
  "episodeCount": 1200,
  "episodes": [
    {
      "ep": 1,
      "title": "I'm Luffy!",
      "duration": 1430.5,
      "intro": { "start": 0, "end": 90 },
      "outro": { "start": 1340, "end": 1430 },
      "fingerprint": "AQADtJKYRFI..."
    }
  ]
}
```

---

## 🚀 Getting Started (Development)

### Phase 1 Implementation Order

1. `TrimRecipe.java` - Data model
2. `TrimRecipeExporter.java` - JSON serialization
3. `TrimRecipeImporter.java` - JSON parsing
4. `EpisodeMatcher.java` - Duration-based matching
5. UI dialogs (Export, Import, Apply)

---

*This document will be updated as implementation progresses.*
