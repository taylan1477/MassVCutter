# Mass Video Cutter Tool

> **Batch video trimming with intelligent intro/outro detection**

A JavaFX-based desktop application for automatically detecting and removing intros, outros, and unnecessary segments from video archives. Perfect for anime series, TV shows, and movie collections.

## ✨ Features

### Currently Implemented
- **Manual Trim** - Set custom start/end points with draggable timeline markers
- **Audio Analysis** - Automatic silence detection for intro/outro boundaries
- **Batch Processing** - Trim multiple videos with the same settings
- **Drag & Drop** - Import videos by dragging files into the app
- **Waveform Visualization** - See audio levels on the timeline
- **Modern Dark UI** - Clean interface with orange accent theme

### In Development
- **Scene Detection** - AI-based scene change detection
- **Reference Image Matching** - Match intro/outro by image similarity

## 🖼️ UI Components

```
┌─────────────────────────────────────────────────────────────────┐
│  File  Edit  Help                                    [Progress] │
├────────┬─────────────────────────────────────────────┬──────────┤
│        │                                             │          │
│  FILE  │          VIDEO PLAYER                       │  INFO    │
│  LIST  │                                             │          │
│        ├─────────────────────────────────────────────┤  LOG     │
│        │  [S]▂▄█▃▅█▂▄██▃▅█▂▄█▃▅█▂▄██▃▅█▂▄█[E]      │          │
│  TRIM  │        START: 00:00  01:32 / 25:32  END    │ [TRIM]   │
│ METHOD │  [INTRO] [✂] [⏪▶⏩] [✂] [OUTRO]            │[TRIM ALL]│
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
- FFmpeg installed and accessible

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
├── Main.java                    # App entry point
├── core/
│   ├── TrimFacade.java         # FFmpeg trim orchestration
│   ├── TrimStrategy.java       # Strategy interface
│   ├── ManualTrimStrategy.java # User-defined trim points
│   ├── AudioAnalyzerStrategy.java # Silence-based trim
│   ├── AudioAnalyzer.java      # FFmpeg silence detection
│   ├── BatchProcessFacade.java # Multi-file processing
│   ├── TaskManager.java        # Thread pool management
│   └── ffmpeg/
│       ├── FFmpegWrapper.java  # FFmpeg command execution
│       └── FFmpegCommandFactory.java
└── ui/
    ├── MainController.java     # UI logic & bindings
    ├── TimelineControl.java    # Custom timeline + waveform
    └── WaveformView.java       # Audio visualization
```

## 🎯 Roadmap

### Phase 1 ✅
- [x] English localization
- [x] Drag & drop file import
- [x] Modern CSS styling
- [x] Pill-style method selector

### Phase 2 ✅
- [x] Draggable timeline markers (START/END)
- [x] Waveform visualization with FFmpeg
- [x] Dynamic UI (show/hide intro/outro slots)

### Phase 3 🚧
- [ ] Scene detection integration
- [ ] Reference image matching
- [ ] Settings/preferences panel

### Future
- [ ] Multi-language support (TR, EN, JP)
- [ ] GPU-accelerated FFmpeg
- [ ] Cloud sync for trim presets

## 💾 Storage Savings Example

| Series | Episodes | Intro+Outro | Total Saved |
|--------|----------|-------------|-------------|
| One Piece | 1000+ | ~3 min/ep | **~50 hours / 90+ GB** |
| Naruto | 720 | ~2.5 min/ep | **~30 hours / 50+ GB** |

## 📄 License

MIT License - Free to use and modify.

---

Made with ☕ and JavaFX
