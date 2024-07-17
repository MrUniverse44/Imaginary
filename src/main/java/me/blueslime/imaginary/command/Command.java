package me.blueslime.imaginary.command;

import me.blueslime.bukkitmeteor.libs.utilitiesapi.commands.AdvancedCommand;
import me.blueslime.bukkitmeteor.libs.utilitiesapi.commands.sender.Sender;
import me.blueslime.imaginary.Imaginary;

import java.util.Locale;

public class Command extends AdvancedCommand<Imaginary> {
    public Command(Imaginary plugin) {
        super(plugin, "imaginary");
    }

    @Override
    public void executeCommand(Sender sender, String label, String[] arguments) {
        if (!sender.hasPermission("imaginary.command")) {
            sender.send("&cYou don't have permission to use this command!");
            return;
        }
        if (arguments.length == 0 || arguments[0].equalsIgnoreCase("help")) {
            sender.send("&7/imaginary help");
            sender.send("&7/imaginary reload");
            sender.send("&7/imaginary version");
            return;
        }
        String argument = arguments[0].toLowerCase(Locale.ENGLISH);
        switch (argument) {
            case "reload":
                long time = System.currentTimeMillis();
                plugin.reload();
                sender.send("&aReloaded in " + (System.currentTimeMillis() - time) + "ms!");
                return;
            case "version":
                sender.send("&bBeta v" + plugin.getDescription().getVersion());
                return;
            default:
                sender.send("&cUnknown command!");
                break;
        }
    }
}
