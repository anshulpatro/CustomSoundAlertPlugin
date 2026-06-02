# Changelog

All notable changes to **Custom Sound Alert** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- _Nothing yet._

## [1.0.1] - 2026-06-02

### Fixed
- Removed the `until-build` upper bound so the plugin installs on newer IDEs,
  including **Android Studio 2025.3 "Panda"** (platform build 253) and later.
  The plugin uses only stable platform APIs and is verified compatible against
  IntelliJ Platform 241 and 243.

## [1.0.0] - 2026-06-02

### Added
- Custom sounds for **build success** and **build failure** events.
- Custom sounds for **all tests passed** and **one or more tests failed** events.
- Settings screen under **Settings → Tools → SoundBuild** with a path field,
  file chooser, and **Test** button per event.
- Global **enable** toggle and **volume** control.
- WAV (native) and MP3 (bundled decoder) playback.
- Settings persisted across IDE restarts via `PersistentStateComponent`.

[Unreleased]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/releases/tag/v1.0.0
