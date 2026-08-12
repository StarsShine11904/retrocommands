# Retro Commands for b1.7.3

Modern Minecraft's command system and chat window, backported to Beta 1.7.3.

+ A real [Brigadier](https://github.com/Mojang/brigadier) dispatcher: typed arguments, entity
  selectors, per-argument completions, and the same error messages modern Minecraft gives.
+ A purpose-built chat screen on the chat key, with a text field that has selection and a clipboard,
  live command colouring, a completion window, and sent-message history.
+ A chat window with a hundred lines of scrollback, word wrapping, hover tooltips and clickable text.
+ Works in singleplayer and on servers; a server shares its command tree with clients that have the
  mod, and stays readable to those that do not.
+ No required dependencies.

## Commands

`/give` `/clear` `/tp` (`/teleport`) `/kill` `/killall` `/time` `/weather` `/toggledownfall`
`/summon` `/seed` `/say` `/me` `/msg` (`/tell`, `/w`) `/list` `/help`
`/god` `/heal` `/hat` `/ride` `/warp` `/noclip` `/whoami` `/clock` `/id` `/mobs` `/mods` `/clearchat`
`/op` `/deop` `/kick` `/ban` `/ban-ip` `/pardon` `/pardon-ip` `/whitelist` `/save-all` `/save-on`
`/save-off` `/stop` `/tpa`
`/gamemode` (with BHCreative) `/reloadcryonicconfig` (with Cryonic Config)

Where modern Minecraft has a command of the same name, its syntax wins:

```
/give <targets> <item> [count]      /give Steve minecraft:stone 64
/clear [targets] [item] [maxCount]  clears an inventory - the old chat-clearing /clear is now /clearchat
/tp <targets> <location>            /tp @a ~ ~10 ~
/kill [targets]                     /kill @e[type=Creeper,distance=..20]
/time set day|noon|night|midnight
```

Items are named, in the mod's own `minecraft:` registry - no other mod required:

```
/give @s minecraft:iron_ingot 5
/give @s minecraft:red_wool          a subtype, named the way modern names it
/give @s minecraft:wool:14           the same thing, by metadata
/give @s minecraft:spawner 1 Creeper a spawner with a mob in it
```

StationAPI and RetroAPI, when present, add their own namespaces on top; they are never needed for
the vanilla set. Older beta names (`planks`, `rose`, `raw_fish`) still resolve.

Selectors work as they do in modern: `@p @a @r @e @s`, with `type`, `name`, `distance`, `limit`,
`sort`, `x/y/z` and `dx/dy/dz`.

Run `/help` in game for the generated list, or `/help <command>` for one command's forms.

## Chat

| | |
|---|---|
| Chat key | opens the screen |
| `Tab` | complete, or list what the command accepts |
| `↑` `↓` | walk sent-message history, or move through completions |
| `Ctrl+A/C/X/V` | select all, copy, cut, paste (`Cmd` on macOS) |
| `Ctrl+←/→` | move by word; hold `Shift` to select |
| `Home` `End` | jump to either end |
| Wheel, `PgUp` `PgDn` | scroll the chat window |
| Click a message | runs, suggests or copies whatever that text carries |

## API

Retro Commands vendors Brigadier under its own package name, `com.mojang.brigadier`, so command
code written against modern Minecraft compiles unchanged.

`build.gradle`:

```gradle
repositories {
    maven { url "https://jitpack.io/" }
}

dependencies {
    modImplementation('com.github.matthewperiut:retrocommands:0.8.0') {
        transitive false
    }
}
```

`fabric.mod.json` - please keep it optional:

```json
"suggests": {
    "retrocommands": "*"
}
```

Register during your mod's initialisation:

```java
if (FabricLoader.getInstance().isModLoaded("retrocommands")) {
    MyCommands.register();
}
```

```java
import com.mojang.brigadier.Command;
import com.periut.retrocommands.api.CommandRegistrationCallback;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retrocommands.command.RetroCommandManager.argument;
import static com.periut.retrocommands.command.RetroCommandManager.literal;
import static com.periut.retrocommands.command.argument.EntityArgumentType.getPlayers;
import static com.periut.retrocommands.command.argument.EntityArgumentType.players;

public final class MyCommands {
    public static void register() {
        CommandRegistrationCallback.register((dispatcher, environment) ->
            dispatcher.register(literal("sparkle")
                .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
                .then(argument("targets", players())
                    .then(argument("amount", integer(1, 64))
                        .executes(context -> {
                            context.getSource().sendFeedback(
                                Text.literal("Sparkled " + getPlayers(context, "targets").size() + " players"));
                            return Command.SINGLE_SUCCESS;
                        })))));
    }
}
```

The callback runs every time a tree is built - once per world on a client, once per start on a
server. `environment` says which side is asking, so a command that only makes sense on a server can
skip the other.

Argument types worth knowing: `EntityArgumentType.player()/players()/entity()/entities()`,
`ItemArgumentType.item()`, `BlockPosArgumentType.blockPos()`, `Vec3ArgumentType.vec3()`,
`TimeArgumentType.time()`, `MessageArgumentType.message()`, `DimensionArgumentType.dimension()`,
`EntitySummonArgumentType.entitySummon()`, plus all of Brigadier's own.

For a custom argument type to survive being sent to a client, register a serializer for it:

```java
ArgumentTypes.registerCustom("mymod:thing", ThingArgumentType.class, buffer -> ThingArgumentType.thing());
```

Without one it still works; clients simply treat it as a single word when completing.

### Summonable entities

`SummonRegistry.add(MyEntity.class, (world, position, arguments) -> ..., "[my option]")` gives an
entity type summon-time options. Anything not registered is still summonable.

### Migrating from the old API

The `Command` interface and `CommandRegistry.add` are gone, along with `SharedCommandSource` and
`PosParse`. A command that used to look like this:

```java
public class Heal implements Command {
    public void command(SharedCommandSource source, String[] parameters) { ... }
    public String name() { return "heal"; }
}
```

becomes a registration callback. `parameters[n]` becomes a typed argument, `name()` becomes the
literal, `needsPermissions()` becomes `.requires(...)`, `suggestion(...)` is replaced by the
argument type's own completions (or `.suggests(...)` for something specific), and `manual(...)` is
no longer written by hand - `/help` generates it from the tree.

## Optional dependencies

+ **StationAPI** - `/tp` between dimensions, item identifiers from its registry, and mod item names
  in command output (client-side; its lang support is client-only, as beta's is)
+ **RetroAPI** - item identifiers from its registry, and the same for mod item names
+ **BHCreative** - `/gamemode`
+ **Cryonic Config** - `/reloadcryonicconfig`

## Building

```
./gradlew build          # compiles, remaps, and runs the test suite
./gradlew retroTest      # just the tests
./gradlew runClient      # play with it
```

The test suite is plain Java with a `main`, not JUnit, so it needs nothing beyond what the mod
already compiles against. See `docs/PARITY.md` for how closely each feature tracks modern Minecraft.
