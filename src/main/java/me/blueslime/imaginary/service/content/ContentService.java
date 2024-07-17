package me.blueslime.imaginary.service.content;

import me.blueslime.bukkitmeteor.implementation.Implements;
import me.blueslime.bukkitmeteor.implementation.module.RegisteredModule;
import me.blueslime.bukkitmeteor.logs.MeteorLogger;
import me.blueslime.bukkitmeteor.utils.FileUtil;
import me.blueslime.imaginary.Imaginary;
import me.blueslime.imaginary.executables.Executable;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ContentService implements RegisteredModule {

    private final Map<String, Executable> executables = new HashMap<>();

    @Override
    public void initialize() {
        File file = Implements.fetch(File.class, "executables");

        if (!file.exists()) {
            if (file.mkdirs()) {
                File target = new File(
                    file, "TestYaml.yml"
                );
                InputStream src = FileUtil.build("TestYaml.yml");
                src = src == null ? Implements.fetch(Imaginary.class).getResource("TestYaml.yml") : src;
                FileUtil.saveResource(target, src);
            }
            return;
        }

        File[] files = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (files == null) {
            Implements.fetch(MeteorLogger.class).info("This plugin don't have executable files.");
            return;
        }

        for (File executable : files) {
            String name = executable.getName()
                    .replace(".yml", "")
                    .toLowerCase(Locale.ENGLISH);

            Executable instance = executables.put(
                name,
                new Executable(executable)
            );

            if (instance != null) {
                instance.initialize();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void shutdown() {
        File outputMainDir = new File(Implements.fetch(File.class, "folder"), "generated");
        if (outputMainDir.exists()) {
            outputMainDir.delete();
        }
        executables.forEach((key, value) -> value.shutdown());
        executables.clear();
    }

    @Override
    public void reload() {
        shutdown();
        initialize();
    }

}
