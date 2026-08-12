package com.periut.retrocommands.command.argument;

import com.periut.retrocommands.command.argument.VanillaIds.VanillaItem;
import com.periut.retrocommands.optionaldep.retroapi.RetroApiCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Turns what a player typed into an item id.
 *
 * <p>{@link VanillaIds} is the mod's own table and always answers for the {@code minecraft}
 * namespace, so identifiers work on a plain b1.7.3 install. StationAPI and RetroAPI are consulted
 * only for namespaces the mod does not own, which is exactly what they add.
 */
public final class ItemIds {
    private ItemIds() {
    }

    /** Resolves a token to an item and subtype, or null when nothing answers to that name. */
    public static VanillaItem resolve(final String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            return new VanillaItem(Integer.parseInt(token), 0);
        } catch (final NumberFormatException ignored) {
            // Not a raw id, so it must be a name.
        }

        final String lower = token.toLowerCase(Locale.ROOT);
        final int separator = lower.indexOf(':');
        final String namespace = separator < 0 ? VanillaIds.NAMESPACE : lower.substring(0, separator);
        final String path = separator < 0 ? lower : lower.substring(separator + 1);

        if (VanillaIds.NAMESPACE.equals(namespace)) {
            return VanillaIds.byName(path);
        }

        if (FabricLoader.getInstance().isModLoaded("station-registry-api-v0")) {
            final int id = stationId(token);
            if (id != -1) {
                return new VanillaItem(id, 0);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("retroapi")) {
            final int id = RetroApiCompat.identifierToItemId(token);
            if (id != -1) {
                return new VanillaItem(id, 0);
            }
        }

        return null;
    }

    /** The name to show a player for an id, always namespaced. */
    public static String nameOf(final int id) {
        final String identifier = identifierOf(id);
        return identifier == null ? String.valueOf(id) : identifier;
    }

    /**
     * The identifier an id is registered under - {@code minecraft:stone},
     * {@code somemod:copper_ingot} - or null if nothing claims it.
     *
     * <p>The mod's own table answers for vanilla; a modded id is looked up in whichever registry
     * put it there. This is what lets a modded item still be named on a dedicated server, where no
     * translation table exists to ask.
     */
    public static String identifierOf(final int id) {
        final String vanilla = VanillaIds.nameOf(id);
        if (vanilla != null) {
            return VanillaIds.NAMESPACE + ":" + vanilla;
        }

        final net.minecraft.item.Item item = itemAt(id);
        if (item == null) {
            return null;
        }

        if (FabricLoader.getInstance().isModLoaded("station-registry-api-v0")) {
            final String identifier = stationIdentifierOf(item);
            if (identifier != null) {
                return identifier;
            }
        }
        if (FabricLoader.getInstance().isModLoaded("retroapi")) {
            return RetroApiCompat.identifierOf(item);
        }

        return null;
    }

    /**
     * Reaching {@code Item.ITEMS} runs the item and block class initialisers, and those failing
     * raises an Error rather than an exception - never worth taking a command down for.
     */
    private static net.minecraft.item.Item itemAt(final int id) {
        try {
            if (id < 0 || id >= net.minecraft.item.Item.ITEMS.length) {
                return null;
            }
            return net.minecraft.item.Item.ITEMS[id];
        } catch (final RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static String stationIdentifierOf(final net.minecraft.item.Item item) {
        try {
            final var identifier = net.modificationstation.stationapi.api.registry.ItemRegistry.INSTANCE.getId(item);
            return identifier == null ? null : identifier.namespace.toString() + ":" + identifier.path;
        } catch (final RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    /** Every identifier worth suggesting, namespaced as modern Minecraft writes them. */
    public static List<String> allIdentifiers() {
        final TreeSet<String> identifiers = new TreeSet<>();

        for (final String name : VanillaIds.names()) {
            identifiers.add(VanillaIds.NAMESPACE + ":" + name);
        }

        if (FabricLoader.getInstance().isModLoaded("station-registry-api-v0")) {
            identifiers.addAll(stationIdentifiers());
        }
        if (FabricLoader.getInstance().isModLoaded("retroapi")) {
            for (final String identifier : RetroApiCompat.itemIdentifiers()) {
                // The vanilla namespace is ours to describe; anything else is theirs.
                if (!identifier.startsWith(VanillaIds.NAMESPACE + ":")) {
                    identifiers.add(identifier);
                }
            }
        }

        return new ArrayList<>(identifiers);
    }

    /** StationAPI types stay behind this call so they are only loaded when the mod is present. */
    private static int stationId(final String identifier) {
        try {
            final Optional<net.minecraft.item.Item> item = net.modificationstation.stationapi.api.registry.ItemRegistry.INSTANCE
                .getOrEmpty(net.modificationstation.stationapi.api.util.Identifier.of(identifier));
            return item.map(value -> value.id).orElse(-1);
        } catch (final RuntimeException | LinkageError ignored) {
            return -1;
        }
    }

    private static List<String> stationIdentifiers() {
        final List<String> identifiers = new ArrayList<>();
        try {
            for (final net.modificationstation.stationapi.api.util.Identifier id
                : net.modificationstation.stationapi.api.registry.ItemRegistry.INSTANCE.getIds()) {
                final String namespace = id.namespace.toString();
                if (!VanillaIds.NAMESPACE.equals(namespace)) {
                    identifiers.add(namespace + ":" + id.path);
                }
            }
        } catch (final RuntimeException | LinkageError ignored) {
        }
        return identifiers;
    }
}
