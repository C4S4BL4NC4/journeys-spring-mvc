/*
 * Unit tests for ImageService with an in-memory ImageStorage (no Spring context, no disk).
 *
 * Checks that uploads are resized and cropped to the Natours sizes as JPEG under the expected
 * names and folders (user photo 500x500, tour cover and images 2000x1333), and that files
 * that aren't images are rejected with a 400.
 */
package com.java.journeysspringmvc.storage;

import com.java.journeysspringmvc.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageServiceTest {

    private final Map<String, byte[]> stored = new HashMap<>();
    private final ImageService imageService = new ImageService(stored::put);
    private final UUID id = UUID.randomUUID();

    private static MockMultipartFile png(String part, int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        return new MockMultipartFile(part, part + ".png", "image/png", bytes.toByteArray());
    }

    private BufferedImage storedImage(String path) throws IOException {
        assertThat(stored).containsKey(path);
        return ImageIO.read(new ByteArrayInputStream(stored.get(path)));
    }

    @Test
    void cropsUserPhotoToSquareJpeg() throws IOException {
        String name = imageService.saveUserPhoto(id, png("photo", 1200, 800));

        assertThat(name).startsWith("user-" + id + "-").endsWith(".jpeg");
        BufferedImage image = storedImage("users/" + name);
        assertThat(image.getWidth()).isEqualTo(500);
        assertThat(image.getHeight()).isEqualTo(500);
    }

    @Test
    void resizesTourCoverAndImages() throws IOException {
        String cover = imageService.saveTourCover(id, png("imageCover", 640, 480));
        List<String> images = imageService.saveTourImages(id, List.of(png("images", 300, 900), png("images", 50, 50)));

        assertThat(cover).endsWith("-cover.jpeg");
        assertThat(images).hasSize(2).allSatisfy(name -> assertThat(name).startsWith("tour-" + id + "-"));
        assertThat(images.get(0)).endsWith("-1.jpeg");
        for (String name : List.of(cover, images.get(0), images.get(1))) {
            BufferedImage image = storedImage("tours/" + name);
            assertThat(image.getWidth()).isEqualTo(2000);
            assertThat(image.getHeight()).isEqualTo(1333);
        }
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        var text = new MockMultipartFile("photo", "notes.txt", "text/plain", "hello".getBytes());
        var fakeImage = new MockMultipartFile("photo", "fake.jpg", "image/jpeg", "not really".getBytes());

        assertThatThrownBy(() -> imageService.saveUserPhoto(id, text)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> imageService.saveUserPhoto(id, fakeImage)).isInstanceOf(BadRequestException.class);
        assertThat(stored).isEmpty();
    }
}
