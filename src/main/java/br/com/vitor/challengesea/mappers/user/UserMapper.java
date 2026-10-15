package br.com.vitor.challengesea.mappers.user;

import br.com.vitor.challengesea.mappers.BaseMapperConfig;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.CreateUserDTO;
import br.com.vitor.challengesea.model.user.RegisterRequestDTO;
import br.com.vitor.challengesea.model.user.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Passwords are never mapped: the services hash them into {@code passwordHash}. */
@Mapper(config = BaseMapperConfig.class)
public interface UserMapper {
    UserResponseDTO toResponse(AppUser user);

    @Mapping(target = "role", constant = "CLIENT")
    AppUser toEntity(RegisterRequestDTO dto);

    AppUser toEntity(CreateUserDTO dto);
}
