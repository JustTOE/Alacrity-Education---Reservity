package dev.tmmc.reservity.user.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.user.dto.UserResponse;
import dev.tmmc.reservity.user.dto.UserUpdateRequest;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.mapper.UserMapper;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(loadActive(id));
    }


    @Transactional(readOnly = true)
    public UserResponse getByHandle(String handle) {
        return userMapper.toResponse(
                userRepository.findByHandle(handle)
                        .filter(u -> !u.isDeleted())
                        .orElseThrow(() -> new EntityNotFoundException("User not found"))
        );
    }

    @Transactional
    public UserResponse update(UUID userId, UserUpdateRequest req) {
        User u = loadActive(userId);

        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.realName() != null) u.setRealName(req.realName());
        if (req.initials() != null && !req.initials().isBlank()) u.setInitials(req.initials());
        if (req.bio() != null) u.setBio(req.bio());
        if (req.coverGradient() != null) u.setCoverGradient(req.coverGradient());

        if (req.handle() != null && !req.handle().equals(u.getHandle())) {
            if (userRepository.existsByHandle(req.handle())) {
                throw new DataIntegrityViolationException("Handle already taken");
            }
            u.setHandle(req.handle());
        }

        return userMapper.toResponse(userRepository.save(u));
    }

    private User loadActive(UUID id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
