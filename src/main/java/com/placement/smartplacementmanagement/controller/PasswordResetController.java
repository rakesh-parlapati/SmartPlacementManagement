package com.placement.smartplacementmanagement.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.placement.smartplacementmanagement.service.PasswordResetService;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            PasswordResetService passwordResetService) {

        this.passwordResetService = passwordResetService;
    }

    // =========================
    // SEND OTP
    // POST: /api/password/forgot
    // =========================
    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        Map<String, String> response = new HashMap<>();

        if (email == null || email.trim().isEmpty()) {

            response.put("message", "Email is required");

            return ResponseEntity.badRequest().body(response);
        }

        boolean otpSent =
                passwordResetService.generateOtp(email);

        if (!otpSent) {

            response.put(
                    "message",
                    "No account found with this email"
            );

            return ResponseEntity.badRequest().body(response);
        }

        response.put(
                "message",
                "OTP sent successfully to your email"
        );

        return ResponseEntity.ok(response);
    }


    // =========================
    // VERIFY OTP
    // POST: /api/password/verify-otp
    // =========================
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        Map<String, String> response = new HashMap<>();

        if (email == null || email.trim().isEmpty()
                || otp == null || otp.trim().isEmpty()) {

            response.put(
                    "message",
                    "Email and OTP are required"
            );

            return ResponseEntity.badRequest().body(response);
        }

        boolean verified =
                passwordResetService.verifyOtp(email, otp);

        if (!verified) {

            response.put(
                    "message",
                    "Invalid or expired OTP"
            );

            return ResponseEntity.badRequest().body(response);
        }

        response.put(
                "message",
                "OTP verified successfully"
        );

        return ResponseEntity.ok(response);
    }


    // =========================
    // RESET PASSWORD
    // POST: /api/password/reset
    // =========================
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        Map<String, String> response = new HashMap<>();

        if (email == null || email.trim().isEmpty()
                || otp == null || otp.trim().isEmpty()
                || newPassword == null
                || newPassword.trim().isEmpty()) {

            response.put(
                    "message",
                    "Email, OTP and new password are required"
            );

            return ResponseEntity.badRequest().body(response);
        }

        boolean reset =
                passwordResetService.resetPassword(
                        email,
                        otp,
                        newPassword
                );

        if (!reset) {

            response.put(
                    "message",
                    "Password reset failed. Please verify your OTP again."
            );

            return ResponseEntity.badRequest().body(response);
        }

        response.put(
                "message",
                "Password reset successfully"
        );

        return ResponseEntity.ok(response);
    }
}