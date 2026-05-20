package dev.tmmc.reservity.user.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.user.dto.UserSocialDto;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.entity.UserSocial;
import dev.tmmc.reservity.user.repository.UserRepository;
import dev.tmmc.reservity.user.repository.UserSocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSocialService {

    private final UserSocialRepository userSocialRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSocialDto> list(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userSocialRepository.findByUserOrderByDisplayOrderAsc(u).stream()
                .map(s -> new UserSocialDto(s.getKind(), s.getHandle()))
                .toList();
    }

    @Transactional
    public List<UserSocialDto> replace(UUID userId, List<UserSocialDto> incoming) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userSocialRepository.deleteByUser(u);
        userSocialRepository.flush();

        for (int i = 0; i < incoming.size(); i++) {
            UserSocialDto dto = incoming.get(i);
            UserSocial entity = UserSocial.builder()
                    .user(u)
                    .kind(dto.kind())
                    .handle(dto.handle())
                    .displayOrder((short) i)
                    .build();
            userSocialRepository.save(entity);
        }
        return list(userId);
    }
}
