# Utilities Scarce for Minecraft 1.21.11

The 1.21.11 port, built as a standalone Gradle project so it can use its own
toolchain without disturbing the 26.2 build at the repository root.

```
./gradlew -p versions/1.21.11 build
```

The jar lands in `versions/1.21.11/build/libs/`.

## Why this build differs from the 26.2 one

1.21.11 ships an **obfuscated** client; 26.2 does not. Loom 1.17 has one plugin
for each case, and they are not interchangeable:

| | plugin | mappings | mod dependencies |
| --- | --- | --- | --- |
| 26.2 (root) | `net.fabricmc.fabric-loom` | none — names in the jar are already the real ones | `implementation` |
| 1.21.11 (here) | `net.fabricmc.fabric-loom-remap` | `loom.officialMojangMappings()` | `modImplementation` |

Using the non-remapping plugin here is the mistake worth naming, because it does
not fail loudly: the build configures, then javac reports that nothing under
`net.minecraft` exists, since it was handed the obfuscated classes.

Java 21 rather than 25, to match what 1.21.11 runs on.

## API differences from the 26.2 source

The two source trees are otherwise the same mod. What changed:

- `ClickType` and `handleInventoryMouseClick` instead of `ContainerInput` and
  `handleContainerInput`
- `Minecraft.screen` / `Minecraft.setScreen` instead of `Minecraft.gui.screen()`
  / `Minecraft.gui.setScreen()`
- `KeyBindingHelper` (`fabric-key-binding-api-v1`) instead of
  `KeyMappingHelper`
- `Camera.setup(Level, Entity, boolean, boolean, float)` instead of
  `Camera.update(DeltaTracker)`
- `WorldRenderEvents.BEFORE_TRANSLUCENT`, drawing through a `MultiBufferSource`,
  instead of `LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES` and its submit-node
  collector
- a `SectionOcclusionGraph` mixin for seeing through chunks, which 26.2 gets
  from `CameraRenderState.smartCull`

## No settings screen here

Cloth Config builds the settings screen on 26.2. This build leaves it out, so
`ConfigScreens.available()` returns false and the Mod Menu entry has no config
button; the modules are still fully configurable by editing
`config/utilitiesscarce.json`, and every one of them is still bound to a
hotkey.
