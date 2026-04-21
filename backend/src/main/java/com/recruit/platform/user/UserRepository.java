package com.recruit.platform.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findById(Long id);
}
