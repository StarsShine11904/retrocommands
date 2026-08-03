package com.periut.retrocommands;

import com.periut.retrocommands.optionaldep.cryonicconfig.CryonicConfigCompat;
import com.periut.retrocommands.util.RetroChatUtil;
import com.periut.retrocommands.util.VanillaMobs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class RetroCommands implements ModInitializer {
    public static final String MOD_ID = "retrocommands";

    // other mods located
    public static boolean mojangFix = false;
    public static boolean cryConfig = false;
    public static boolean bhCreative = false;

    // Multiplayer
    public static boolean mp_rc = false;
    public static boolean mp_op = false;

    // String arrays for completions help
    public static String[] player_names = null;
    public static List<String> disabled_commands = List.of(new String[]{});

    @Override
    public void onInitialize() {
        mojangFix = FabricLoader.getInstance().isModLoaded("mojangfixstationapi");
        // Cryonic Config is reached by reflection, so require the API to have resolved too -
        // an incompatible version must not register /reloadcryonicconfig.
        cryConfig = FabricLoader.getInstance().isModLoaded("cryonicconfig")
                && CryonicConfigCompat.isAvailable();
        bhCreative = FabricLoader.getInstance().isModLoaded("bhcreative");

        RetroChatUtil.addDefaultCommands();
        VanillaMobs.setupSummons();
    }
}
