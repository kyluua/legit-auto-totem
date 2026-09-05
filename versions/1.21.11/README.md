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

## Optional integrations

Same as 26.2: Mod Menu puts a Config button next to the mod, and Cloth Config
builds the screen behind it. Both are compile-only, and neither is touched
until `ConfigScreens` has confirmed the mod is loaded, so the mod runs without
them -- with the settings then living in `config/utilitiesscarce.json`. The
hotkeys work either way.

Cloth Config numbers its releases `<mc version without the leading 1.>.<build>`,
so the 1.21.11 line is `21.11.x` where 26.2's is `26.2.x`.
