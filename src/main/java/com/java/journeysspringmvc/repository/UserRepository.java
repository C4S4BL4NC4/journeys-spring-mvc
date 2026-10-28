package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByRoleIn(Collection<Role> roles);
}
