
package com.placement.smartplacementmanagement.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.service.PasswordResetService;
import com.placement.smartplacementmanagement.service.StudentService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentService studentService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            StudentService studentService,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService) {

        this.studentService = studentService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {

            if (email == null ||
                    email.trim().isEmpty() ||
                    password == null ||
                    password.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body("Email and password are required");
            }

            email = email.trim();

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    password
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );

            Map<String, Object> result =
                    new HashMap<>();

            // ADMIN LOGIN
            if ("admin@gmail.com".equalsIgnoreCase(email)) {

                result.put("role", "admin");
                result.put("name", "Administrator");
                result.put("email", email);

                return ResponseEntity.ok(result);
            }

            // STUDENT LOGIN
            Student student =
                    studentService.getStudentByEmail(email);

            if (student == null) {

                SecurityContextHolder.clearContext();

                return ResponseEntity
                        .badRequest()
                        .body("Invalid email or password");
            }

            result.put("role", "student");
            result.put("id", student.getId());
            result.put("name", student.getName());
            result.put("email", student.getEmail());

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            return ResponseEntity
                    .badRequest()
                    .body("Invalid email or password");
        }
    }


    // =========================
    // FORGOT PASSWORD
    // =========================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam("email") String email) {

        if (email == null ||
                email.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("Email is required");
        }

        email = email.trim().toLowerCase();

        passwordResetService.generateOtp(email);

        // Generic response prevents account enumeration
        return ResponseEntity.ok(
                "If an account exists with this email, an OTP has been generated."
        );
    }


    // =========================
    // VERIFY OTP
    // =========================

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp) {

        if (email == null ||
                email.trim().isEmpty() ||
                otp == null ||
                otp.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("Email and OTP are required");
        }

        boolean verified =
                passwordResetService.verifyOtp(
                        email,
                        otp
                );

        if (!verified) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid or expired OTP");
        }

        return ResponseEntity.ok(
                "OTP verified successfully"
        );
    }


    // =========================
    // RESET PASSWORD
    // =========================

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            @RequestParam("newPassword") String newPassword) {

        if (email == null ||
                email.trim().isEmpty() ||
                otp == null ||
                otp.trim().isEmpty() ||
                newPassword == null ||
                newPassword.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("All fields are required");
        }

        if (newPassword.length() < 6) {

            return ResponseEntity
                    .badRequest()
                    .body("Password must contain at least 6 characters");
        }

        boolean reset =
                passwordResetService.resetPassword(
                        email,
                        otp,
                        newPassword
                );

        if (!reset) {

            return ResponseEntity
                    .badRequest()
                    .body("Password reset failed");
        }

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }


    // =========================
    // LOGOUT
    // =========================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        return ResponseEntity.ok(
                "Logged out successfully"
        );
    }
}

