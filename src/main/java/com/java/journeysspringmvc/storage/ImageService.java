/*
 * Turns uploaded images into the files the app serves, like Natours' multer + sharp setup.
 *
 * Every upload is checked to really be an image, cropped to fill the target size from the
 * centre (sharp's "cover" fit), converted to JPEG at 90% quality and saved through
 * ImageStorage under a unique name:
 *   user photo   500 x 500     users/user-<userId>-<timestamp>.jpeg
 *   tour cover   2000 x 1333   tours/tour-<tourId>-<timestamp>-cover.jpeg
 *   tour images  2000 x 1333   tours/tour-<tourId>-<timestamp>-<n>.jpeg
 * The returned file names are what the photo / imageCover / images fields store; the files
 * are served at /img/users/<name> and /img/tours/<name>. Non-images get a 400.
 */
package com.java.journeysspringmvc.storage;

import com.java.journeysspringmvc.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final float JPEG_QUALITY = 0.9f;

    private final ImageStorage imageStorage;

    public String saveUserPhoto(UUID userId, MultipartFile photo) {
        String name = "user-" + userId + "-" + System.currentTimeMillis() + ".jpeg";
        imageStorage.save("users/" + name, toJpeg(photo, 500, 500));
        return name;
    }

    public String saveTourCover(UUID tourId, MultipartFile cover) {
        String name = "tour-" + tourId + "-" + System.currentTimeMillis() + "-cover.jpeg";
        imageStorage.save("tours/" + name, toJpeg(cover, 2000, 1333));
        return name;
    }

    public List<String> saveTourImages(UUID tourId, List<MultipartFile> images) {
        long timestamp = System.currentTimeMillis();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String name = "tour-" + tourId + "-" + timestamp + "-" + (i + 1) + ".jpeg";
            imageStorage.save("tours/" + name, toJpeg(images.get(i), 2000, 1333));
            names.add(name);
        }
        return names;
    }

    private static byte[] toJpeg(MultipartFile file, int width, int height) {
        BufferedImage source = read(file);
        // Scale so the image covers the whole target, then crop the overflow evenly from both sides
        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
        int scaledWidth = (int) Math.ceil(source.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(source.getHeight() * scale);

        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // JPEG has no transparency, so transparent pixels become white rather than black
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, (width - scaledWidth) / 2, (height - scaledHeight) / 2, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        return encodeJpeg(target);
    }

    private static BufferedImage read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BadRequestException("Not an image! Please upload only images.");
        }
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BadRequestException("Not an image! Please upload only images.");
            }
            return image;
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded image");
        }
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not encode JPEG", ex);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
