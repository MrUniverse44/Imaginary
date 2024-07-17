package me.blueslime.imaginary.executables;

import me.blueslime.bukkitmeteor.implementation.Implements;
import me.blueslime.bukkitmeteor.inventory.Inventories;
import me.blueslime.bukkitmeteor.inventory.handlers.DefaultInventory;
import me.blueslime.bukkitmeteor.libs.utilitiesapi.reflection.utils.storage.PluginStorage;
import me.blueslime.bukkitmeteor.libs.utilitiesapi.utils.consumer.PluginConsumer;
import me.blueslime.bukkitmeteor.logs.MeteorLogger;
import me.blueslime.bukkitmeteor.menus.Menus;
import me.blueslime.imaginary.Imaginary;
import me.blueslime.imaginary.api.events.ExecutableInitializeEvent;
import me.blueslime.imaginary.api.events.ExecutableShutdownEvent;
import me.blueslime.imaginary.executables.executor.ExecutorService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Executable {
    private final PluginStorage<String, File> runningFileStorage = PluginStorage.initAsConcurrentHash();
    private final PluginStorage<String, Object> instanceStorage = PluginStorage.initAsConcurrentHash();
    private static final String PACKAGE_NAME = "me.blueslime.imaginary.generated";

    private ExecutorService loader;

    private final String identifier;
    private final File file;

    public Executable(final File file) {
        this.file = file;

        this.identifier = file.getName()
            .replace(".yml", "")
            .toLowerCase(Locale.ENGLISH);

        this.loader = new ExecutorService(
            Implements.fetch(Imaginary.class).getClass().getClassLoader()
        );
    }

    public void initialize() {
        MeteorLogger logs = Implements.fetch(MeteorLogger.class);
        long time = System.currentTimeMillis();
        // Initialize FileConfiguration for this file.
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        // Imaginary plugin instance
        Imaginary plugin = Implements.fetch(Imaginary.class);

        // Initialize menus from this file.
        ConfigurationSection menuSection = configuration.getConfigurationSection("menus");
        // Load menus
        if (menuSection != null) {
            logs.info("Loading events from " + file.getName());
            // Instance
            Menus menus = Implements.fetch(Menus.class);
            // Menus section exists so we need to load all instances
            for (String menuIdentifier : menuSection.getKeys(false)) {
                ConfigurationSection menuData = menuSection.getConfigurationSection(menuIdentifier);

                if (menuData != null) {
                    // We save the menu to the storage settings, and with this the menu was already finished.
                    menus.getMenuStorageSettings().set(
                        identifier + ":" + menuIdentifier, menuData
                    );
                }
            }
        }


        // Initialize pre-made inventories.
        ConfigurationSection inventorySection = configuration.getConfigurationSection("inventories");
        // Load Inventories
        if (inventorySection != null) {
            logs.info("Loading inventories from " + file.getName());
            // Instance
            Inventories inventories = Implements.fetch(Inventories.class);
            // Inventory section exists, so we need to load all instances
            for (String inventoryIdentifier : inventorySection.getKeys(false)) {
                ConfigurationSection inventoryData = inventorySection.getConfigurationSection(inventoryIdentifier);


                if (inventoryData != null) {
                    // An inventory with a new identifier exists, so we need to save it in the storage.
                    inventories.getInventoryStorage().set(
                        identifier + ":" + inventoryIdentifier,
                        new DefaultInventory(plugin, inventoryData, file)
                    );
                }
            }
        }

        // Initialize event system.
        ConfigurationSection eventSection = configuration.getConfigurationSection("events");

        // Check to prevent null exceptions.
        if (eventSection == null) {
            logs.info("No events found for " + file.getName());
            return;
        }

        // We need to get values from events
        Map<String, Object> events = eventSection.getValues(false);

        // We save here the generated classes
        File outputMainDir = new File(plugin.getDataFolder(), "generated");
        File outputDir = new File(outputMainDir, identifier);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String key : eventSection.getKeys(false)) {
            String path = "events." + key + ".";
            List<String> imports = configuration.getStringList(path + "imports");
            List<String> codeLines = configuration.getStringList(path + "class-modifiers.code");

            // Class name will be the same of this fileName
            String className = this.identifier + "_" + key;
            StringBuilder codeBuilder = new StringBuilder();

            // We should add the imports to this current class
            for (String importStatement : imports) {
                codeBuilder.append("import ").append(importStatement).append(";\n");
            }
            codeBuilder.append("\n");

            // This is an implementation of listener, so we register it as a listener.
            codeBuilder.append("public class ").append(className).append(" implements Listener {\n");

            // Now we add the code lines here.
            for (String codeLine : codeLines) {
                codeBuilder.append(codeLine).append("\n");
            }

            // We close the class here.
            codeBuilder.append("}\n");

            // We get the final code here.
            String classCode = codeBuilder.toString();

            // We show the result in console
            logs.info("Generated class result from event of executable: " + identifier, "class name: " + className + "result:\n" + classCode);

            PluginConsumer.process(
                () -> {
                    File javaFile = saveJavaFile(outputDir, className, classCode);
                    compileFile(javaFile);
                    File jarFile = packageToJar(outputDir, className);

                    Class<?> loadedClass = loadClassFromJar(jarFile, PACKAGE_NAME + "." + className);
                    registerEvent(loadedClass, className);
                },
                e -> logs.error(e, "Failed to load executable: " + className)
            );
        }
        logs.info("Executable with identifier: " + identifier + ", was loaded in " + (System.currentTimeMillis() - time) + "ms");
    }

    private void compileFile(File javaFile) {
        PluginConsumer.process(
            () -> {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                int result = compiler.run(null, null, null, javaFile.getPath());

                if (result != 0) {
                    throw new IOException("Compilation failed for " + javaFile.getName());
                }
            },
            e -> {}
        );
    }

    private Class<?> loadClassFromJar(File jarFile, String className) {
        return PluginConsumer.ofUnchecked(
            () -> {
                ExecutorService service = Implements.fetch(ExecutorService.class);
                service.addJar(jarFile);
                runningFileStorage.set(className, jarFile);
                return service.loadClass(className);
            },
            e -> Implements.fetch(MeteorLogger.class).error(e, "Failed to load class " + className),
            () -> null
        );
    }

    private void registerEvent(Class<?> clazz, String identifier) {
        Imaginary plugin = Implements.fetch(Imaginary.class);

        if (clazz == null) {
            // We can't create an instance of a null class XD
            return;
        }
        try {
            Object instance = clazz.newInstance();

            if (instance instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) instance, plugin);
            }

            plugin.getServer().getPluginManager().callEvent(
                new ExecutableInitializeEvent(instance)
            );

            instanceStorage.set(identifier, instance);
        } catch (InstantiationException | IllegalAccessException e) {
            plugin.getLogs().error(e, "Failed to register event for class " + clazz.getName());
        }
    }

    private File saveJavaFile(File outputDir, String className, String classCode) {
        return PluginConsumer.ofUnchecked(
            () -> {
                File packageDir = new File(outputDir, PACKAGE_NAME.replace('.', '/'));
                if (!packageDir.exists()) {
                    packageDir.mkdirs();
                }
                File javaFile = new File(packageDir, className + ".java");
                try (FileWriter fileWriter = new FileWriter(javaFile)) {
                    fileWriter.write(classCode);
                }
                return javaFile;
            },
            e -> {},
            () -> null
        );
    }

    private File packageToJar(File outputDir, String className) {
        return PluginConsumer.ofUnchecked(
            () -> {
                File jarFile = new File(outputDir, className + ".jar");
                try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
                    File classFile = new File(outputDir, PACKAGE_NAME.replace('.', '/') + "/" + className + ".class");
                    JarEntry entry = new JarEntry(PACKAGE_NAME.replace('.', '/') + "/" + classFile.getName());
                    jos.putNextEntry(entry);
                    Files.copy(classFile.toPath(), jos);
                    jos.closeEntry();
                }
                return jarFile;
            },
            e -> Implements.fetch(MeteorLogger.class).error(e, "Cannot package to a jar " + className + " due to an issue."),
            () -> null
        );
    }

    public void shutdown() {
        Implements.fetch(MeteorLogger.class).info("Unloading executable: " + identifier);
        // Remove menus from this executable file.
        Menus menus = Implements.fetch(Menus.class);
        if (menus != null) {
            menus.getMenuStorageSettings().entrySet().removeIf(entry -> entry.getKey().startsWith(identifier + ":"));
        }
        // Remove inventories from this executable file
        Inventories inventories = Implements.fetch(Inventories.class);
        if (inventories != null) {
            inventories.getInventoryStorage().entrySet().removeIf(entry -> entry.getKey().startsWith(identifier + ":"));
        }
        // Remove all files from this project
        runningFileStorage.entrySet().forEach(
            entry -> entry.getValue().delete()
        );

        Imaginary plugin = Implements.fetch(Imaginary.class);

        // Unload event(s)
        instanceStorage.entrySet().forEach(
            entry -> {
                Object value = entry.getValue();
                // First we need to call the disable event
                // Because the executor author should know that this resource will be unloaded.
                plugin.getServer().getPluginManager().callEvent(new ExecutableShutdownEvent(value));
                // Now we can unload the listener, cause was already called.
                if (value instanceof Listener) {
                    HandlerList.unregisterAll((Listener)value);
                }
            }
        );

        instanceStorage.clear();
        runningFileStorage.clear();

        // Now we don't need to remove instances from the class loader because is dynamic, but we should remove this class loader from plugin's class loader
        loader.clear();
        loader = null;
        Implements.fetch(MeteorLogger.class).info("Executable unloaded: " + identifier);
    }
}
