package net.skeagle.manhunt.model;

import net.skeagle.vrncommands.BukkitMessages;

public enum MHWinner {
    RUNNER("runnerWin"),
    HUNTERS("huntersWin"),
    DRAW("drawGame");

    private final String title;

    MHWinner(String title) {
        this.title = title;
    }

    public String getTitle() {
        return BukkitMessages.msg(title);
    }
}
