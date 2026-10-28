/*
 * Where uploaded images are stored, bound from journeys.storage.* (see application.properties).
 *
 *   image-dir  directory for user photos (users/) and tour images (tours/); relative paths
 *              are resolved against the working directory. Served at /img/** by WebConfig.
 */
package com.java.journeysspringmvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

@ConfigurationProperties("journeys.storage")
public record StorageProperties(@DefaultValue("uploads/img") Path imageDir) {
}
