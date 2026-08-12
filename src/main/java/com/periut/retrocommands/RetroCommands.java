package com.periut.retrocommands;

import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandManager;
import com.periut.retrocommands.optionaldep.cryonicconfig.CryonicConfigCompat;
import com.periut.retrocommands.util.VanillaMobs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.logging.Logger;

public class RetroCommands implements ModInitializer {
    public static final String MOD_ID = "retrocommands";
    public static final Logger LOGGER = Logger.getLogger("Minecraft");

    // other mods located
    public static boolean cryConfig = false;
    public static boolean bhCreative = false;

    // Multiplayer: whether the server runs this mod, and whether we are op on it.
    public static boolean mp_rc = false;
    public static boolean mp_op = false;

    // Filled in from the server, for suggestions.
    public static String[] player_names = null;
    public static List<String> disabled_commands = List.of();

    @Override
    public void onInitialize() {
        // Cryonic Config is reached by reflection, so require the API to have resolved too -
        // an incompatible version must not register /reloadcryonicconfig.
        cryConfig = FabricLoader.getInstance().isModLoaded("cryonicconfig")
                && CryonicConfigCompat.isAvailable();
        bhCreative = FabricLoader.getInstance().isModLoaded("bhcreative");

        VanillaMobs.setupSummons();

        // A dedicated server builds its tree now; a client waits until it has a world, because
        // singleplayer commands run against that world and the tree describes it.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            RetroCommandManager.setInstance(new RetroCommandManager(RegistrationEnvironment.DEDICATED));
            com.periut.retrocommands.network.ServerCommandNetworking.register();
        }
    }
}
