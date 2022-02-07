package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.Settings;
import net.skeagle.manhunt.model.MHBasePhase;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.MHState;
import net.skeagle.vrnlib.misc.Task;

public class EndGamePhase extends MHBasePhase {

    public EndGamePhase(MHManager manager) {
        super(manager, MHState.ENDED);
    }

    @Override
    protected void onInit() {
        Task.syncDelayed(this::nextPhase, Settings.restartTime * 20L);
    }
}
