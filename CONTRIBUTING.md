# Contributing to MassVideoCutter

Thank you for your interest in contributing! This guide will help you get started.

## 🚀 Getting Started

### Prerequisites
- Java 23+ with JavaFX
- FFmpeg & FFprobe installed and in your PATH
- IntelliJ IDEA (recommended) or any Java IDE

### Building from Source
```bash
git clone https://github.com/taylan1477/MassVCutter.git
cd MassVCutter
mvn clean compile
```

### Running
```bash
mvn javafx:run
```

## 🔧 Development Setup

1. Open the project in IntelliJ IDEA
2. Ensure Java 23 SDK is configured
3. Run `Main.java` directly from the IDE

## 📁 Project Structure

```
src/main/java/io/github/taylan1477/massvideocutter/
├── Main.java              # Application entry point
├── core/                  # Business logic
│   ├── TrimFacade.java    # FFmpeg trim orchestration
│   ├── TrimStrategy.java  # Strategy interface
│   ├── VolumeAnalyzer.java # Volume-based detection
│   └── ffmpeg/            # FFmpeg wrappers & command builders
└── ui/                    # JavaFX controllers & components
    ├── MainController.java
    └── TimelineControl.java
```

## 📝 Code Style

- Use descriptive variable and method names
- Write comments in English
- Follow standard Java naming conventions
- Add JavaDoc for public methods

## 🔀 Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes
4. Test thoroughly
5. Commit with clear messages (`git commit -m "Add: description"`)
6. Push and create a Pull Request

## 🐛 Reporting Issues

- Use GitHub Issues
- Include steps to reproduce
- Include your OS, Java version, and FFmpeg version
- Attach relevant log files from `~/.massvideocutter/logs/`

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.
