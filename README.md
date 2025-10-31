# NoDamageAnimation

Forge 1.12.2 client-side utility mod that instantly toggles the inventory screen on any incoming damage, auto-issues `/ext` whenever the player catches fire, and fires off `/fix all` once equipped armor drops below half durability.

## Building

```sh
./gradlew build
```

Always invoke the included wrapper scripts rather than a locally installed Gradle. The build enforces Gradle 4.9 (ForgeGradle 3's supported version) and will download the missing `gradle/wrapper/gradle-wrapper.jar` automatically on first use.

The generated mod JAR is located in `build/libs/`.
