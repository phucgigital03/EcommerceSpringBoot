package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByAccessToken(String jwt);

    @Query("SELECT u.signUpMethod, COUNT(u.userId) FROM User u " +
            "GROUP BY u.signUpMethod"
    )
    List<Object[]> getRegisterMethod();
}
