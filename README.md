# Mass Video Cutter Tool

> **Batch video trimming with intelligent intro/outro detection**

A JavaFX-based desktop application for automatically detecting and removing intros, outros, and unnecessary segments from video archives. Perfect for anime series, TV shows, and movie collections.

## ✨ Features

### Currently Implemented
- **Manual Trim** - Set custom start/end points with draggable timeline markers
- **Audio Analysis (Anime Mode)** - Volume-based intro/outro detection using FFmpeg astats
- **Batch Processing** - Analyze and trim multiple videos with detection caching
- **Drag & Drop** - Import videos by dragging files into the app
- **Waveform Visualization** - Real-time audio visualization on timeline with caching
- **Modern Dark UI** - Clean interface with orange accent theme

### In Development
- **Scene Detection** - AI-based scene change detection
- **Reference Image Matching** - Match intro/outro by image similarity

## 🖼️ UI Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  File  Edit  Help                                    [Progress] │
├────────┬─────────────────────────────────────────────┬──────────┤
│        │                                             │          │
│  FILE  │          VIDEO PLAYER                       │ ANALYSIS │
│  LIST  │                                             │   LOG    │
│        ├─────────────────────────────────────────────┤          │
│ METHOD │  [S]▂▄█▃▅█▂▄██▃▅█▂▄█▃▅█▂▄██▃▅█▂▄█[E]      │ ✓ vid1   │
│        │        START: 01:32     END: 24:00         │ ✓ vid2   │
│ Manual │  [INTRO] [✂] [⏪▶⏩] [✂] [OUTRO]            │          │
│ Audio  │                                             │ [TRIM]   │
│ Scene  │                                             │[TRIM ALL]│
└────────┴─────────────────────────────────────────────┴──────────┘
```

## 📦 Tech Stack

| Component | Technology |
|-----------|------------|
| UI Framework | JavaFX 23 |
| Video Processing | FFmpeg |
| Build Tool | Maven |
| Language | Java 23 |

## 🚀 Getting Started

### Prerequisites
- Java 23+ with JavaFX
- FFmpeg & FFprobe installed and accessible

### Run from IDE
1. Open project in IntelliJ IDEA
2. Run `Main.java`

### Run from Terminal
```bash
mvn javafx:run
```

## 📁 Project Structure

```
src/main/java/com/example/massvideocutter/
├── Main.java
├── core/
│   ├── TrimFacade.java
│   ├── TrimStrategy.java
│   ├── ManualTrimStrategy.java
│   ├── AudioAnalyzerStrategy.java  # Silence + Anime modes
│   ├── VolumeAnalyzer.java         # NEW: Volume-based detection
│   ├── AudioAnalyzer.java
│   ├── BatchProcessFacade.java
│   ├── TaskManager.java
│   └── ffmpeg/
│       ├── FFmpegWrapper.java
│       └── FFmpegCommandFactory.java
└── ui/
    ├── MainController.java
    └── TimelineControl.java        # Waveform + cache
