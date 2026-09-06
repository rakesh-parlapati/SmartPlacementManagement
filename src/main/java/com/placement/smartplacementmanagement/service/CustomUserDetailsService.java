
package com.placement.smartplacementmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.repository.StudentRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public CustomUserDetailsService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // ADMIN LOGIN
        if (adminEmail.equalsIgnoreCase(email)) {

            return User.withUsername(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles("ADMIN")
                    .build();
        }

        // STUDENT LOGIN
        Student student =
                studentRepository.findByEmail(email);

        if (student == null) {

            throw new UsernameNotFoundException(
                    "User not found: " + email
            );
        }

        return User.withUsername(student.getEmail())
                .password(student.getPassword())
                .roles("STUDENT")
                .build();
    }
}

