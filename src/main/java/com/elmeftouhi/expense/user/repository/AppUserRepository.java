package com.elmeftouhi.expense.user.repository;

import com.elmeftouhi.expense.user.entity.AppUser;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"defaultTenant", "roles"})
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = {"defaultTenant", "roles"})
    Optional<AppUser> findWithDefaultTenantById(Long id);
}

