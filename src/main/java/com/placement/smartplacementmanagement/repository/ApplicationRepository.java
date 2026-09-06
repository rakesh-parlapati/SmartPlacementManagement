
package com.placement.smartplacementmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placement.smartplacementmanagement.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    boolean existsByStudentEmailAndCompanyName(
            String studentEmail,
            String companyName
    );
}

