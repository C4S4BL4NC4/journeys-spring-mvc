/*
 * Database access for users. Spring Data JPA implements this interface at runtime.
 *
 *   JpaRepository             CRUD and paging methods
 *   JpaSpecificationExecutor  dynamic filtered queries built from Specs
 *   findAllByRoleIn           derived query (Spring writes the SQL from the method name);
 *                             BootstrapData uses it to find the guides to assign to tours
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    List<User> findAllByRoleIn(Collection<Role> roles);
}
