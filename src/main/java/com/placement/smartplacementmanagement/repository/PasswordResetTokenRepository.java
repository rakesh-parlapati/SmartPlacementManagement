
package com.placement.smartplacementmanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placement.smartplacementmanagement.entity.PasswordResetToken;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findTopByEmailOrderByIdDesc(
            String email
    );

    void deleteByEmail(String email);
}

