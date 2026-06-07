# Changelog

All notable changes to **Custom Sound Alert** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- _Nothing yet._

## [1.4.0] - 2026-06-04

### Added
- **Per-event on/off toggles.** Each event (build success, build failure, test
  success, test failure) now has its own checkbox on the settings screen, so you
  can silence just one — e.g. keep failure sounds but mute success — without
  disabling the whole plugin. When an event is unchecked, its path field and
  Test button are disabled and no sound (not even the built-in default) plays
  for it.

### Changed
- **Default is now failures-only.** Out of the box, only build failure and test
  failure play; success events are off by default (opt in per event) to avoid a
  sound on every green build.

## [1.3.1] - 2026-06-04

### Added
- **Built-in default sound for success events.** A bundled success clip
  (`sounds/success.mp3`) now plays on build success and test success out of the
  box, until the user picks their own file.

### Changed
- All four events now have a built-in default (previously only failures did), so
  success builds/tests are audible without any configuration.
- Gradle detection now plays the success/failure sound on **every Gradle build
  and project-sync result** (Android Studio runs both through the Gradle
  external-system, not the JPS build pipeline). The notifier debounces so a
  single action sounds once.

## [1.2.0] - 2026-06-02

### Added
- **Built-in default sound for failure events.** A bundled sound now plays on
  build failure and test failure out of the box, until the user picks their own
  file. Success events remain silent unless configured.
- `SoundPlayerService.playResource(...)` and `AudioSupport.playResource(...)`
  to play sounds bundled on the classpath; the **Test** button previews the
  default when a field is left blank.

## [1.1.3] - 2026-06-02

### Changed
- The **Tools → Custom Sound Alert** entry is now anchored to the **top** of the
  Tools menu (like ADB Idea) instead of the bottom, so it's easy to find.

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

[Unreleased]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.3.1...v1.4.0
[1.3.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.2.0...v1.3.1
[1.2.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.3...v1.2.0
[1.1.3]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/anshulpatro/CustomSoundAlertPlugin/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/anshulpatro/CustomSoundAlertPlugin/releases/tag/v1.0.0
