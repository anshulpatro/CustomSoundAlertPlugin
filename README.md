# Custom Sound Alert

A plugin for **Android Studio** and **IntelliJ IDEA** that plays a sound of your
choosing when a build or test run finishes — so you can look away and still know
what happened.

![Settings → Tools → Custom Sound Alert](docs/settings.png)

---

## Features

| Event | When it fires |
|-------|---------------|
| **Build success** | A build / make / rebuild completes with no errors |
| **Build failure** | A build completes with one or more errors |
| **Test success**  | A test run finishes and every test passed |
| **Test failure**  | A test run finishes with one or more failures/errors |

- Per-event sound file, configured under **Settings → Tools → Custom Sound Alert**.
- **WAV** supported natively; **MP3** supported via a bundled decoder.
- A **Test** button beside every field to preview the sound (using the current,
  unsaved volume).
- Global **enable** switch and **volume** control.
- Sounds play **asynchronously** — the IDE is never blocked, there are no popups,
  and there is no dialog spam.
- Settings are persisted and **survive IDE restarts**.

## Installation

### From the JetBrains Marketplace (once published)
1. **Settings → Plugins → Marketplace**.
2. Search for **Custom Sound Alert**.
3. Click **Install** and restart the IDE.

### From a local build
1. Build the distributable ZIP:
   ```bash
   ./gradlew buildPlugin
   ```
   The artifact is written to `build/distributions/custom-sound-alert-<version>.zip`.
2. In the IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and select
   that ZIP.

## Usage

1. Open **Settings → Tools → Custom Sound Alert**.
2. Tick **Enable Custom Sound Alert**.
3. For each event, click **Browse…**, pick a `.wav` or `.mp3` file, and use
   **Test** to preview it.
4. Adjust the **Volume** (0–100) and click **OK**.

---

## Development setup

### Requirements
- **JDK 17** (the build's Java toolchain target).
- No local Gradle install needed — use the bundled wrapper (`./gradlew`).

> The Gradle launcher itself runs best on JDK 17. If your default `java` is
> newer, point Gradle at JDK 17, e.g.:
> ```bash
> ./gradlew build -Dorg.gradle.java.home="$(/usr/libexec/java_home -v 17)"
> ```
> (or set `JAVA_HOME` accordingly).

### Common tasks
| Command | Purpose |
|---------|---------|
| `./gradlew build` | Compile everything and run the unit tests |
| `./gradlew test` | Run the JUnit 5 test suite |
| `./gradlew runIde` | Launch a sandbox IDE with the plugin installed |
| `./gradlew buildPlugin` | Produce the installable plugin ZIP |
| `./gradlew verifyPlugin` | Run the JetBrains Plugin Verifier |

### Project layout
```
src/main/kotlin/com/soundbuild/
├── model/        SoundEvent, PlaybackResult/PlaybackError (domain types)
├── util/         AudioSupport (pure audio decode + gain helpers)
├── services/     SoundPlayerService (async player), SoundNotifier (event → sound)
├── listeners/    BuildEventListener, TestEventListener (IDE event detection)
├── settings/     SoundSettingsState (persistence), Configurable + Component (UI glue)
└── ui/           SoundRow (reusable path + browse + test widget)
src/main/resources/META-INF/plugin.xml
src/test/kotlin/...   JUnit 5 unit, service, and persistence tests
```

### Platform & Java version
The project targets **Java 17** (per requirement) and therefore builds against
**IntelliJ Platform 2024.1 (build 241)** — the latest platform line that still
compiles against Java 17. IntelliJ Platform **2024.2+ (242+) mandates Java 21**.
To move to a newer platform, bump `platformVersion` in `gradle.properties`, set
the Java/Kotlin toolchains to `21` in `build.gradle.kts`, and raise `sinceBuild`.
Because the plugin uses only stable platform APIs, the declared compatibility
range still extends through current releases (`until-build = 252.*`).

### Architecture notes
- **Event detection uses modern, non-deprecated APIs.** Builds are observed via
  `com.intellij.task.ProjectTaskListener`; test runs via the SMTRunner
  framework (`SMTRunnerEventsListener` / `SMTRunnerEventsAdapter`). Both are
  declared as `<projectListeners>` so the platform manages their lifecycle.
- **Separation of concerns.** `SoundPlayerService` owns only threading and
  lifecycle; the actual decoding/playback lives in the stateless `AudioSupport`,
  which is why the audio logic is unit-testable without an IDE. `SoundNotifier`
  is a thin façade so listeners never touch settings or the player directly
  (dependency inversion).
- **Light services.** `SoundPlayerService` and `SoundSettingsState` use the
  `@Service` annotation and are therefore *not* registered in `plugin.xml`.
- **MP3 discovery.** `javax.sound.sampled.AudioSystem` discovers SPI providers
  through the system class loader, which cannot see a plugin's bundled jars, so
  the MP3 reader/converter from `mp3spi` are instantiated **explicitly** rather
  than via service discovery. WAV uses the JDK's built-in providers.

## Testing

Tests are written with **JUnit 5** (`./gradlew test`):
- `AudioSupportTest` — format detection, volume→dB mapping, gain clamping,
  validation, and graceful failure on missing/corrupt files.
- `SoundPlayerServiceTest` — service threading/lifecycle (blank paths ignored,
  no exceptions escape, safe disposal) using a synchronous executor.
- `SoundSettingsStatePersistenceTest` — `getState`/`loadState` round-trip,
  defaults, volume clamping, and path trimming.

---

## Publishing to the JetBrains Marketplace

1. **Create the plugin listing** once at
   <https://plugins.jetbrains.com> and obtain a **permanent upload token**.
2. **Prepare signing keys** (required by the Marketplace):
   ```bash
   openssl genpkey -aes-256-cbc -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096
   openssl req -key private.pem -new -x509 -days 3650 -out chain.crt
   ```
3. **Provide secrets via environment variables** (never commit them):
   | Variable | Meaning |
   |----------|---------|
   | `CERTIFICATE_CHAIN` | Path to `chain.crt` |
   | `PRIVATE_KEY` | Path to `private.pem` |
   | `PRIVATE_KEY_PASSWORD` | Password for the private key |
   | `PUBLISH_TOKEN` | Marketplace upload token |
4. **Verify, sign, and publish:**
   ```bash
   ./gradlew verifyPlugin
   ./gradlew signPlugin
   ./gradlew publishPlugin
   ```

### Versioning strategy
This project follows [Semantic Versioning](https://semver.org):
- **MAJOR** — incompatible changes (e.g. dropped IDE versions).
- **MINOR** — new, backward-compatible features.
- **PATCH** — backward-compatible bug fixes.

Bump `pluginVersion` in `gradle.properties`, move the `Unreleased` section of
[CHANGELOG.md](CHANGELOG.md) under the new version, and update the
`<change-notes>` in `plugin.xml`. Keep the `since-build`/`until-build` range in
`build.gradle.kts` current with the IDE releases you have verified against.

## License
[MIT](LICENSE) © 2026 Anshul Patro
