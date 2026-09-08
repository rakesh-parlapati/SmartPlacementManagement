
package com.placement.smartplacementmanagement.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placement.smartplacementmanagement.entity.PasswordResetToken;
import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.repository.PasswordResetTokenRepository;
import com.placement.smartplacementmanagement.repository.StudentRepository;

@Service
public class PasswordResetService {

    private final StudentRepository studentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // Brevo SMTP verified sender email
    @Value("${MAIL_FROM}")
    private String mailFrom;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            StudentRepository studentRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender) {

        this.studentRepository = studentRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    // =========================
    // GENERATE AND SEND OTP
    // =========================
    @Transactional
    public boolean generateOtp(String email) {

        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        email = email.trim().toLowerCase();

        // Find student
        Student student = studentRepository.findByEmail(email);

        if (student == null) {
            return false;
        }

        // Delete old OTP
        passwordResetTokenRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpNumber);

        // Store hashed OTP
        String otpHash = passwordEncoder.encode(otp);

        // OTP expires after 5 minutes
        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(5);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        email,
                        otpHash,
                        expiresAt
                );

        passwordResetTokenRepository.save(resetToken);

        // =========================
        // SEND OTP EMAIL
        // =========================
        SimpleMailMessage message = new SimpleMailMessage();

        // Use verified Brevo sender
        message.setFrom(mailFrom);

        message.setTo(email);

        message.setSubject("SPMS Password Reset OTP");

        message.setText(
                "Hello,\n\n"
                + "Your Smart Placement Management System "
                + "password reset OTP is:\n\n"
                + otp
                + "\n\nThis OTP is valid for 5 minutes."
                + "\n\nIf you did not request a password reset, "
                + "please ignore this email."
                + "\n\nRegards,\n"
                + "Smart Placement Management System"
        );

        try {

            mailSender.send(message);

            System.out.println(
                    "Password reset OTP email sent successfully to: "
                    + email
            );

            return true;

        } catch (Exception e) {

            System.err.println(
                    "Failed to send password reset OTP email."
            );

            e.printStackTrace();

            return false;
        }
    }

    // =========================
    // VERIFY OTP
    // =========================
    @Transactional
    public boolean verifyOtp(String email, String otp) {

        if (email == null || otp == null) {
            return false;
        }

        email = email.trim().toLowerCase();
        otp = otp.trim();

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(email)
                        .orElse(null);

        if (resetToken == null) {
            return false;
        }

        // Check expiration
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

    // =========================
    // RESET PASSWORD
    // =========================
    @Transactional
    public boolean resetPassword(
            String email,
            String otp,
            String newPassword) {

        if (email == null ||
            otp == null ||
            newPassword == null ||
            newPassword.trim().isEmpty()) {

            return false;
        }

        email = email.trim().toLowerCase();
        otp = otp.trim();

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findTopByEmailOrderByIdDesc(email)
                        .orElse(null);

        if (resetToken == null) {
            return false;
        }

        // Check expiration
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

        // Find student
        Student student =
                studentRepository.findByEmail(email);

        if (student == null) {
            return false;
        }

        // Encode new password
        student.setPassword(
                passwordEncoder.encode(newPassword)
        );

        // Save student
        studentRepository.save(student);

        // Delete used OTP
        passwordResetTokenRepository.delete(resetToken);

        return true;
    }
}

