# Parity with modern Minecraft

How closely each part of this backport tracks modern Minecraft (26.2-era), written by working
through modern's behaviour feature by feature and checking what this mod actually does.

Every row is one of:

- **Match** - behaves as modern does.
- **Divergence** - deliberately different, because beta cannot do what modern does. The reason is
  given; these are not bugs.
- **Gap** - modern has it, beta could have it, and this mod does not yet.

---

## Command dispatch

| Feature | Status | Notes |
|---|---|---|
| Brigadier parser, tree, builders | Match | Reimplemented from the public API under `com.mojang.brigadier`; redirects, forks and `getSmartUsage` included |
| Literals beat arguments when both parse | Match | `getRelevantNodes` prefers an exact literal |
| Best-parse selection among candidates | Match | Fully-consumed parses first, then error-free ones |
| `Unknown command` / `Incorrect argument for command` | Match | Brigadier's own wording |
| Error position with `<--[HERE]` | Match | Same slice width (10 characters) and same red underline |
| Failed command is clickable to retype | Match | The error line carries a `suggest_command` |
| Permission levels gate parsing and completion | Match | `requires` on every node; the client tree is trimmed per player |
| `/execute`, `/data`, `/function`, macros | Divergence | These need NBT, storage and a data-pack system beta has none of |
| Command result / success chaining | Divergence | Return values work; nothing reads them, as there is no `/execute store` |

## Argument types

| Feature | Status | Notes |
|---|---|---|
| bool, integer, long, float, double, string | Match | Including range checks and their error messages |
| Item by identifier | Match | `minecraft:stone`, bare `stone`, and modded namespaces |
| Item subtypes | Divergence | Modern gives each its own id; beta stores a damage value, so `minecraft:red_wool` and `minecraft:wool:14` both work |
| Item NBT (`{Enchantments:...}`) | Divergence | Beta stacks carry no NBT to write |
| Block position, `~` relative | Match | |
| `^` local coordinates | Match | Same basis from yaw and pitch |
| Whole numbers centre in their block | Match | For `/tp`, as modern does |
| Time with `t`/`s`/`d` suffixes | Match | |
| Message with selectors substituted | Match | `/say hello @a` names everyone |
| Game mode | Divergence | Survival and creative only; beta has no adventure or spectator |
| Dimension | Divergence | Needs StationAPI, which is what gives beta more than two |
| Custom types survive to the client | Match | Via a registered serializer; unknown types degrade to a single word |

## Entity selectors

| Feature | Status | Notes |
|---|---|---|
| `@p @a @r @e @s` | Match | Including the default sort and limit each implies |
| `type`, `name`, `distance`, `limit`, `sort` | Match | `distance` accepts `n`, `a..b`, `a..`, `..b` |
| `x/y/z`, `dx/dy/dz` | Match | Volume anchored at the origin, negative sizes extend backwards |
| Negation (`type=!player`) | Match | |
| An explicit limit implies nearest-first | Match | |
| `player()` rejects a multi-target selector | Match | With modern's wording |
| `tag`, `scores`, `advancements`, `predicate`, `nbt` | Divergence | Beta has none of these systems |
| `gamemode`, `level` | Divergence | No experience levels; game mode belongs to another mod |
| `x_rotation`, `y_rotation` | Gap | Beta has the angles; nothing reads them yet |

## Chat input

| Feature | Status | Notes |
|---|---|---|
| Cursor, selection anchor, shift-selection | Match | |
| `Ctrl+A/C/X/V` | Match | Also `Cmd` on macOS |
| `Ctrl+←/→` word movement, `Ctrl+Backspace/Delete` | Match | Same "skip spaces, then the word" rule |
| `Home` / `End` | Match | |
| Horizontal scrolling when text outruns the box | Match | |
| 256-character limit | Match | |
| Sent-message history on `↑`/`↓` | Match | Draft is preserved when you walk into history |
| Clipboard paste | Match | Through beta's own reader |
| Clipboard copy | Divergence | LWJGL 2 cannot write the clipboard, so AWT does it; silently no-ops on a headless JVM |
| Opening chat with `/` pre-filled | Gap | Beta binds no such key, and the key loop cannot be hooked cleanly; the chat key then typing `/` is one extra keystroke |

## Completions

