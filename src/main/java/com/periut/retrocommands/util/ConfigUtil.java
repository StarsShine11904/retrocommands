package com.periut.retrocommands.util;

import com.periut.retrocommands.RetroCommands;
import com.periut.retrocommands.optionaldep.cryonicconfig.CryonicConfigCompat;

public class ConfigUtil {
    public static void refreshDisabledCommands() {
        String disabled = CryonicConfigCompat.getString(RetroCommands.MOD_ID, "disabledCommands", "");
        RetroCommands.disabled_commands = java.util.List.of(disabled.split(","));
    }
}
