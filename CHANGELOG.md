# Changelog

All notable changes to **Custom Sound Alert** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- _Nothing yet._

## [1.1.2] - 2026-06-02

### Fixed
- **Build sounds now fire in Android Studio / Gradle projects.** Builds and
  project syncs run through the Gradle external-system, not the JPS build
  pipeline that `ProjectTaskListener` observes, so failures (including broken
  `build.gradle` / sync-configuration errors) previously played nothing.

### Added
- `GradleBuildListener` on the external-system task events:
  `EXECUTE_TASK` success/failure → build success/failure, and a failed
  `RESOLVE_PROJECT` (sync/configuration error) → build failure.
- Debounce in `SoundNotifier` so the JPS and Gradle listeners can't double-play
  the same result.

## [1.1.1] - 2026-06-02

### Fixed
- Build-failure detection: a failed build that the platform also flags as
  *aborted* is now correctly treated as a failure (errors are checked before
  the aborted flag), so the **Build failure** sound plays.

### Added
- Informational logging on the build/test → notifier → player path (in
  `idea.log`) to make "why didn't my sound play?" easy to diagnose.

## [1.1.0] - 2026-06-02

### Added
- A **Tools → Custom Sound Alert** menu (menu bar) with a **Settings…** action
  and an **Enable Sound Alerts** toggle.

### Changed
- Renamed the settings page from **SoundBuild** to **Custom Sound Alert**
  (under Settings → Tools).

## [1.0.2] - 2026-06-02

### Fixed
- The **Settings → Tools → SoundBuild** page now registers and appears. The
  `<extensions>` element used the wrong attribute (`defaultExtensionPointName`
  instead of `defaultExtensionNs`), which left the namespace empty so the
  `applicationConfigurable` extension was never resolved.

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

[Unreleased]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.2...HEAD
[1.1.2]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/releases/tag/v1.0.0
