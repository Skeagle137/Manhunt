package net.skeagle.manhunt.model;

import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;

import java.util.ArrayList;
import java.util.List;

public abstract class MHBasePhase {

    private final List<EventListener<?>> events;
    private final List<Task> tasks;
    protected final MHManager manager;
    private final MHState state;

    public MHBasePhase(MHManager manager, MHState state) {
        this.events = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.manager = manager;
        this.state = state;
    }

    protected final void nextPhase() {
        this.cleanup();
        manager.nextPhase();
        this.onEnd(manager.getCurrentPhase().state);
    }

    protected final void previousPhase() {
        this.cleanup();
        manager.previousPhase();
        this.onEnd(manager.getCurrentPhase().state);
    }

    private void cleanup() {
        manager.getUpdateTask().cancel();
        this.events.forEach(EventListener::unregister);
        this.events.clear();
        this.tasks.forEach(Task::cancel);
        this.tasks.clear();
    }

    public void startPhase() {
        this.onInit();
        manager.setState(state);
        manager.startUpdateTask();
    }

    protected final void addListener(EventListener<?> event) {
        events.add(event);
    }

    protected void onInit() {

    }

    protected void onUpdate() {
        //cancel if not overridden
        manager.getUpdateTask().cancel();
    }

    protected void onEnd(MHState state) {

    }

    protected void addTask(Task task) {
        tasks.add(task);
    }
}
