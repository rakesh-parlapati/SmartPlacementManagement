
package com.placement.smartplacementmanagement.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.placement.smartplacementmanagement.entity.Company;
import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.service.CompanyService;
import com.placement.smartplacementmanagement.service.StudentService;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final StudentService studentService;

    public CompanyController(
            CompanyService companyService,
            StudentService studentService) {

        this.companyService = companyService;
        this.studentService = studentService;
    }

    // =========================================================
    // SECURITY HELPER - GET LOGGED-IN USER EMAIL
    // =========================================================

    private String getLoggedInEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return null;
        }

        return authentication.getName();
    }

    // =========================================================
    // SECURITY HELPER - CHECK ADMIN
    // =========================================================

    private boolean isAdmin() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );
    }

    // =========================================================
    // ADD COMPANY
    // ADMIN ONLY
    // =========================================================

    @PostMapping
    public ResponseEntity<?> addCompany(
            @RequestBody Company company) {

        if (!isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only administrators can add companies"
                    );
        }

        return ResponseEntity.ok(
                companyService.saveCompany(company)
        );
    }

    // =========================================================
    // GET ALL COMPANIES
    // ADMIN + STUDENT
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getAllCompanies() {

        return ResponseEntity.ok(
                companyService.getAllCompanies()
        );
    }

    // =========================================================
    // GET COMPANY BY ID
    // ADMIN + STUDENT
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(
            @PathVariable Integer id) {

        Company company =
                companyService.getCompanyById(id);

        if (company == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(company);
    }

    // =========================================================
    // UPDATE COMPANY
    // ADMIN ONLY
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(
            @PathVariable Integer id,
            @RequestBody Company updatedCompany) {

        if (!isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only administrators can update companies"
                    );
        }

        Company company =
                companyService.getCompanyById(id);

        if (company == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        company.setCompanyName(
                updatedCompany.getCompanyName()
        );

        company.setEmail(
                updatedCompany.getEmail()
        );

        company.setLocation(
                updatedCompany.getLocation()
        );

        company.setJobRole(
                updatedCompany.getJobRole()
        );

        company.setPackageAmount(
                updatedCompany.getPackageAmount()
        );

        company.setEligibility(
                updatedCompany.getEligibility()
        );

        company.setDescription(
                updatedCompany.getDescription()
        );

        return ResponseEntity.ok(
                companyService.saveCompany(company)
        );
    }

    // =========================================================
    // DELETE COMPANY
    // ADMIN ONLY
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCompany(
            @PathVariable Integer id) {

        if (!isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only administrators can delete companies"
                    );
        }

        Company company =
                companyService.getCompanyById(id);

        if (company == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        companyService.deleteCompany(id);

        return ResponseEntity.ok(
                "Company deleted successfully"
        );
    }

    // =========================================================
    // CHECK STUDENT ELIGIBILITY
    // ADMIN = ANY STUDENT
    // STUDENT = OWN PROFILE ONLY
    // =========================================================

    @GetMapping("/{id}/eligibility/{studentId}")
    public ResponseEntity<?> checkEligibility(
            @PathVariable Integer id,
            @PathVariable Integer studentId) {

        // =====================================================
        // FIND COMPANY
        // =====================================================

        Company company =
                companyService.getCompanyById(id);

        if (company == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // =====================================================
        // FIND REQUESTED STUDENT
        // =====================================================

        Student student =
                studentService.getStudentById(studentId);

        if (student == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Student not found");
        }

        // =====================================================
        // SECURITY CHECK
        //
        // ADMIN:
        // Can check any student.
        //
        // STUDENT:
        // Can check only their own student ID.
        // =====================================================

        if (!isAdmin()) {

            String loggedInEmail =
                    getLoggedInEmail();

            if (loggedInEmail == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Login is required");
            }

            if (student.getEmail() == null ||
                    !student.getEmail()
                            .equalsIgnoreCase(
                                    loggedInEmail
                            )) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                "You are not authorized to check another student's eligibility"
                        );
            }
        }

        // =====================================================
        // CHECK STUDENT CGPA
        // =====================================================

        if (student.getCgpa() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student CGPA is not available"
                    );
        }

        // =====================================================
        // CHECK COMPANY ELIGIBILITY
        // =====================================================

        if (company.getEligibility() == null ||
                company.getEligibility()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Company eligibility is not available"
                    );
        }

        // =====================================================
        // READ ELIGIBILITY VALUE
        // =====================================================

        String eligibilityText =
                company.getEligibility().trim();

        String numberOnly =
                eligibilityText
                        .replaceAll("[^0-9.]", "");

        double requiredPercentage;

        try {

            requiredPercentage =
                    Double.parseDouble(numberOnly);

        } catch (NumberFormatException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid company eligibility value"
                    );
        }

        // =====================================================
        // CONVERT CGPA TO PERCENTAGE
        // =====================================================

        double studentPercentage =
                student.getCgpa() * 10;

        // =====================================================
        // CHECK ELIGIBILITY
        // =====================================================

        boolean eligible =
                studentPercentage >= requiredPercentage;

        // =====================================================
        // CREATE RESPONSE
        // =====================================================

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "eligible",
                eligible
        );

        response.put(
                "studentCgpa",
                student.getCgpa()
        );

        response.put(
                "studentPercentage",
                studentPercentage
        );

        response.put(
                "requiredPercentage",
                requiredPercentage
        );

        response.put(
                "companyName",
                company.getCompanyName()
        );

        response.put(
                "message",
                eligible
                        ? "Student is eligible"
                        : "Student is not eligible"
        );

        return ResponseEntity.ok(response);
    }
}

