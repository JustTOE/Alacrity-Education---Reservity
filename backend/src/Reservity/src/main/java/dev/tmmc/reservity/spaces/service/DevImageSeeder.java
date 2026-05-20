package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.storage.StorageService;
import dev.tmmc.reservity.common.storage.StoredObject;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only seeder. Runs once after Spring is fully started; for any space that
 * has no images, generates a deterministic gradient JPEG with the space slug
 * stamped on it, uploads it via {@link StorageService}, and inserts the
 * {@link SpaceImage} row. Skips spaces that already have images so reruns
 * are no-ops.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevImageSeeder {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 540;

    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository imageRepository;
    private final StorageService storage;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedPlaceholderImages() {
        if (GraphicsEnvironment.isHeadless()) {
            // ImageIO works headless on the JDK, but Color/Graphics depends on a font config.
            // Most servers have it; log and proceed.
            log.debug("Headless graphics — DevImageSeeder still proceeding (Java 2D works headlessly).");
        }

        List<Space> spaces = spaceRepository.findAll();
        if (spaces.isEmpty()) {
            log.debug("DevImageSeeder: no spaces to seed.");
            return;
        }

        User uploader = userRepository.findAll().stream().findFirst().orElse(null);
        int uploaded = 0;
        for (Space space : spaces) {
            if (imageRepository.countBySpaceId(space.getId()) > 0) continue;
            try {
                byte[] bytes = renderPlaceholderJpeg(space.getName(), space.getType().value());
                String key = "spaces/" + space.getId() + "/" + UUID.randomUUID() + ".jpg";
                StoredObject stored = storage.upload(
                        new ByteArrayInputStream(bytes), bytes.length, "image/jpeg", key);

                SpaceImage img = SpaceImage.builder()
                        .space(space)
                        .s3Key(stored.key())
                        .url(stored.url())
                        .altText(space.getName() + " placeholder")
                        .width(WIDTH)
                        .height(HEIGHT)
                        .byteSize((long) bytes.length)
                        .contentType("image/jpeg")
                        .displayOrder((short) 0)
                        .primary(true)
                        .uploadedBy(uploader)
                        .build();
                imageRepository.save(img);
                uploaded++;
            } catch (IOException | RuntimeException e) {
                log.warn("DevImageSeeder: failed to seed image for space {}: {}", space.getSlug(), e.getMessage());
            }
        }
        if (uploaded > 0) {
            log.info("DevImageSeeder: uploaded placeholder images for {} space(s)", uploaded);
        }
    }

    private static byte[] renderPlaceholderJpeg(String title, String typeLabel) throws IOException {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color start = colorFromHash(title, 0.55f);
            Color end = colorFromHash(title + "-end", 0.40f);
            for (int y = 0; y < HEIGHT; y++) {
                float t = (float) y / HEIGHT;
                int r = (int) (start.getRed() + t * (end.getRed() - start.getRed()));
                int gn = (int) (start.getGreen() + t * (end.getGreen() - start.getGreen()));
                int b = (int) (start.getBlue() + t * (end.getBlue() - start.getBlue()));
                g.setColor(new Color(clamp(r), clamp(gn), clamp(b)));
                g.drawLine(0, y, WIDTH, y);
            }

            g.setColor(new Color(255, 255, 255, 200));
            g.fillRoundRect(40, HEIGHT - 140, WIDTH - 80, 100, 24, 24);

            g.setColor(new Color(20, 20, 30));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            g.drawString(title, 60, HEIGHT - 95);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.setColor(new Color(80, 80, 100));
            g.drawString(typeLabel.toUpperCase() + " · placeholder", 60, HEIGHT - 65);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private static Color colorFromHash(String s, float brightness) {
        int hash = s.hashCode();
        float hue = ((hash & 0x7fffffff) % 360) / 360f;
        return Color.getHSBColor(hue, 0.55f, brightness);
    }

    private static int clamp(int x) {
        return Math.max(0, Math.min(255, x));
    }
}
