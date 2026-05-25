package com.example.menubot.repository;

import com.example.menubot.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Profile("legacy")
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByApprovedFalse();
    List<User> findByAdminFalse();
}