| Feature | Status | Notes |
|---|---|---|
| Re-parse and re-suggest on every edit | Match | |
| Per-argument colour cycling | Match | Aqua, yellow, green, light purple, gold - modern's order |
| Unparsable remainder in red | Match | |
| Error message under the field | Match | |
| `Tab` completes; `Tab` with nothing to complete lists usages | Match | |
| `↑`/`↓` through the window, `Enter` accepts | Match | |
| Mouse click and wheel in the window | Match | |
| Ghost text preview of the highlighted entry | Match | Shown as soon as the window opens |
| Scrollbar past ten entries | Match | |
| Suggestion tooltips | Match | Selectors carry theirs, as modern's do |
| Window placed under the text it replaces | Match | Clamped to stay on screen |
| Position completions grow a coordinate at a time | Match | `~`, `~ ~`, `~ ~ ~`; local `^` parses but is not suggested, as in modern. A trailing space counts as a finished coordinate, not an empty one |
| Namespaces offered before ids | Divergence | With nothing typed, an item argument offers `minecraft:` and any other namespace rather than every id at once, so one Tab gets past the prefix. Once something is typed, modern's rule takes over and a bare path such as `sto` finds `minecraft:stone` |
| A completion replaces the token being typed | Match | Covered by a regression test - offsetting to the token's end instead appends |
| Client and server suggestions merged | Divergence | The server's answer replaces the local one rather than merging; on a vanilla server the local tree is used |

## Chat window

| Feature | Status | Notes |
|---|---|---|
| 100 lines of scrollback | Match | |
| Wheel and `PgUp`/`PgDn` scrolling | Match | `Ctrl` scrolls by a page |
| Word wrapping | Match | Breaks at a space, falls back mid-word |
| Fade after 200 ticks | Match | Same squared curve |
| 10 lines closed, 20 open | Match | |
| Scroll bar with an unread marker | Match | |
| Hover tooltips (`show_text`) | Match | |
| Click to run, suggest, copy, open a URL | Match | |
| `show_item` / `show_entity` hovers | Divergence | Beta stacks carry nothing worth describing |
| Chat width, height, opacity, scale options | Gap | Fixed at modern's defaults; beta has no options screen entry for them |
| Message signing, reporting, deletion | Divergence | Nothing in beta to sign or report |

## Text components

| Feature | Status | Notes |
|---|---|---|
| Literal and translatable content, siblings, style inheritance | Match | |
| Full RGB colour | Match | Drawn directly; not limited to the sixteen codes |
| Bold, underline, strikethrough, obfuscated | Match | Drawn by the mod, since beta's font has no code for them |
| Italic | Gap | Would need to skew glyphs; beta's font renderer draws axis-aligned quads |
| Click and hover events, insertion | Match | |
| Component JSON round-trip | Match | Reads what modern would send, minus what beta cannot express |
| Legacy `§` conversion both ways | Match | Colours snap to the nearest of the sixteen on the way out |
| Item display names in command output | Match | `Gave 1 [Apple] to Steve`, uncoloured, identifier and id on hover |
| Modded item names | Match on a client | StationAPI and RetroAPI both inject their mod lang into beta's `TranslationStorage`, which step one reads, so a modded item shows its own localised name. Neither can do so on a dedicated server - both reach through the client-only translation class - so there the identifier is spelled out instead (`somemod:copper_ingot` becomes "Copper Ingot") |
| Names resolve on a dedicated server | Divergence | Beta ships its lang file with the client only. Three sources are tried - the game's table, the lang file read off the classpath, then the identifier spelled out (`iron_ingot` becomes "Iron Ingot") - so a name always appears, and metadata subtypes get real names (`Red Wool`) that beta's own lang file cannot express |

## Multiplayer

| Feature | Status | Notes |
|---|---|---|
| Server sends its command tree on join | Match | Trimmed to that player's permissions, as modern's packet is |
| Tree carries argument types and their properties | Match | Round-tripped in the test suite |
| Redirects survive the trip | Match | Index-based, so an alias costs one number |
| Async completion requests | Match | Ids let a stale answer be dropped |
| Rich command output to clients with the mod | Match | Falls back to `§` text otherwise |
| Vanilla clients keep working | Match | They see plain coloured chat |
| Tree resent when permissions change | Match | `/op` and `/deop` push a freshly trimmed tree to that player |

---

## Not verified here

Two things this pass could not do, both for the same reason - the machine it was written on has no
outbound network:

- **The game was never launched.** Loom downloads a version manifest before it can configure, so
  `runClient` could not start. Everything was compiled against the mapped jar with a direct
  `javac` classpath, every mixin target was checked to exist in that jar by name and descriptor,
  and the logic that can run without a game runs in the test suite - but nobody has watched the
  chat screen open. Run `./gradlew runClient` to do that.
- **No diff against the 26.2 source.** mcsrc.dev could not be reached, so modern's behaviour here is
  reproduced from knowledge of that codebase rather than read off it. The rows above are the
  honest result of that comparison; a source diff would tighten them further.
