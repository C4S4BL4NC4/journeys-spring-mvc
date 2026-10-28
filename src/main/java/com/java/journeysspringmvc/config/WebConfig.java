/*
 * Spring MVC customisations.
 *
 *   - Registers Spring Boot's lenient converters, so enum query and path parameters accept
 *     Natours-style spellings: ?difficulty=easy, ?role=lead-guide, /unit/mi.
 *   - Serves uploaded images from journeys.storage.image-dir at /img/**, e.g.
 *     /img/users/user-<id>-<timestamp>.jpeg and /img/tours/tour-<id>-<timestamp>-cover.jpeg.
 */
package com.java.journeysspringmvc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class WebConfig implements WebMvcConfigurer {

    public static final String IMAGE_URL_PREFIX = "/img/";

    private final StorageProperties storageProperties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        ApplicationConversionService.addApplicationConverters(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(IMAGE_URL_PREFIX + "**")
                .addResourceLocations("file:" + storageProperties.imageDir().toAbsolutePath().normalize() + "/");
    }
}
