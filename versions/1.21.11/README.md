# Utilities Scarce for Minecraft 1.21.11

This is the 1.21.11 port of the mod. **It does not currently build**, and the
reason is upstream rather than in this source.

## What is wrong

Stock Minecraft 1.21.11 ships **non-obfuscated**. Every Fabric Loom version in
the 1.17 line knows that, and therefore

- refuses `loom.officialMojangMappings()` — "Cannot use Mojang mappings in a
  non-obfuscated environment", because the names in the jar already are those
  names; and
- refuses access wideners declared in the `intermediary` namespace, wanting the
  `official` one instead.

The only Fabric API ever published for 1.21.11 is `0.141.6+1.21.11`, frozen
since July 2026, and several of its modules — `fabric-biome-api-v1` among them —
declare their access wideners in `intermediary`. Cloth Config's 1.21.11 release
does the same.

So the two published artifacts you need cannot be used together under Loom 1.17:

```
Expected official namespace for access widener entry,
found: intermediary in mod: fabric-biome-api-v1
```

Fabric API has a `1.21.11_unobfuscated` branch (`0.139.5`) built for the
official namespace, but it targets a separate experimental Minecraft artifact
called `1.21.11_unobfuscated`, not the 1.21.11 that players run.

## What was tried

| Loom | Result |
| --- | --- |
| `1.17-SNAPSHOT` (1.17.20) | rejects the mappings line; then rejects Fabric API's access wideners |
| `1.17.7` | same, so the whole 1.17 line behaves this way |
| `1.16-SNAPSHOT` | still fails |

Dropping Cloth Config removed one offender but not Fabric API itself, which is
not optional.

## What would fix it

Any one of these, none of which this repository controls:

- a Fabric API release for stock 1.21.11 with official-namespace access
  wideners;
- a Loom version that accepts intermediary access wideners for a
  non-obfuscated game;
- targeting 1.21.10 or earlier instead, which predates the transition.

## The code itself

The port is complete and is not the blocker. It is the 26.2 source with the
1.21.11 API differences applied: `ClickType` and `handleInventoryMouseClick`,
`Minecraft.screen` and `setScreen`, `KeyBindingHelper`, `Camera.setup` instead
of `Camera.update`, `WorldRenderEvents` drawing into a `MultiBufferSource`
instead of the submit-node collector, and a `SectionOcclusionGraph` mixin for
seeing through chunks. None of it has ever been compiled, because the build
fails before reaching it.
