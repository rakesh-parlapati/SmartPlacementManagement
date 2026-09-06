package com.placement.smartplacementmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placement.smartplacementmanagement.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    // Find student by email for login
    Student findByEmail(String email);
}