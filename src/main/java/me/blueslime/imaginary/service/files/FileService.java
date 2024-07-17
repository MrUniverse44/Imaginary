package me.blueslime.imaginary.service.files;

import me.blueslime.bukkitmeteor.implementation.Implements;
import me.blueslime.bukkitmeteor.implementation.module.Module;
import me.blueslime.bukkitmeteor.inventory.event.InventoriesFolderGenerationEvent;
import me.blueslime.bukkitmeteor.menus.event.MenusFolderGenerationEvent;
import me.blueslime.imaginary.Imaginary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FileService implements Module, Listener {
    @Override
    public void initialize() {
        Imaginary imaginary = Implements.fetch(Imaginary.class);

        // We don't want menus and inventories folders in this project.
        imaginary.getServer().getPluginManager().registerEvents(
            this, imaginary
        );
    }

    @EventHandler
    public void on(MenusFolderGenerationEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(InventoriesFolderGenerationEvent event) {
        event.setCancelled(true);
    }
}
