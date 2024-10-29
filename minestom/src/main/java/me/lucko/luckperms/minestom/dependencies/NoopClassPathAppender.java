package me.lucko.luckperms.minestom.dependencies;

import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;

import java.nio.file.Path;

public final class NoopClassPathAppender implements ClassPathAppender {

    @Override
    public void addJarToClasspath(Path file) {
        // no-op
    }

}
