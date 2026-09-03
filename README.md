# Utilities Scarce

A client-side Fabric mod for **Minecraft 26.2** with eight utility modules.
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

### ESP
Draws a box around every configured entity and block, through terrain by default.

### Tracer
Draws a line from the viewer to every configured entity and block. Lines start just in
front of the camera by default, which keeps them clear of the near clip plane and makes
them read as fanning out from the crosshair; `EYES` and `FEET` are the alternatives.
When Free Cam is running, crosshair lines follow the free camera's aim rather than the
body's.

Both take their targets from one shared **Targets** section — entity categories
(players, hostile mobs, passive mobs, dropped items, everything else) and a list of
block ids. Deciding "what to highlight" once
means the two can never disagree about it, and the block sweep only has to run once no
matter how many modules are drawing. Each module then owns only its own presentation:
which of the two kinds to draw, the colours, the line width, and whether to draw through
walls.

Block ids are plain strings like `minecraft:ancient_debris`; anything unparseable or
unknown is skipped rather than breaking the rest of the list.

**Range** follows your render distance by default — detect as far as you can see. That
is exactly right for entities, which are cheap to filter and only exist near you anyway.
Blocks follow it too but stop at `Block radius limit` (48 by default), because a sweep
covers the *cube* of its radius: at a 32-chunk view that would be tens of millions of
positions per pass and a refresh measured in minutes, which is slower than useless for
finding ore. Turn the toggle off to set both ranges by hand.

**Colour** is per module, and separate for entities and blocks. `STATIC` uses the RGBA
picker; `RAINBOW` walks the hue wheel on a wall-clock timer, so it animates at the same
rate whatever your frame rate. `Rainbow spread` offsets each target's hue a little so a
crowd reads as a gradient rather than one flat colour, and `Rainbow opacity` sets alpha
for that mode.

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

Flying uses your own movement keybinds — the camera is driven by the player's input
handler rather than by a second set of controls, so rebinding forward/back/left/right
in Options → Controls moves the camera too, and toggle-sneak and controller mods carry
over. Jump and sneak go up and down, sprint applies the speed multiplier.
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
(`Max actions per tick`, default 2). **Everything that reaches the server counts against
it, hotbar switches included** — the game syncs the held item at the end of the tick, so
a switch costs a packet just as much as an attack does. Hotbar changes still ride that
built-in sync rather than sending anything of our own, so a swap-and-hit lands with the
new item at no extra cost.

Three things that used to waste packets are gone:

- **Pulling a tool out of storage** sends a container click that only pays off next tick,
  and the module bails out meanwhile. An auto-triggering module would send one every
  tick for as long as the tool stayed out of the hotbar — twenty a second, going
  nowhere. Pulls are now rate-limited.
- **Auto Totem** would re-fire while its refill click was still unconfirmed, turning one
  pop into a burst. It now waits for the slot to settle before trying again.
- **Fast Anchor** swung after every charge. Vanilla swings only when the interaction
  asks for it, so that was a spare packet per charge and an animation on clicks the
  server had refused.

ESP and Tracer draw through Fabric's `LevelRenderEvents`, so they need no mixins
either. They do register two line render types of their own, built from vanilla's line
snippet so no shader assets ship with the mod; the only difference between the two is
that the through-walls one drops the depth state.

Scanning for blocks is the expensive half, so its cost is bounded rather than left to
grow with the radius. The sweep walks a linear cursor through the volume, stops when it
has spent its per-tick budget of position checks, and resumes there next tick. Cost per
tick is therefore flat no matter how far the search reaches — only the time to finish a
pass grows. Results are swapped in only when a pass completes, so what gets drawn is
always a complete sweep rather than a half-built one. `Max highlighted blocks` caps the
result so a vein-rich area cannot stall a frame, and the sweep does not run at all
unless a module is actually drawing blocks.

Entity boxes are not interpolated between ticks, so a fast-moving target's box can trail
it by up to a tick.

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
| `Keypad 7` | Toggle ESP |
| `Keypad 8` | Toggle Tracer |

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

Only Auto Totem is enabled out of the box. The other seven default to off — turn on what
you want in the settings screen or with the hotkeys.

## Building

```sh
./gradlew build
```

The jar lands in `build/libs/`. Use the one **without** the `-sources` suffix. You need
**JDK 25** — Minecraft 26.2 compiles at release 25, so an older JDK will not do it.

Every push also builds on CI (`.github/workflows/build.yml`). If you would rather not
set up a toolchain, open the repository's **Actions** tab, pick the latest run for your
branch, and download the `utilities-scarce` artifact — the jar is inside.

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
