/*
 * Where processed images end up. The app only depends on this interface, so the local-disk
 * implementation (LocalImageStorage) can be swapped for S3 or similar without touching the
 * code that handles uploads.
 */
package com.java.journeysspringmvc.storage;

public interface ImageStorage {

    // path is relative, e.g. "users/user-<id>-<timestamp>.jpeg"
    void save(String path, byte[] content);
}
