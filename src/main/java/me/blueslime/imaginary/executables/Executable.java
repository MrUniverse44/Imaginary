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
import me.blueslime.imaginary.executables.executor.ExecutorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import javax.tools.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Executable {
    private final PluginStorage<String, File> runningFileStorage = PluginStorage.initAsConcurrentHash();
    private final PluginStorage<String, Object> instanceStorage = PluginStorage.initAsConcurrentHash();
    private static final String PACKAGE_NAME = "me.blueslime.imaginary.generated";

    private final String identifier;
    private final File file;

    public Executable(final File file) {
        this.file = file;

        this.identifier = file.getName()
            .replace(".yml", "")
            .toLowerCase(Locale.ENGLISH);
    }

    public void initialize() {
        MeteorLogger logs = Implements.fetch(MeteorLogger.class);
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

        // We save here the generated classes
        File outputMainDir = new File(plugin.getDataFolder(), "generated");
        File outputDir = new File(outputMainDir, identifier);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        long time = System.currentTimeMillis();
        for (String key : eventSection.getKeys(false)) {
            String path = "events." + key + ".";
            List<String> imports = configuration.getStringList(path + "imports");
            List<String> codeLines = configuration.getStringList(path + "class-modifiers.code");

            // Class name will be the same of this fileName
            String className = this.identifier + "_" + key;
            StringBuilder codeBuilder = new StringBuilder();

            //PACKAGE_NAME + "." + className
            codeBuilder.append("package " + PACKAGE_NAME + ";\n\n");

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

            FileConfiguration settings = Implements.fetch(FileConfiguration.class, "settings.yml");

            boolean result = settings == null || settings.getBoolean("debug-mode", false);

            if (result) {
                // We show the result in console
                logs.info(
                        "Generated class result from event of executable: " + identifier,
                        "class name: " + className + ", result:\n\n" + classCode
                );
            }

            PluginConsumer.process(
                () -> {
                    File javaFile = saveJavaFile(outputDir, className, classCode);
                    compileFile(javaFile);
                    File jarFile = packageToJar(outputDir, className);

                    Class<?> loadedClass = loadClassFromJar(jarFile, className);
                    registerEvent(loadedClass, className);
                },
                e -> logs.error(e, "Failed to load executable: " + className)
            );
        }
        logs.info("Executable with identifier: " + identifier + ", was loaded in " + (System.currentTimeMillis() - time) + "ms");
    }

    private List<File> findAllLoadedJars() {
        List<File> loadedJars = new ArrayList<>();
        Imaginary plugin = Implements.fetch(Imaginary.class);

        ClassLoader cl = plugin.getClass().getClassLoader();

        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) cl).getURLs();
                for (URL url : urls) {
                    try {
                        File file = new File(url.toURI());
                        if (file.exists() && file.getName().toLowerCase().endsWith(".jar")) {
                            loadedJars.add(file);
                        }
                    } catch (URISyntaxException ignored) {}
                }
            }
            cl = cl.getParent();
        }

        // Adds the server jar to the classpath
        // This is the important because the compiler should compile the class
        // Using the jar to prevent issues.

        String serverJarPath = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File serverJar = new File(serverJarPath);

        if (serverJar.exists() && !loadedJars.contains(serverJar)) {
            loadedJars.add(serverJar);
        }

        File serverJarURI = PluginConsumer.ofUnchecked(
            () -> new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()),
            e -> {
                plugin.getLogs().error(e, "Can't add the server jar URI");
            },
            () -> null
        );

        if (serverJarURI != null) {
            if (!loadedJars.contains(serverJarURI)) {
                loadedJars.add(serverJarURI);
            }
        }

        return loadedJars;
    }

    private void compileFile(File javaFile) {
        PluginConsumer.process(
            () -> {
                List<File> loadedJars = findAllLoadedJars();

                // Set up the Java compiler with the entire classpath with all loaded jars

                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFile);

                List<String> optionList = new ArrayList<>();
                StringBuilder classpathBuilder = new StringBuilder();

                for (File jar : loadedJars) {
                    if (jar.exists() && jar.isFile()) {
                        classpathBuilder.append(jar.getAbsolutePath()).append(File.pathSeparator);
                    }
                }
                classpathBuilder.append(System.getProperty("java.class.path"));

                optionList.add("-classpath");
                optionList.add(classpathBuilder.toString());

                // With this if a developer add an optional plugin support, it will not pause the compiler
                // If that plugin is not installed.
                optionList.add("-Xdiags:verbose");
                optionList.add("-Xlint:none");

                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                StringWriter writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                JavaCompiler.CompilationTask task = compiler.getTask(printWriter, fileManager, diagnostics, optionList, null, compilationUnits);

                if (task.call()) {
                    Implements.fetch(MeteorLogger.class).info("Loaded an event class for executor: " + identifier);
                } else {
                    MeteorLogger logs = Implements.fetch(MeteorLogger.class);
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        logs.debug("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic.getSource().toUri(), diagnostic.getMessage(Locale.ENGLISH));
                    }
                }

                printWriter.flush();
                fileManager.close();
            },
            e -> {}
        );
    }

    private Class<?> loadClassFromJar(File jarFile, String className) {
        return PluginConsumer.ofUnchecked(
            () -> {
                if (jarFile != null && jarFile.exists()) {
                    runningFileStorage.set(className, jarFile);
                    return ExecutorUtil.findClass(jarFile);
                }
                Implements.fetch(MeteorLogger.class).info("Can't load: " + className , "because file was not found in path: " + (jarFile != null ? jarFile.getAbsolutePath() : ""));
                return null;
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
                    if (!classFile.exists()) {
                        Implements.fetch(MeteorLogger.class).error("Can't find class file at: " + classFile.getAbsolutePath());
                        return null;
                    }
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

        Implements.fetch(MeteorLogger.class).info("Executable unloaded: " + identifier);
    }
}
