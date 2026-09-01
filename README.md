# Utilities Scarce

A client-side Fabric mod for **Minecraft 26.2** with six utility modules.
Everything is configurable from Mod Menu and every module has its own toggle hotkey.

## Modules

### Auto Totem
Puts a fresh totem back into the exact slot the last one left.

Every tick the mod records which hand slots hold a totem. When a slot that held one
is suddenly empty — which is what a pop looks like from the client's side — a
replacement is moved in from the rest of the inventory, back into that same hotbar
slot or into the offhand. With the default `SWAP` method that is a single container
click, so a refill costs one packet.

Notable settings: separate offhand/hotbar toggles, refill delay and cooldown,
`Keep in reserve` (leave the last N totems alone), `Only after a pop` (require recent
damage, so shuffling totems by hand is ignored), and `SWAP` vs `PICKUP` click style.

### Stun Slam
Axe into mace. When you hit a target that is holding its shield up, the mod swaps to
an axe, breaks the shield, swaps to a mace and lands the follow-up.

By default the mace hit waits for the attack cooldown to recharge, because a mace
swung on a cold cooldown does a fraction of its damage. Turn `Wait for attack cooldown`
off for a faster but much weaker follow-up. If the hit that triggered the sequence was
already an axe hit, the axe step is skipped.

### Shield Disable
Breaks a raised shield with an axe and goes straight back to the item you were holding.

`ON_ATTACK` (default) reacts to your own hit; `AUTO` fires as soon as a shielded target
is under the crosshair. Stun Slam already disables shields itself, so Shield Disable
stands down in `AUTO` mode while Stun Slam is enabled.

### Breach Swap
Hit something with a sword or axe and the mace finishes it: swaps to a Breach-enchanted
mace, lands one hit, and returns to the weapon that started the trade.

`Require Breach` and `Minimum Breach level` control which mace qualifies; the trigger
weapons (sword, axe) can be enabled independently.

### Free Cam
Detaches the camera from your body without moving your body.

The player entity is never touched — it keeps its position and, crucially, its pitch
and yaw. So if you were aiming down mining a shaft when you turned Free Cam on,
**holding the attack key keeps breaking that same column** while you fly around
looking at something else. Everything the game aims — block breaking, placing,
attacks, the block outline — still comes off the body, because vanilla ray-traces
from the player and the player has not moved.

Two things have to be taken away from the body for that to hold. Movement keys are
cut off by swapping the player's input handler for a blank one that never reads the
keyboard; the real handler is put back on exit. Mouse look is intercepted and applied
to the camera instead. The body is left standing exactly where you left it, still
subject to gravity and still mining.

WASD flies the camera, jump/sneak go up and down, sprint applies the speed multiplier.
`Fly along look direction` is off by default so movement stays level and you can look
down at your body while flying sideways. `Max distance from body` leashes the camera
(0 is unlimited — past the loaded chunks there is nothing to see). `Snap back on damage`
is on by default, so a mob or a lava pocket drops you back into your body rather than
letting you die watching scenery.

### Fast Anchor
Charges a respawn anchor the moment you place one, then puts a totem back in your hand.

Placement is confirmed by looking for the block rather than assuming it landed, so a
cancelled placement does nothing. One glowstone charge is enough to make the anchor
explode; `Swap to` chooses what ends up in your hand afterwards (totem, another anchor,
glowstone, or the slot you started on). `Only where anchors explode` skips the Nether,
where an anchor sets your spawn instead.

The mod never detonates the anchor — that is still your click.

## Packet behaviour

Every module runs its steps through one scheduler with a shared per-tick budget
(`Max actions per tick`, default 2). Steps that produce a packet are counted; local
steps such as picking a hotbar slot are free. Hotbar changes rely on the game's own
held-item sync, which is flushed before the next attack or use, so a swap-and-hit still
lands with the new item without any extra packets of our own.

Free Cam is the one module that needs mixins: the camera position and mouse look have
no event hooks. It uses two, both narrow — one on `Camera` (place the camera, and turn
off occlusion culling, which is computed from the player's position and would otherwise
hide most of what you flew out to look at) and one on `MouseHandler` (send look input to
the camera). The other five modules use Fabric API events only.

Sequences from different modules share a single "hand" lane, so a new sequence cancels
the previous one rather than two modules fighting over the hotbar. Priority on an
attack is Stun Slam, then Shield Disable, then Breach Swap.

Inventory moves are only sent while no screen is open, so the mod never clicks into a
container you have open or fights a stack you are dragging.

## Hotkeys

Defaults sit on the numeric keypad, which vanilla leaves unbound. Rebind them in
**Options → Controls → Utilities Scarce**.

| Key | Action |
| --- | --- |
| `Keypad 0` | Open settings |
| `Keypad 1` | Toggle Auto Totem |
| `Keypad 2` | Toggle Stun Slam |
| `Keypad 3` | Toggle Shield Disable |
| `Keypad 4` | Toggle Breach Swap |
| `Keypad 5` | Toggle Fast Anchor |
| `Keypad 6` | Toggle Free Cam |

Toggling writes straight to the config file, so hotkeys and the settings screen always
agree.

## Requirements

| | |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.158.0+26.2 |
| Java | 25 |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | optional — needed for the settings screen |
| [Mod Menu](https://modrinth.com/mod/modmenu) | optional — adds the Config button in the mod list |

Both optional mods are compile-only. Without them the mod still runs and the hotkeys
still work; you just edit `config/utilitiesscarce.json` by hand instead.

Only Auto Totem is enabled out of the box. The other five default to off — turn on what
you want in the settings screen or with the hotkeys.

## Building

```sh
./gradlew build
```

The jar lands in `build/libs/`. Use the one **without** the `-sources` suffix.

`./gradlew runClient` starts a dev client with Mod Menu and Cloth Config already on the
classpath.

Version numbers all live in `gradle.properties`; check
[fabricmc.net/develop](https://fabricmc.net/develop) when moving to a new Minecraft
version. Cloth Config is pinned as `26.2.+` because it publishes as
`<mc version>.<build number>` — pin the exact patch if you want a reproducible build.

## Fair warning

These modules automate combat actions. Plenty of servers treat that as cheating
regardless of how few packets it sends. Check the rules of anywhere you play before
turning them on.

## Licence

MIT — see [LICENSE](LICENSE).
