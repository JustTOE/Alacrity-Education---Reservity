package dev.tmmc.reservity.user.repository;

import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.entity.UserSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSocialRepository extends JpaRepository<UserSocial, UUID> {

    List<UserSocial> findByUserOrderByDisplayOrderAsc(User user);

    @Transactional
    void deleteByUser(User user);
}
