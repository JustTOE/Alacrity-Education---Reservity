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
import dev.tmmc.reservity.spaces.entity.SpaceStatus;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceImageValidationIntegrationTest {

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
    void unauth_post_returns_403() throws Exception {
        Building b = buildingRepository.save(Building.builder()
                .name("Hawthorn Sciences").shortCode("HSC").campusName("Main")
                .pinX(BigDecimal.valueOf(0.5)).pinY(BigDecimal.valueOf(0.5)).build());
        User owner = userRepository.save(User.builder()
                .email("owner-v@m3.local")
                .passwordHash("$2a$12$0000000000000000000000000000000000000000000000000000")
                .handle("ownerv")
                .displayName("OwnerV")
                .initials("OV")
                .memberSince(java.time.LocalDate.now())
                .build());
        createSpaceOwnedBy(owner.getId(), b, "lab-noauth", "Lab Noauth");

        mvc.perform(multipart("/api/spaces/lab-noauth/images")
                        .file(new MockMultipartFile("file", "x.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrong_mime_returns_415() throws Exception {
        UploadContext ctx = setupOwnerAndSpace("wrong-mime@m3.local", "wrongmime", "lab-mime");
        mvc.perform(multipart("/api/spaces/lab-mime/images")
                        .file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void non_owner_returns_403() throws Exception {
        // Owner registers and owns the space.
        UploadContext owner = setupOwnerAndSpace("real-owner@m3.local", "real_owner", "lab-foreign");
        // A second user registers; gets their own token but doesn't own the space.
        RegisterRequest reg = new RegisterRequest("intruder@m3.local", "secret-password-123",
                "Intruder", "intruder");
        MvcResult intruderRes = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(intruderRes.getResponse().getContentAsByteArray());
        String intruderToken = body.get("accessToken").asText();

        mvc.perform(multipart("/api/spaces/lab-foreign/images")
                        .file(tinyPng())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    private UploadContext setupOwnerAndSpace(String email, String handle, String slug) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "secret-password-123", "User " + handle, handle);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        String token = body.get("accessToken").asText();
        UUID userId = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElseThrow().getId();

        String safeCode = handle.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (safeCode.length() < 2) safeCode = "BX" + safeCode;
        if (safeCode.length() > 8) safeCode = safeCode.substring(0, 8);
        Building b = buildingRepository.save(Building.builder()
                .name("Hawthorn " + handle).shortCode(safeCode)
                .campusName("Main")
                .pinX(BigDecimal.valueOf(0.5)).pinY(BigDecimal.valueOf(0.5)).build());
        createSpaceOwnedBy(userId, b, slug, "Space " + slug);
        return new UploadContext(userId, token);
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
                .floor((short) 1)
                .room("1")
                .seats((short) 8)
                .areaSqm(BigDecimal.valueOf(30))
                .pricePerHour(BigDecimal.valueOf(0))
                .currency("USD")
                .blurb(name + " test")
                .description("test")
                .amenities(emptyArr)
                .rules(emptyArr)
                .operatingHours(hours)
                .pinX(BigDecimal.valueOf(0.5))
                .pinY(BigDecimal.valueOf(0.5))
                .status(SpaceStatus.PUBLISHED)
                .build());
    }

    private static MockMultipartFile tinyPng() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return new MockMultipartFile("file", "tiny.png", "image/png", out.toByteArray());
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

    private record UploadContext(UUID userId, String token) {}
}
