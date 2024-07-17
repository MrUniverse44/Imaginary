package me.blueslime.imaginary.service;

import me.blueslime.bukkitmeteor.implementation.Implements;
import me.blueslime.bukkitmeteor.implementation.module.Module;
import me.blueslime.bukkitmeteor.implementation.registered.Register;
import me.blueslime.bukkitmeteor.inventory.Inventories;
import me.blueslime.bukkitmeteor.logs.MeteorLogger;
import me.blueslime.bukkitmeteor.menus.Menus;
import me.blueslime.bukkitmeteor.utils.FileUtil;
import me.blueslime.imaginary.Imaginary;

import java.io.File;
import java.io.InputStream;

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
        if (!file.exists()) {
            if (file.mkdirs()) {
                File target = new File(
                        file, "TestYaml.yml"
                );
                InputStream src = FileUtil.build("TestYaml.yml");
                src = src == null ? Implements.fetch(Imaginary.class).getResource("TestYaml.yml") : src;
                if (src == null) {
                    src = Implements.fetch(Imaginary.class).getResource("/TestYaml.yml");
                    if (src == null) {
                        src = FileUtil.build("/TestYaml.yml");
                    }
                }
                FileUtil.saveResource(target, src);
            } else {
                plugin.getLogs().error("Can't create executables folder");
            }
        }
        return file;
    }

    @Register
    public Menus provideMenus() {
        return new Menus(plugin);
    }

    @Register
    public Inventories provideInventories() {
        return new Inventories(plugin);
    }
}
