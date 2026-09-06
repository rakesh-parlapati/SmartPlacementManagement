
package com.placement.smartplacementmanagement.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.placement.smartplacementmanagement.entity.PasswordResetToken;
import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.repository.PasswordResetTokenRepository;
import com.placement.smartplacementmanagement.repository.StudentRepository;

@Service
public class PasswordResetService {

    private final StudentRepository studentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            StudentRepository studentRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateOtp(String email) {

        email = email.trim().toLowerCase();

        Student student = studentRepository.findByEmail(email);

        if (student == null) {
            return null;
        }

        // Remove any previous OTP
        passwordResetTokenRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        int otpNumber =
                100000 + secureRandom.nextInt(900000);

        String otp = String.valueOf(otpNumber);

        // Store only hashed OTP
        String otpHash =
                passwordEncoder.encode(otp);

        // OTP valid for 5 minutes
        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(5);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        email,
                        otpHash,
                        expiresAt
                );

        passwordResetTokenRepository.save(resetToken);

        // For testing: OTP appears in Eclipse console
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "SPMS PASSWORD RESET OTP"
        );
        System.out.println(
                "Email: " + email
        );
        System.out.println(
                "OTP: " + otp
        );
        System.out.println(
                "Expires: " + expiresAt
        );
        System.out.println(
                "=========================================="
        );

        return otp;
    }

    public boolean verifyOtp(
            String email,
            String otp) {

        email = email.trim().toLowerCase();

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(email)
                        .orElse(null);

        if (resetToken == null) {
            return false;
        }

        // Check expiry
        if (LocalDateTime.now()
                .isAfter(resetToken.getExpiresAt())) {

            passwordResetTokenRepository.delete(resetToken);

            return false;
        }

        // Maximum 5 attempts
        if (resetToken.getAttempts() >= 5) {

            passwordResetTokenRepository.delete(resetToken);

            return false;
        }

        // Already verified
        if (resetToken.isVerified()) {
            return true;
        }

        // Increase attempt count
        resetToken.setAttempts(
                resetToken.getAttempts() + 1
        );

        // Check OTP
        if (passwordEncoder.matches(
                otp,
                resetToken.getOtpHash())) {

            resetToken.setVerified(true);

            passwordResetTokenRepository.save(resetToken);

            return true;
        }

        passwordResetTokenRepository.save(resetToken);

        return false;
    }

    public boolean resetPassword(
            String email,
            String otp,
            String newPassword) {

        email = email.trim().toLowerCase();

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(email)
                        .orElse(null);

        if (resetToken == null) {
            return false;
        }

        // Check expiry
        if (LocalDateTime.now()
                .isAfter(resetToken.getExpiresAt())) {

            passwordResetTokenRepository.delete(resetToken);

            return false;
        }

        // OTP must be verified first
        if (!resetToken.isVerified()) {
            return false;
        }

        // Verify OTP again
        if (!passwordEncoder.matches(
                otp,
                resetToken.getOtpHash())) {

            return false;
        }

        Student student =
                studentRepository.findByEmail(email);

        if (student == null) {
            return false;
        }

        // Hash the new password
        student.setPassword(
                passwordEncoder.encode(newPassword)
        );

        studentRepository.save(student);

        // Delete used reset token
        passwordResetTokenRepository.delete(resetToken);

        return true;
    }
}

