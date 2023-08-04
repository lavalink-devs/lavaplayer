package com.sedmelluq.lava.common.natives.architecture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public enum DefaultOperatingSystemTypes implements OperatingSystemType {
    LINUX("linux", "lib", ".so"),
    LINUX_MUSL("linux-musl", "lib", ".so"),
    WINDOWS("win", "", ".dll"),
    DARWIN("darwin", "lib", ".dylib"),
    SOLARIS("solaris", "lib", ".so");

    private static final Logger log = LoggerFactory.getLogger(DefaultOperatingSystemTypes.class);
    private static volatile Boolean cachedMusl;

    private final String identifier;
    private final String libraryFilePrefix;
    private final String libraryFileSuffix;

    DefaultOperatingSystemTypes(String identifier, String libraryFilePrefix, String libraryFileSuffix) {
        this.identifier = identifier;
        this.libraryFilePrefix = libraryFilePrefix;
        this.libraryFileSuffix = libraryFileSuffix;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public String libraryFilePrefix() {
        return libraryFilePrefix;
    }

    @Override
    public String libraryFileSuffix() {
        return libraryFileSuffix;
    }

    public static OperatingSystemType detect() {
        String osFullName = System.getProperty("os.name");

        if (osFullName.startsWith("Windows")) {
            return WINDOWS;
        } else if (osFullName.startsWith("Mac OS X")) {
            return DARWIN;
        } else if (osFullName.startsWith("Solaris")) {
            return SOLARIS;
        } else if (osFullName.toLowerCase().startsWith("linux")) {
            return checkMusl() ? LINUX_MUSL : LINUX;
        } else {
            throw new IllegalArgumentException("Unknown operating system: " + osFullName);
        }
    }

    private static boolean checkMusl() {
        Boolean b = cachedMusl;
        if (b == null) {
            synchronized (DefaultOperatingSystemTypes.class) {
                boolean check = false;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("/proc/self/maps"))))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("-musl-")) {
                            check = true;
                            break;
                        }
                    }
                } catch (IOException fail) {
                    log.error("Failed to detect libc type, assuming glibc", fail);
                    check = false;
                }
                log.debug("is musl: {}", check);
                b = cachedMusl = check;
            }
        }
        return b;
    }
}
