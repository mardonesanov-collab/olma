package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTgId(Long tgId);
    List<AppUser> findByRole(UserRole role);
}
