package net.skeagle.manhunt.model;

import net.skeagle.vrncommands.BukkitMessages;

public enum MHEndReason {
    DRAGON_KILLED("dragonKilled"),
    RUNNER_DIED("runnerDied"),
    RUNNER_KILLED("runnerKilled"),
    RUNNER_QUIT("runnerQuit"),
    HUNTERS_QUIT("huntersQuit"),
    OUT_OF_TIME("outOfTime"),
    FORCED_END("forcedEnd");

    private final String reason;

    MHEndReason(String reason) {
        this.reason = reason;
    }

    public String get() {
        return BukkitMessages.msg(reason);
    }
}
