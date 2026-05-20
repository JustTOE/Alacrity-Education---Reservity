package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.auth.dto.RegisterRequest;
import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.storage.StorageProperties;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.spaces.entity.SpaceType;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
import dev.tmmc.reservity.spaces.repository.SpaceFavoriteRepository;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.spaces.repository.SpaceVibeRepository;
import dev.tmmc.reservity.spaces.repository.SpaceWaitlistRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceImageUploadIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private SavedPassRepository savedPassRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRequestRepository requestRepository;
    @Autowired private ReservationSeriesRepository seriesRepository;
    @Autowired private SpaceFavoriteRepository favoriteRepository;
    @Autowired private SpaceWaitlistRepository waitlistRepository;
    @Autowired private SpaceVibeRepository spaceVibeRepository;
    @Autowired private SpaceImageRepository spaceImageRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private S3Client s3Client;
    @Autowired private StorageProperties storageProperties;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        savedPassRepository.deleteAll();
        reservationRepository.deleteAll();
        requestRepository.deleteAll();
        seriesRepository.deleteAll();
        favoriteRepository.deleteAll();
        waitlistRepository.deleteAll();
        spaceVibeRepository.deleteAll();
        spaceImageRepository.deleteAll();
        spaceRepository.deleteAll();
        buildingRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        ensureBucket();
    }

    @Test
    void upload_lifecycle_with_primary_flip_and_auto_promote() throws Exception {
        // Register the future owner; capture the JWT and the user's UUID.
        RegisterResult owner = registerUser("owner@m3.local", "owner_m3", "secret-password-123");

        Building hsc = buildingRepository.save(Building.builder()
                .name("Hawthorn Sciences").shortCode("HSC").campusName("Main")
                .pinX(BigDecimal.valueOf(0.5)).pinY(BigDecimal.valueOf(0.5)).build());

        Space space = createSpaceOwnedBy(owner.userId, hsc, "lab-4b", "Chemistry Lab 4B");

        // First upload — auto-becomes primary because no images exist.
        MvcResult firstUpload = mvc.perform(multipart("/api/spaces/lab-4b/images")
                        .file(pngPart("file", "first.png"))
                        .param("altText", "first photo")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith(storageProperties.getPublicBaseUrl())))
                .andExpect(jsonPath("$.altText").value("first photo"))
                .andExpect(jsonPath("$.displayOrder").value(0))
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andReturn();
        String firstId = readId(firstUpload);

        // GET /api/spaces/{slug} should now include the image and primaryImageUrl.
        mvc.perform(get("/api/spaces/lab-4b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].id").value(firstId))
                .andExpect(jsonPath("$.primaryImageUrl").exists());

        // Second upload with isPrimary=true flips primary to the new row.
        MvcResult secondUpload = mvc.perform(multipart("/api/spaces/lab-4b/images")
                        .file(pngPart("file", "second.png"))
                        .param("isPrimary", "true")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andReturn();
        String secondId = readId(secondUpload);

        mvc.perform(get("/api/spaces/lab-4b/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '" + firstId + "')].isPrimary").value(false))
                .andExpect(jsonPath("$[?(@.id == '" + secondId + "')].isPrimary").value(true));

        // PATCH altText.
        mvc.perform(patch("/api/spaces/lab-4b/images/" + firstId)
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altText\":\"updated alt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altText").value("updated alt"));

        // DELETE the current primary; the remaining image auto-promotes.
        mvc.perform(delete("/api/spaces/lab-4b/images/" + secondId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/spaces/lab-4b/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[0].isPrimary").value(true));
    }

    @Test
    void max_ten_images_returns_409() throws Exception {
        RegisterResult owner = registerUser("cap-owner@m3.local", "cap_owner", "secret-password-123");
        Building b = buildingRepository.save(Building.builder()
                .name("Linden Hall").shortCode("LHL").campusName("Main")
                .pinX(BigDecimal.valueOf(0.5)).pinY(BigDecimal.valueOf(0.5)).build());
        createSpaceOwnedBy(owner.userId, b, "pod-cap", "Pod Cap Test");

        for (int i = 0; i < 10; i++) {
            mvc.perform(multipart("/api/spaces/pod-cap/images")
                            .file(pngPart("file", "img" + i + ".png"))
                            .header("Authorization", "Bearer " + owner.token))
                    .andExpect(status().isCreated());
        }

        mvc.perform(multipart("/api/spaces/pod-cap/images")
                        .file(pngPart("file", "overflow.png"))
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isConflict());
    }

    private Space createSpaceOwnedBy(UUID ownerId, Building building, String slug, String name) {
        var emptyArr = json.createArrayNode();
        var hours = json.createObjectNode().put("mode", "24/7");
        return spaceRepository.save(Space.builder()
                .slug(slug)
                .name(name)
                .type(SpaceType.LAB)
                .ownerId(ownerId)
                .ownerType(SpaceOwnerType.USER)
                .building(building)
                .floor((short) 4)
                .room("4B")
                .seats((short) 12)
                .areaSqm(BigDecimal.valueOf(48))
                .pricePerHour(BigDecimal.valueOf(18))
                .currency("USD")
                .blurb(name + " test fixture")
                .description("test fixture")
                .amenities(emptyArr)
                .rules(emptyArr)
                .operatingHours(hours)
                .pinX(BigDecimal.valueOf(0.5))
                .pinY(BigDecimal.valueOf(0.5))
                .status(dev.tmmc.reservity.spaces.entity.SpaceStatus.PUBLISHED)
                .build());
    }

    private RegisterResult registerUser(String email, String handle, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, password, "Test " + handle, handle);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        String token = body.get("accessToken").asText();
        User u = userRepository.findAll().stream()
                .filter(usr -> usr.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow();
        return new RegisterResult(u.getId(), token);
    }

    private static MockMultipartFile pngPart(String name, String filename) throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.PINK);
            g.fillRect(0, 0, 40, 30);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return new MockMultipartFile(name, filename, "image/png", out.toByteArray());
    }

    private static String readId(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(result.getResponse().getContentAsByteArray()).get("id").asText();
    }

    private void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(storageProperties.getBucket()).build());
        } catch (NoSuchBucketException missing) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(storageProperties.getBucket()).build());
        } catch (SdkClientException notReachable) {
            throw new IllegalStateException(
                    "MinIO not reachable at " + storageProperties.getEndpointOverride() +
                            " — start docker compose minio service.", notReachable);
        }
    }

    private record RegisterResult(UUID userId, String token) {}
}
