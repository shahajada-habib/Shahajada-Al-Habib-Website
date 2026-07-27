package com.blogcms.user;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByUsernameAndPublicProfileTrue(String username);

    @Query("""
            SELECT u FROM User u
            WHERE u.publicProfile = true
              AND u.role IN ('reporter', 'editor', 'admin')
            ORDER BY COALESCE(u.displayName, u.username) ASC, u.username ASC
            """)
    List<User> findPublicJournalists();

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'active'
              AND u.role IN ('reporter', 'editor', 'admin')
            ORDER BY COALESCE(u.displayName, u.username) ASC, u.username ASC
            """)
    List<User> findActiveWriters();
}
