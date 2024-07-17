package me.blueslime.imaginary.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExecutableShutdownEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Object executable;

    public ExecutableShutdownEvent(final Object executable) {
        this.executable = executable;
    }

    public Object getExecutable() {
        return executable;
    }

    public boolean isThis(Object clazz) {
        return executable == clazz || executable.equals(clazz);
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
