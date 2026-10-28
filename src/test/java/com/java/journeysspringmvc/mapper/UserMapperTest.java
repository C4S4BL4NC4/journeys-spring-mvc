package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsDtoToEntityKeepingServerDefaults() {
        UserDTO dto = new UserDTO(null, "Jane", "jane@example.com", null, Role.ADMIN,
                "password123", "password123", null, null);

        User user = mapper.userDTOToUser(dto);

        assertThat(user.getPhoto()).isEqualTo(User.DEFAULT_PHOTO);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getPassword()).isNull();
    }

    @Test
    void keepsPhotoWhenProvided() {
        UserDTO dto = new UserDTO(null, "Jane", "jane@example.com", "jane.jpg", null,
                "password123", "password123", null, null);

        assertThat(mapper.userDTOToUser(dto).getPhoto()).isEqualTo("jane.jpg");
    }
}
