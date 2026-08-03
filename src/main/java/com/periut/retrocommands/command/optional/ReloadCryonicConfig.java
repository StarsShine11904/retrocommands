package com.periut.retrocommands.command.optional;

import com.periut.retrocommands.api.Command;
import com.periut.retrocommands.util.SharedCommandSource;
import com.periut.retrocommands.optionaldep.cryonicconfig.CryonicConfigCompat;

public class ReloadCryonicConfig implements Command {

    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        if (CryonicConfigCompat.reload(System.getProperty("user.dir"))) {
            commandSource.sendFeedback("Cryonic Config has been refreshed!");
        } else {
            commandSource.sendFeedback("Cryonic Config is not available!");
        }
    }

    @Override
    public String name() {
        return "reloadcryonicconfig";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /reloadcryonicconfig");
        commandSource.sendFeedback("Info: Refreshes the config cache for Cryonic Config");
    }
}
