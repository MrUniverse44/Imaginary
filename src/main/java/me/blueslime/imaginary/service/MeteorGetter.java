package me.blueslime.imaginary.service;

import me.blueslime.bukkitmeteor.implementation.module.Module;
import me.blueslime.bukkitmeteor.implementation.registered.Register;
import me.blueslime.bukkitmeteor.logs.MeteorLogger;
import me.blueslime.imaginary.Imaginary;

import java.io.File;

public class MeteorGetter implements Module {
    private final Imaginary plugin;

    public MeteorGetter(Imaginary plugin) {
        this.plugin = plugin;
        register(this);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Register
    public MeteorLogger provideLogger() {
        return plugin.getLogs();
    }

    @Register
    public Imaginary providePlugin() {
        return plugin;
    }

    @Register(identifier = "folder")
    public File provideDataFolder() {
        return plugin.getDataFolder();
    }

    @Register(identifier = "executables")
    public File provideExecutables() {
        File file = new File(plugin.getDataFolder(), "executables");
        if (!file.exists() && !file.mkdirs()) {
            plugin.getLogs().error("Can't create executables folder");
        }
        return file;
    }
}