```

---

# 🗺️ Roadmap (2026)

## Group 1: Trimming Methods 🎬

### Completed ✅
- [x] Manual trim with timeline markers
- [x] Silence-based detection
- [x] Volume-based anime intro/outro detection (VolumeAnalyzer)
- [x] Batch analysis with result caching

### Planned 📋
- [ ] **Scene Detection** - FFmpeg scene change filter + AI refinement
- [ ] **Image Matching** - Compare frames with reference intro/outro images
- [ ] **Template Learning** - Learn intro pattern from one episode, apply to all
- [ ] **Audio Fingerprinting** - Match intro music across episodes
- [ ] **Subtitle Detection** - Detect "Opening" / "Ending" subtitle markers

---

## Group 2: User Experience (UX) 🎨

### Completed ✅
- [x] Drag & drop file import
- [x] Modern dark theme with orange accents
- [x] Waveform caching (instant switch)
- [x] Detection result caching
- [x] Inspector log with analysis results
- [x] **Settings Panel** - Custom output folder
- [x] **Keyboard Shortcuts** - Space=play/pause, S=set start, E=set end, etc.

### Planned 📋
- [ ] **Progress Notifications** - System tray notifications for batch completion
- [ ] **Output Preview** - Preview trimmed result before saving
- [ ] **Undo/Redo** - Undo last marker change
- [ ] **Custom Themes** - Light mode, custom accent colors
- [ ] **Multi-Language** - TR, EN, JP language packs
- [ ] **Preset Library** - Save/load trim presets per series
- [ ] **Timeline Zoom** - Zoom in/out on specific sections
- [ ] **Thumbnail Strip** - Show video thumbnails on timeline
- [ ] **List Filtering** - Filter video list by status (Pending, Success, Error)

---

## Group 3: Developer Experience (DX) 🛠️

### Completed ✅
- [x] Strategy Pattern for trim methods
- [x] Facade Pattern for FFmpeg operations
- [x] Debug console logging
- [x] **Error Handling** - Centralized exception handling
- [x] **Logging Framework** - SLF4J / Logback integration

### Planned 📋
- [ ] **API Documentation** - JavaDoc for all public classes
- [ ] **Unit Tests** - JUnit tests for core logic
- [ ] **Integration Tests** - FFmpeg command verification
- [ ] **Version Numbering** - SemVer (1.0.0, 1.1.0, etc.)
- [ ] **Release Workflow** - GitHub Actions for builds
- [ ] **Changelog** - CHANGELOG.md with version history
- [ ] **Contributing Guide** - CONTRIBUTING.md
- [ ] **Code Style** - Checkstyle / EditorConfig

---

## 🌐 TrimDB - Community Trim Database

> **Share your trim work. Skip the detection. Instant batch processing.**

TrimDB is a community-powered system where users can share their intro/outro detection results with others. Once someone has processed an entire series, they can upload their "Trim Recipe" so others don't have to analyze at all.

### How It Works

```
CONTRIBUTOR                         CLOUD                          CONSUMER
┌─────────────┐                 ┌──────────────┐               ┌─────────────┐
│ Analyze     │   ──Upload──▶   │   TrimDB     │   ◀──Search── │ Load videos │
│ 1200 eps    │                 │   Database   │               │             │
│             │                 │              │               │             │
│ One Piece   │                 │ ┌──────────┐ │               │ One Piece   │
│ Ep 001-1200 │                 │ │One Piece │ │   ──Apply──▶  │ Ep 001-1200 │
│             │                 │ │@user ⭐4.9│ │               │             │
│ Upload ──────────────────────▶│ │15K down  │ │               │ INSTANT     │
│             │                 │ └──────────┘ │               │ TRIM!       │
└─────────────┘                 └──────────────┘               └─────────────┘
     Hours of work                 Shared once                   Zero analysis
```

### Trim Recipe Structure (JSON via Jackson)

```json
{
  "version": 1,
  "series": "One Piece",
  "description": "Season 1 to Wano Arc",
  "contributor": "@taylan1477",
  "episodes": 1200,
  "recipe": [
    { "ep": 1, "intro": [0, 90], "outro": [1340, 1430], "duration": 1430.5 },
    { "ep": 2, "intro": [0, 92], "outro": [1338, 1428], "duration": 1428.2 }
  ]
}
```

### Episode Matching

User filenames may differ. TrimDB matches episodes by:

| Method | Reliability | Speed |
|--------|-------------|-------|
| Video Duration (±2s tolerance) | High | ⚡ Instant |
| Episode Number (Regex e.g. E01, - 05) | Medium | ⚡ Instant |
| Combined (Duration + Regex) | Very High | ⚡ Instant |

### Features

- **Upload Recipe** - Share your detection work with the community
- **Search & Apply** - Find recipes by series name
- **Auto-Match** - Match your files to recipe episodes automatically  
- **Rating System** - Upvote accurate recipes
- **Report Errors** - Flag incorrect timings

> 📋 **See [TrimDB Implementation Plan](docs/TRIMDB_ROADMAP.md) for technical details**

---

## 💾 Storage Savings Example

| Series | Episodes | Intro+Outro | Total Saved |
|--------|----------|-------------|-------------|
| One Piece | 1000+ | ~3 min/ep | **~50 hours / 90+ GB** |
| Naruto | 720 | ~2.5 min/ep | **~30 hours / 50+ GB** |
| Ghost in the Shell | 52 | ~3 min/ep | **~3 hours / 5+ GB** |

## 📄 License

MIT License - Free to use and modify.

---

Made with ☕ and JavaFX
