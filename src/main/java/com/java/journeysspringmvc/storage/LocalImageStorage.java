/*
 * Stores images on local disk under journeys.storage.image-dir; WebConfig serves that
 * directory at /img/**. Paths that would escape the directory (e.g. "../") are rejected.
 */
package com.java.journeysspringmvc.storage;

import com.java.journeysspringmvc.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalImageStorage implements ImageStorage {

    private final Path root;

    public LocalImageStorage(StorageProperties properties) {
        this.root = properties.imageDir().toAbsolutePath().normalize();
    }

    @Override
    public void save(String path, byte[] content) {
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Image path escapes the storage directory: " + path);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store image " + path, ex);
        }
    }
}
