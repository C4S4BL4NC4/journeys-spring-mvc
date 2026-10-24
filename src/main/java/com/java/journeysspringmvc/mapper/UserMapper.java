package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    User userDTOToUser(UserDTO userDTO);
    UserDTO userToUserDTO(User user);
}
