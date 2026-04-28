# Changelog

All notable changes to MassVideoCutter will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.6.0] - 2026-04-26

### Added
- **Manual Trim** — Set custom start/end points with draggable timeline markers
- **Audio Analysis (Anime Mode)** — Volume-based intro/outro detection using FFmpeg astats
- **Batch Processing** — Analyze and trim multiple videos with detection result caching
- **Drag & Drop** — Import videos by dragging files directly into the application
- **Waveform Visualization** — Real-time audio waveform display on timeline with caching
- **Modern Dark UI** — Clean dark theme with orange accent colors
- **Multi-Format Support** — MP4, MKV, TS, AVI, MOV, WebM, FLV
- **FFmpeg Command Builders** — Strategy + Builder pattern for format-specific trim commands
- **Inspector Log** — Real-time analysis results and trim progress display

### Architecture
- Strategy Pattern for swappable trim methods (Manual, Audio, Scene)
- Facade Pattern for FFmpeg operations (`TrimFacade`, `BatchProcessFacade`)
- Builder Pattern for format-specific FFmpeg commands
- Thread pool management via `TaskManager`

---

## Pre-Release History

### 2026-01-11 — Audio Analyzer Mastered
- Completed VolumeAnalyzer with anime-optimized intro/outro detection
- Added batch analysis with concurrent result caching

### 2026-01-05 — Timeline & Waveform
- Implemented custom TimelineControl with integrated waveform
- Added WaveformView with Canvas-based rendering
- Waveform caching for instant video switching

### 2026-01-04 — Development Resumed
- Project transferred to new development machine

### 2025-05-12 — Audio Analysis
- Implemented silence-based audio detection
- Added AudioAnalyzer and AudioAnalyzerStrategy
- Progress tracking with ProgressUpdater

### Earlier — Foundation
- Initial video trimming with FFmpeg
- ListView-based file management
- Multi-format remux support (MP4, MKV, TS)
