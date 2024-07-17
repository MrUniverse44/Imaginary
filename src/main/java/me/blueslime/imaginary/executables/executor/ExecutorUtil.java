package me.blueslime.imaginary.executables.executor;

import me.blueslime.imaginary.executables.Executable;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/*
 * This class contains findClass from PlaceholderAPI
 *
 * PlaceholderAPI free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlaceholderAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * This part is for credits to the author
 * If you want to know more about PlaceholdersAPI, see:
 * https://github.com/PlaceholderAPI/PlaceholderAPI/
 * for more information.
 */
public class ExecutorUtil {

    // Code from PlaceholdersAPI, credits above
    public static Class<? extends Listener> findClass(File file) throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            return null;
        }

        final URL jarUrl = file.toURI().toURL();
        final URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Executable.class.getClassLoader());
        final List<String> matches = new ArrayList<>();
        final List<Class<? extends Listener>> classes = new ArrayList<>();

        try (JarInputStream stream = new JarInputStream(Files.newInputStream(file.toPath()))) {
            JarEntry entry;
            while ((entry = stream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                matches.add(name.substring(0, name.lastIndexOf('.')).replace('/', '.'));
            }

            for (String match : matches) {
                try {
                    Class<?> loaded = loader.loadClass(match);
                    if (Listener.class.isAssignableFrom(loaded)) {
                        classes.add(loaded.asSubclass(Listener.class));
                    }
                } catch (NoClassDefFoundError ignored) {
                }
            }
        }

        if (classes.isEmpty()) {
            loader.close();
            return null;
        }
        return classes.get(0);
    }
}
