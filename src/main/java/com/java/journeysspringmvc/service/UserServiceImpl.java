package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.UserMapper;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.PageRequests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.java.journeysspringmvc.repository.UserSpecifications.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final static Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private final static Set<String> SORTABLE_FIELDS = Set.of("name", "email", "role", "createdAt");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(String name, Role role, String sortBy, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PageRequests.of(sortBy, pageNumber, pageSize, SORTABLE_FIELDS, DEFAULT_SORT);

        Specification<User> spec = Specification.allOf(
                isActive(),
                nameContains(name),
                hasRole(role));

        return userRepository.findAll(spec, pageRequest).map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(UUID id) {
        return findActiveUser(id).map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional
    public UserDTO saveNewUser(UserDTO userDTO) {
        User user = userMapper.userDTOToUser(userDTO);
        user.setId(null);
        // New accounts are always plain users; admins promote them through updateUser
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(userDTO.password()));

        return userMapper.userToUserDTO(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public Optional<UserDTO> updateUser(UUID id, UserUpdateDTO userUpdateDTO) {
        return findActiveUser(id).map(user -> {
            userMapper.updateUserFromDTO(userUpdateDTO, user);

            return userMapper.userToUserDTO(userRepository.saveAndFlush(user));
        });
    }

    // Deactivates rather than deletes, so the account's history (e.g. guided tours) stays intact
    @Override
    @Transactional
    public boolean deleteUser(UUID id) {
        return findActiveUser(id).map(user -> {
            user.setActive(false);
            return true;
        }).orElse(false);
    }

    private Optional<User> findActiveUser(UUID id) {
        return userRepository.findById(id).filter(User::getActive);
    }
}
