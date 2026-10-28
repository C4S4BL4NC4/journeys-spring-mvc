/*
 * Unit test for the MapStruct-generated UserMapper (no Spring context needed).
 *
 * Checks that every profile field of the User entity is copied into the response DTO.
 */
package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsProfileToDto() {
        UUID id = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4);
        User user = User.builder()
                .id(id)
                .name("Jane Doe")
                .email("jane@example.com")
                .photo("jane.jpg")
                .role(Role.GUIDE)
                .createdAt(created)
                .lastModifiedAt(created)
                .build();

        assertThat(mapper.userToUserDTO(user))
                .isEqualTo(new UserDTO(id, "Jane Doe", "jane@example.com", "jane.jpg", Role.GUIDE, created, created));
    }
}
