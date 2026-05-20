package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.storage.StorageService;
import dev.tmmc.reservity.common.storage.StoredObject;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.spaces.dto.ImageUpdateRequest;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.spaces.repository.SpaceImageRepository;
import dev.tmmc.reservity.user.entity.AccountType;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceImageService {

    static final int MAX_IMAGES_PER_SPACE = 10;
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final SpaceService spaceService;
    private final SpaceImageRepository imageRepository;
    private final StorageService storage;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;

    @Transactional
    public SpaceImage upload(String slugOrId, MultipartFile file, String altText, Boolean isPrimary, UUID actorUserId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Allowed image types: image/jpeg, image/png, image/webp");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image exceeds " + MAX_BYTES + " bytes");
        }

        Space space = resolveSpace(slugOrId);
        User actor = requireUser(actorUserId);
        requireSpaceWriteAccess(space, actor);

        if (imageRepository.countBySpaceId(space.getId()) >= MAX_IMAGES_PER_SPACE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Space already has " + MAX_IMAGES_PER_SPACE + " images");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage probed = ImageIO.read(new ByteArrayInputStream(bytes));
            if (probed != null) {
                width = probed.getWidth();
                height = probed.getHeight();
            }
        } catch (IOException probeFailed) {
            log.debug("Could not probe image dimensions for {}: {}", file.getOriginalFilename(), probeFailed.getMessage());
        }

        String ext = extensionFor(contentType);
        String key = "spaces/" + space.getId() + "/" + UUID.randomUUID() + "." + ext;
        StoredObject stored = storage.upload(new ByteArrayInputStream(bytes), bytes.length, contentType, key);

        List<SpaceImage> existing = imageRepository.findBySpaceIdOrderByDisplayOrderAsc(space.getId());
        boolean noneExist = existing.isEmpty();
        boolean shouldBePrimary = Boolean.TRUE.equals(isPrimary) || noneExist;

        if (shouldBePrimary) {
            for (SpaceImage prev : existing) {
                if (prev.isPrimary()) {
                    prev.setPrimary(false);
                    imageRepository.save(prev);
                }
            }
            imageRepository.flush(); // ensure the partial unique index releases before new insert
        }

        short nextDisplayOrder = (short) existing.size();
        SpaceImage created = SpaceImage.builder()
                .space(space)
                .s3Key(stored.key())
                .url(stored.url())
                .altText(trimToNull(altText))
                .width(width)
                .height(height)
                .byteSize((long) bytes.length)
                .contentType(contentType)
                .displayOrder(nextDisplayOrder)
                .primary(shouldBePrimary)
                .uploadedBy(actor)
                .build();
        return imageRepository.save(created);
    }

    @Transactional(readOnly = true)
    public List<SpaceImage> list(String slugOrId) {
        Space space = resolveSpace(slugOrId);
        return imageRepository.findBySpaceIdOrderByDisplayOrderAsc(space.getId());
    }

    @Transactional
    public SpaceImage update(String slugOrId, UUID imageId, ImageUpdateRequest req, UUID actorUserId) {
        Space space = resolveSpace(slugOrId);
        User actor = requireUser(actorUserId);
        requireSpaceWriteAccess(space, actor);

        SpaceImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("SpaceImage", imageId));
        if (!img.getSpace().getId().equals(space.getId())) {
            throw new EntityNotFoundException("SpaceImage", imageId);
        }

        if (req.altText() != null) {
            img.setAltText(trimToNull(req.altText()));
        }
        if (req.displayOrder() != null) {
            img.setDisplayOrder(req.displayOrder());
        }
        if (Boolean.TRUE.equals(req.isPrimary()) && !img.isPrimary()) {
            for (SpaceImage other : imageRepository.findBySpaceIdOrderByDisplayOrderAsc(space.getId())) {
                if (other.isPrimary() && !other.getId().equals(img.getId())) {
                    other.setPrimary(false);
                    imageRepository.save(other);
                }
            }
            imageRepository.flush();
            img.setPrimary(true);
        }
        return imageRepository.save(img);
    }

    @Transactional
    public void delete(String slugOrId, UUID imageId, UUID actorUserId) {
        Space space = resolveSpace(slugOrId);
        User actor = requireUser(actorUserId);
        requireSpaceWriteAccess(space, actor);

        SpaceImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("SpaceImage", imageId));
        if (!img.getSpace().getId().equals(space.getId())) {
            throw new EntityNotFoundException("SpaceImage", imageId);
        }

        boolean wasPrimary = img.isPrimary();
        String s3Key = img.getS3Key();
        UUID deletedId = img.getId();

        // Find the would-be-next-primary BEFORE deleting, excluding the row we're
        // about to remove. Querying after delete + flush risks Hibernate L1 cache
        // returning the deleted row and re-inserting it on save.
        SpaceImage nextPrimary = wasPrimary
                ? imageRepository.findFirstBySpaceIdAndIdNotOrderByDisplayOrderAsc(space.getId(), deletedId).orElse(null)
                : null;

        // Clear the primary flag first so the partial unique index releases before
        // we promote the next image.
        if (wasPrimary) {
            img.setPrimary(false);
            imageRepository.saveAndFlush(img);
        }

        imageRepository.delete(img);
        imageRepository.flush();

        if (nextPrimary != null) {
            nextPrimary.setPrimary(true);
            imageRepository.save(nextPrimary);
        }

        try {
            storage.delete(s3Key);
        } catch (RuntimeException storageFailed) {
            log.warn("S3 delete failed for {}; row already removed", s3Key, storageFailed);
        }
    }

    private Space resolveSpace(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return spaceService.requireById(id);
        } catch (IllegalArgumentException notUuid) {
            return spaceService.requireBySlug(slugOrId);
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private void requireSpaceWriteAccess(Space space, User actor) {
        if (actor.getAccountType() == AccountType.ADMIN) return;

        if (space.getOwnerType() == SpaceOwnerType.USER) {
            if (space.getOwnerId().equals(actor.getId())) return;
        } else if (space.getOwnerType() == SpaceOwnerType.ORGANIZATION) {
            Optional<Organization> org = organizationRepository.findById(space.getOwnerId());
            if (org.isPresent() && orgMembershipRepository.existsByUserAndOrganization(actor, org.get())) {
                return;
            }
        }
        throw new ForbiddenOperationException("Only the space owner can modify images");
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "bin";
        };
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
