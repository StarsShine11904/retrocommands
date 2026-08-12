package com.periut.retrocommands.network;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.periut.retrocommands.command.argument.BlockPosArgumentType;
import com.periut.retrocommands.command.argument.DimensionArgumentType;
import com.periut.retrocommands.command.argument.EntityArgumentType;
import com.periut.retrocommands.command.argument.EntitySummonArgumentType;
import com.periut.retrocommands.command.argument.GameModeArgumentType;
import com.periut.retrocommands.command.argument.ItemArgumentType;
import com.periut.retrocommands.command.argument.MessageArgumentType;
import com.periut.retrocommands.command.argument.TimeArgumentType;
import com.periut.retrocommands.command.argument.Vec3ArgumentType;
import net.ornithemc.osl.networking.api.PacketBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Names argument types so a command tree can cross the network, the job modern Minecraft's
 * {@code ArgumentTypeInfos} registry does.
 *
 * <p>A type the client has never heard of is read back as a single word. That keeps an unknown
 * argument from breaking the parse of everything after it: the client still colours the command and
 * still offers completions, and the server - the only side that has to be right - is unaffected.
 */
public final class ArgumentTypes {
    /** Reads and writes whatever a type needs beyond its name, such as an integer's bounds. */
    public interface Serializer<T extends ArgumentType<?>> {
        default void write(final T type, final PacketBuffer buffer) {
        }

        T read(PacketBuffer buffer);
    }

    private record Entry<T extends ArgumentType<?>>(String id, Class<T> type, Serializer<T> serializer) {
    }

    private static final Map<String, Entry<?>> BY_ID = new LinkedHashMap<>();
    private static final Map<Class<?>, Entry<?>> BY_CLASS = new LinkedHashMap<>();

    private ArgumentTypes() {
    }

    static {
        register("brigadier:bool", BoolArgumentType.class, buffer -> BoolArgumentType.bool());
        register("brigadier:integer", IntegerArgumentType.class,
            (type, buffer) -> {
                buffer.writeInt(type.getMinimum());
                buffer.writeInt(type.getMaximum());
            },
            buffer -> IntegerArgumentType.integer(buffer.readInt(), buffer.readInt()));
        register("brigadier:long", LongArgumentType.class,
            (type, buffer) -> {
                buffer.writeLong(type.getMinimum());
                buffer.writeLong(type.getMaximum());
            },
            buffer -> LongArgumentType.longArg(buffer.readLong(), buffer.readLong()));
        register("brigadier:float", FloatArgumentType.class,
            (type, buffer) -> {
                buffer.writeFloat(type.getMinimum());
                buffer.writeFloat(type.getMaximum());
            },
            buffer -> FloatArgumentType.floatArg(buffer.readFloat(), buffer.readFloat()));
        register("brigadier:double", DoubleArgumentType.class,
            (type, buffer) -> {
                buffer.writeDouble(type.getMinimum());
                buffer.writeDouble(type.getMaximum());
            },
            buffer -> DoubleArgumentType.doubleArg(buffer.readDouble(), buffer.readDouble()));
        register("brigadier:string", StringArgumentType.class,
            (type, buffer) -> buffer.writeByte(type.getType().ordinal()),
            buffer -> switch (buffer.readByte()) {
                case 0 -> StringArgumentType.word();
                case 2 -> StringArgumentType.greedyString();
                default -> StringArgumentType.string();
            });

        register("retrocommands:entity", EntityArgumentType.class,
            (type, buffer) -> buffer.writeByte((type.isSingleTarget() ? 1 : 0) | (type.isPlayersOnly() ? 2 : 0)),
            buffer -> {
                final byte flags = buffer.readByte();
                final boolean single = (flags & 1) != 0;
                final boolean playersOnly = (flags & 2) != 0;
                if (playersOnly) {
                    return single ? EntityArgumentType.player() : EntityArgumentType.players();
                }
                return single ? EntityArgumentType.entity() : EntityArgumentType.entities();
            });
        register("retrocommands:item", ItemArgumentType.class, buffer -> ItemArgumentType.item());
        register("retrocommands:block_pos", BlockPosArgumentType.class, buffer -> BlockPosArgumentType.blockPos());
        register("retrocommands:vec3", Vec3ArgumentType.class,
            (type, buffer) -> buffer.writeBoolean(type.centersIntegers()),
            buffer -> Vec3ArgumentType.vec3(buffer.readBoolean()));
        register("retrocommands:time", TimeArgumentType.class, buffer -> TimeArgumentType.time());
        register("retrocommands:gamemode", GameModeArgumentType.class, buffer -> GameModeArgumentType.gameMode());
        register("retrocommands:message", MessageArgumentType.class, buffer -> MessageArgumentType.message());
        register("retrocommands:dimension", DimensionArgumentType.class, buffer -> DimensionArgumentType.dimension());
        register("retrocommands:entity_summon", EntitySummonArgumentType.class, buffer -> EntitySummonArgumentType.entitySummon());
    }

    private static <T extends ArgumentType<?>> void register(final String id, final Class<T> type, final Serializer<T> serializer) {
        final Entry<T> entry = new Entry<>(id, type, serializer);
        BY_ID.put(id, entry);
        BY_CLASS.put(type, entry);
    }

    private static <T extends ArgumentType<?>> void register(final String id, final Class<T> type,
                                                             final java.util.function.BiConsumer<T, PacketBuffer> writer,
                                                             final java.util.function.Function<PacketBuffer, T> reader) {
        register(id, type, new Serializer<T>() {
            @Override
            public void write(final T value, final PacketBuffer buffer) {
                writer.accept(value, buffer);
            }

            @Override
            public T read(final PacketBuffer buffer) {
                return reader.apply(buffer);
            }
        });
    }

    /** Lets another mod make its own argument type survive the trip to a client. */
    public static <T extends ArgumentType<?>> void registerCustom(final String id, final Class<T> type, final Serializer<T> serializer) {
        register(id, type, serializer);
    }

    @SuppressWarnings("unchecked")
    public static void write(final ArgumentType<?> type, final PacketBuffer buffer) {
        final Entry<?> entry = BY_CLASS.get(type.getClass());
        if (entry == null) {
            buffer.writeString("brigadier:string");
            buffer.writeByte(0);
            return;
        }

        buffer.writeString(entry.id());
        ((Serializer<ArgumentType<?>>) entry.serializer()).write(type, buffer);
    }

    public static ArgumentType<?> read(final PacketBuffer buffer) {
        final String id = buffer.readString();
        final Entry<?> entry = BY_ID.get(id);
        if (entry == null) {
            return StringArgumentType.word();
        }
        return entry.serializer().read(buffer);
    }
}
