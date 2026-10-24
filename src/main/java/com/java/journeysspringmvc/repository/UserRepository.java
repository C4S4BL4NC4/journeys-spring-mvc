package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
}
