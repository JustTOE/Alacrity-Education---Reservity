package dev.tmmc.reservity.auth.repository;

import dev.tmmc.reservity.auth.entity.UserBackupCode;
import dev.tmmc.reservity.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBackupCodeRepository extends JpaRepository<UserBackupCode, UUID> {

    List<UserBackupCode> findByUserAndUsedAtIsNull(User user);

    void deleteByUser(User user);

    long countByUserAndUsedAtIsNull(User user);
}
