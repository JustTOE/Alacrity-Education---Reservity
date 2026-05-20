package dev.tmmc.reservity.user.mapper;

import dev.tmmc.reservity.user.dto.UserResponse;
import dev.tmmc.reservity.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "emailVerified", expression = "java(user.getEmailVerifiedAt() != null)")
    UserResponse toResponse(User user);
}
