# HyunseoRPG

HyunseoRPG is an experimental Minecraft RPG expansion plugin for **Paper 26.1.2**.

The plugin project is now **development-frozen**. Its current repository is kept as a buildable reference implementation, live-test artifact, and migration source for future mod development.

## Status

- Plugin development: **frozen / discontinued**
- Target server: **Paper 26.1.2**
- Java: **25**
- Build system: **Gradle**
- Latest development release: **v0.1.0-dev.1**
- Live validation: **partial / ongoing**

The repository may contain experimental or balance-unverified gameplay systems. A successful build does not imply that every gameplay path has been fully verified on a live survival server.

## Core direction

HyunseoRPG was designed to extend vanilla Minecraft rather than replace it with a separate RPG ruleset.

The final plugin architecture emphasizes:

- vanilla-compatible equipment and progression
- custom enchantments integrated with vanilla enchanting/anvils
- custom and modified mobs
- elemental and special equipment
- alchemy built around the vanilla Brewing Stand
- Paper-native event and persistence systems
- preservation of vanilla gameplay wherever possible

Several legacy systems were removed during the final architecture cleanup.

## Building

Requirements:

- JDK 25
- Git

Clone the repository and run:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The resulting plugin JAR is created under:

```text
build/libs/
```

The Gradle build also verifies that the bundled datapack resources required by the plugin are present in the final JAR.

## Running

Use a compatible **Paper 26.1.2** server.

1. Build the project or download a JAR from GitHub Releases.
2. Place the JAR in the server's `plugins/` directory.
3. Start the server.
4. Review the generated HyunseoRPG configuration before testing.

This development snapshot should be tested in a disposable or backed-up world first.

## Testing

The development release includes a dedicated test-command guide:

**GitHub Releases → v0.1.0-dev.1 → `RELEASE_TEST_GUIDE-v0.1.0-dev.1.md`**

The guide covers the current administrative test paths, including:

- `/rpg give`
- basic and custom equipment IDs
- custom enchantment books
- `/rpgtest`
- `/rpgmob spawn`
- `/rpgmob spawncustom`
- special-equipment test commands

Some content can exist and function internally without having a complete natural survival acquisition path in this frozen plugin version.

## Repository layout

```text
src/main/java/        Plugin implementation
src/main/resources/   Plugin configuration and bundled data
src/test/             Automated tests
docs/                 Design, migration, and implementation notes
gradle/                Gradle wrapper
.github/workflows/    Continuous integration
```

Historical reports and migration notes may remain in the repository for development archaeology. They are not necessarily descriptions of the final gameplay design.

## Continuous integration

Pull requests and pushes to `main` run a clean Gradle build and JUnit test suite on Java 25.

## Contributing

This repository primarily exists as a frozen reference implementation rather than an actively developed plugin.

Bug reports and narrowly scoped fixes are still welcome, but large new plugin features are unlikely to be accepted because future development is intended to move away from this Paper implementation.

Please avoid including server credentials, private configuration, player data, world data, or access tokens in issues or pull requests.

## Security

Do not publish credentials, tokens, private server addresses, player databases, or other sensitive runtime data in issues.

See [SECURITY.md](SECURITY.md) for vulnerability-reporting guidance.

## License

This project is licensed under the [MIT License](LICENSE).

Minecraft is a trademark of Microsoft. This project is an independent community project and is not affiliated with or endorsed by Mojang Studios or Microsoft.
