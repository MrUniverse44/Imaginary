package me.blueslime.imaginary;

import me.blueslime.bukkitmeteor.BukkitMeteorPlugin;
import me.blueslime.imaginary.command.Command;
import me.blueslime.imaginary.service.MeteorGetter;
import me.blueslime.imaginary.service.content.ContentService;
import me.blueslime.imaginary.service.files.FileService;
import me.blueslime.imaginary.service.metrics.BukkitMetricsService;

public final class Imaginary extends BukkitMeteorPlugin {

    @Override
    public void onEnable() {
        initialize(this);
    }

    @Override
    public void registerModules() {
        // We register first the most important things.
        new MeteorGetter(this);
        // Register of modules
        registerModule(
            new ContentService(),
            new FileService(),
            new BukkitMetricsService(this, 22681)
        ).finish();
        // Now we register Statistics.
        // Now we register the command
        new Command(this).register();
        // Now an extra feature.
        info(
            "\n\n" +
            "&a██╗███╗░░░███╗░█████╗░░██████╗░██╗███╗░░██╗░█████╗░██████╗░██╗░░░██╗\n" +
            "&a██║████╗░████║██╔══██╗██╔════╝░██║████╗░██║██╔══██╗██╔══██╗╚██╗░██╔╝\n" +
            "&a██║██╔████╔██║███████║██║░░██╗░██║██╔██╗██║███████║██████╔╝░╚████╔╝░\n" +
            "&a██║██║╚██╔╝██║██╔══██║██║░░╚██╗██║██║╚████║██╔══██║██╔══██╗░░╚██╔╝░░\n" +
            "&a██║██║░╚═╝░██║██║░░██║╚██████╔╝██║██║░╚███║██║░░██║██║░░██║░░░██║░░░\n" +
            "&a╚═╝╚═╝░░░░░╚═╝╚═╝░░╚═╝░╚═════╝░╚═╝╚═╝░░╚══╝╚═╝░░╚═╝╚═╝░░╚═╝░░░╚═╝░░░\n" +
            "\n" +
            "    &9&lWELCOME TO IMAGINARY BETA\n" +
            "    &eCreated by JustJustin",
            "    &bSpigotMC: https://www.spigotmc.org/members/imuniverse.163416/"
        );
    }
}
