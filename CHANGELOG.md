# Changelog

All notable changes to **Custom Sound Alert** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- _Nothing yet._

## [1.0.0] - 2026-06-02

### Added
- Custom sounds for **build success** and **build failure** events.
- Custom sounds for **all tests passed** and **one or more tests failed** events.
- Settings screen under **Settings → Tools → SoundBuild** with a path field,
  file chooser, and **Test** button per event.
- Global **enable** toggle and **volume** control.
- WAV (native) and MP3 (bundled decoder) playback.
- Settings persisted across IDE restarts via `PersistentStateComponent`.

[Unreleased]: https://github.com/anshulpatro/custom-sound-alert/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/anshulpatro/custom-sound-alert/releases/tag/v1.0.0
