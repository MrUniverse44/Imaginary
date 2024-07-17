package me.blueslime.imaginary.executables.executor;

import me.blueslime.bukkitmeteor.implementation.Implements;
import me.blueslime.bukkitmeteor.logs.MeteorLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ExecutorService extends ClassLoader {
    private final List<File> jars = new ArrayList<>();

    public ExecutorService(ClassLoader parent) {
        super(parent);
    }

    public void addJar(File jar) {
        jars.add(jar);
    }

    public void clear() {
        jars.clear();
    }

    public void removeJar(File jar) {
        jars.remove(jar);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (File jar : jars) {
            try (JarInputStream jis = new JarInputStream(Files.newInputStream(jar.toPath()))) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(name.replace('.', '/') + ".class")) {
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        int nextValue = 0;
                        while ((nextValue = jis.read()) != -1) {
                            byteStream.write(nextValue);
                        }
                        byte[] classBytes = byteStream.toByteArray();
                        return defineClass(name, classBytes, 0, classBytes.length);
                    }
                }
            } catch (IOException e) {
                Implements.fetch(MeteorLogger.class).error(e, "Error while Imaginary finds a Class File: " + name);
            }
        }
        throw new ClassNotFoundException(name);
    }
}
