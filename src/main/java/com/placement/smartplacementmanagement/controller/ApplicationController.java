
package com.placement.smartplacementmanagement.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.placement.smartplacementmanagement.entity.Application;
import com.placement.smartplacementmanagement.entity.Company;
import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.service.ApplicationService;
import com.placement.smartplacementmanagement.service.CompanyService;
import com.placement.smartplacementmanagement.service.StudentService;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CompanyService companyService;
    private final StudentService studentService;

    private final String UPLOAD_DIR = "uploads/resumes/";

    public ApplicationController(
            ApplicationService applicationService,
            CompanyService companyService,
            StudentService studentService) {

        this.applicationService = applicationService;
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
    // SECURITY HELPER - CHECK APPLICATION OWNER
    // =========================================================

    private boolean isApplicationOwner(
            Application application) {

        if (application == null) {
            return false;
        }

        String loggedInEmail =
                getLoggedInEmail();

        if (loggedInEmail == null ||
                application.getStudentEmail() == null) {
            return false;
        }

        return application
                .getStudentEmail()
                .equalsIgnoreCase(loggedInEmail);
    }

    // =========================================================
    // SECURITY HELPER - ADMIN OR OWNER
    // =========================================================

    private boolean isAdminOrOwner(
            Application application) {

        return isAdmin()
                || isApplicationOwner(application);
    }

    // =========================================================
    // SAVE APPLICATION + RESUME
    // =========================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> saveApplication(

            @RequestParam("studentName")
            String studentName,

            @RequestParam("studentEmail")
            String studentEmail,

            @RequestParam("phone")
            String phone,

            @RequestParam("cgpa")
            double cgpa,

            @RequestParam("companyName")
            String companyName,

            @RequestParam("resume")
            MultipartFile resume)

            throws IOException {

        // =====================================================
        // ONLY LOGGED-IN STUDENT CAN CREATE APPLICATION
        // =====================================================

        if (isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Administrators cannot submit student applications"
                    );
        }

        String loggedInEmail =
                getLoggedInEmail();

        if (loggedInEmail == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Login is required");
        }

        // =====================================================
        // FIND STUDENT USING LOGGED-IN EMAIL
        // =====================================================

        Student student =
                studentService.getStudentByEmail(
                        loggedInEmail
                );

        if (student == null) {

            return ResponseEntity
                    .badRequest()
                    .body("Student account not found");
        }

        // =====================================================
        // IGNORE SPOOFED FRONTEND STUDENT EMAIL
        // =====================================================

        studentEmail =
                student.getEmail();

        // =====================================================
        // VALIDATE PHONE
        // =====================================================

        if (phone == null ||
                !phone.matches("\\d{10}")) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Phone number must contain exactly 10 digits"
                    );
        }

        // =====================================================
        // VALIDATE CGPA
        // =====================================================

        if (cgpa < 0 || cgpa > 10) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "CGPA must be between 0 and 10"
                    );
        }

        // =====================================================
        // USE CGPA FROM DATABASE
        // =====================================================

        if (student.getCgpa() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student CGPA is not available"
                    );
        }

        if (Math.abs(
                student.getCgpa() - cgpa
        ) > 0.001) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Student CGPA does not match profile"
                    );
        }

        // =====================================================
        // FIND COMPANY
        // =====================================================

        Company company =
                findCompanyByName(companyName);

        if (company == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Company not found"
                    );
        }

        // =====================================================
        // CHECK COMPANY ELIGIBILITY
        // =====================================================

        String eligibilityText =
                company.getEligibility();

        if (eligibilityText == null ||
                eligibilityText.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Company eligibility is not available"
                    );
        }

        String numberOnly =
                eligibilityText
                        .trim()
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

        if (studentPercentage < requiredPercentage) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "You are not eligible for "
                                    + company.getCompanyName()
                                    + ". Required: "
                                    + requiredPercentage
                                    + "%, Your percentage: "
                                    + studentPercentage
                                    + "%"
                    );
        }

        // =====================================================
        // CHECK DUPLICATE APPLICATION
        // =====================================================

        boolean alreadyApplied =
                applicationService.alreadyApplied(
                        studentEmail,
                        company.getCompanyName()
                );

        if (alreadyApplied) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "You have already applied to "
                                    + company.getCompanyName()
                    );
        }

        // =====================================================
        // VALIDATE RESUME
        // =====================================================

        if (resume == null ||
                resume.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Please upload your resume"
                    );
        }

        String originalFileName =
                resume.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid resume file"
                    );
        }

        // =====================================================
        // SECURE ORIGINAL FILE NAME
        // =====================================================

        /*
         * Extract only the filename portion.
         *
         * This prevents filenames such as:
         *
         * ../../malicious.pdf
         *
         * from being interpreted as directory paths.
         */
        String safeOriginalFileName =
                Paths.get(originalFileName)
                        .getFileName()
                        .toString();

        if (safeOriginalFileName.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid resume filename"
                    );
        }

        // =====================================================
        // VALIDATE PDF EXTENSION
        // =====================================================

        if (!safeOriginalFileName
                .toLowerCase()
                .endsWith(".pdf")) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Only PDF resume is allowed"
                    );
        }

        // =====================================================
        // VALIDATE CONTENT TYPE
        // =====================================================

        if (resume.getContentType() != null &&
                !resume.getContentType()
                        .equalsIgnoreCase(
                                MediaType.APPLICATION_PDF_VALUE
                        )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Only PDF resume is allowed"
                    );
        }

        // =====================================================
        // CREATE APPLICATION
        // =====================================================

        Application application =
                new Application();

        application.setStudentName(
                student.getName()
        );

        application.setStudentEmail(
                student.getEmail()
        );

        application.setPhone(
                student.getPhone()
        );

        application.setCgpa(
                student.getCgpa()
        );

        application.setCompanyName(
                company.getCompanyName()
        );

        // =====================================================
        // CREATE UPLOAD FOLDER
        // =====================================================

        Path uploadPath =
                Paths.get(UPLOAD_DIR)
                        .toAbsolutePath()
                        .normalize();

        if (!Files.exists(uploadPath)) {

            Files.createDirectories(uploadPath);
        }

        // =====================================================
        // CREATE SECURE UNIQUE FILE NAME
        // =====================================================

        String fileName =
                UUID.randomUUID()
                        .toString()
                        + ".pdf";

        Path filePath =
                uploadPath
                        .resolve(fileName)
                        .normalize();

        // =====================================================
        // FINAL PATH SECURITY CHECK
        // =====================================================

        if (!filePath.startsWith(uploadPath)) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid resume file path"
                    );
        }

        // =====================================================
        // SAVE RESUME
        // =====================================================

        Files.copy(
                resume.getInputStream(),
                filePath
        );

        // =====================================================
        // SAVE RELATIVE FILE PATH IN DATABASE
        // =====================================================

        application.setResume(
                UPLOAD_DIR + fileName
        );

        // =====================================================
        // SAVE APPLICATION
        // =====================================================

        Application savedApplication =
                applicationService
                        .saveApplication(application);

        return ResponseEntity.ok(
                savedApplication
        );
    }

    // =========================================================
    // FIND COMPANY BY NAME
    // =========================================================

    private Company findCompanyByName(
            String companyName) {

        if (companyName == null ||
                companyName.isBlank()) {

            return null;
        }

        return companyService
                .getAllCompanies()
                .stream()
                .filter(company ->
                        company.getCompanyName() != null &&
                        company.getCompanyName()
                                .equalsIgnoreCase(
                                        companyName.trim()
                                )
                )
                .findFirst()
                .orElse(null);
    }

    // =========================================================
    // GET ALL APPLICATIONS
    // ADMIN = ALL APPLICATIONS
    // STUDENT = OWN APPLICATIONS ONLY
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getAllApplications() {

        if (isAdmin()) {

            return ResponseEntity.ok(
                    applicationService
                            .getAllApplications()
            );
        }

        String loggedInEmail =
                getLoggedInEmail();

        if (loggedInEmail == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Login is required");
        }

        List<Application> applications =
                applicationService
                        .getAllApplications()
                        .stream()
                        .filter(application ->
                                application.getStudentEmail() != null
                                && application
                                        .getStudentEmail()
                                        .equalsIgnoreCase(
                                                loggedInEmail
                                        )
                        )
                        .toList();

        return ResponseEntity.ok(applications);
    }

    // =========================================================
    // GET APPLICATION BY ID
    // ADMIN OR APPLICATION OWNER
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicationById(
            @PathVariable Integer id) {

        Application application =
                applicationService
                        .getApplicationById(id);

        if (application == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        if (!isAdminOrOwner(application)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this application"
                    );
        }

        return ResponseEntity.ok(application);
    }

    // =========================================================
    // UPDATE APPLICATION STATUS
    // ADMIN ONLY
    // =========================================================

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateApplicationStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        // =====================================================
        // ADMIN CHECK
        // =====================================================

        if (!isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only administrators can update application status"
                    );
        }

        String status =
                request.get("status");

        // =====================================================
        // ALLOW ONLY APPROVED OR REJECTED
        // =====================================================

        if (status == null ||
                (!status.equals("Approved") &&
                 !status.equals("Rejected"))) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid application status"
                    );
        }

        // =====================================================
        // FIND APPLICATION
        // =====================================================

        Application application =
                applicationService
                        .getApplicationById(id);

        if (application == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // =====================================================
        // UPDATE STATUS
        // =====================================================

        application.setStatus(status);

        applicationService
                .saveApplication(application);

        return ResponseEntity.ok(
                "Application "
                        + status
                        + " successfully"
        );
    }

    // =========================================================
    // OPEN RESUME PDF
    // ADMIN OR APPLICATION OWNER
    // =========================================================

    @GetMapping("/{id}/resume")
    public ResponseEntity<?> getResume(
            @PathVariable Integer id)
            throws IOException {

        Application application =
                applicationService
                        .getApplicationById(id);

        if (application == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        if (!isAdminOrOwner(application)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this resume"
                    );
        }

        String resumePath =
                application.getResume();

        if (resumePath == null ||
                resumePath.isBlank()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        Path uploadPath =
                Paths.get(UPLOAD_DIR)
                        .toAbsolutePath()
                        .normalize();

        Path path =
                Paths.get(resumePath)
                        .toAbsolutePath()
                        .normalize();

        // =====================================================
        // RESUME PATH SECURITY CHECK
        // =====================================================

        if (!path.startsWith(uploadPath)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Invalid resume path");
        }

        if (!Files.exists(path) ||
                !Files.isRegularFile(path)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        byte[] fileData =
                Files.readAllBytes(path);

        ByteArrayResource resource =
                new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(fileData.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + path.getFileName()
                                        .toString()
                                + "\""
                )
                .body(resource);
    }

    // =========================================================
    // DELETE APPLICATION
    // ADMIN OR APPLICATION OWNER
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(
            @PathVariable Integer id) {

        Application application =
                applicationService
                        .getApplicationById(id);

        if (application == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // =====================================================
        // ADMIN OR OWNER CHECK
        // =====================================================

        if (!isAdminOrOwner(application)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to delete this application"
                    );
        }

        // =====================================================
        // DELETE RESUME FILE
        // =====================================================

        String resumePath =
                application.getResume();

        if (resumePath != null &&
                !resumePath.isBlank()) {

            Path uploadPath =
                    Paths.get(UPLOAD_DIR)
                            .toAbsolutePath()
                            .normalize();

            Path path =
                    Paths.get(resumePath)
                            .toAbsolutePath()
                            .normalize();

            /*
             * Delete only files inside the resume folder.
             */
            if (path.startsWith(uploadPath)) {

                try {

                    Files.deleteIfExists(path);

                } catch (IOException e) {

                    System.out.println(
                            "Could not delete resume: "
                                    + e.getMessage()
                    );
                }
            }
        }

        // =====================================================
        // DELETE APPLICATION
        // =====================================================

        applicationService
                .deleteApplication(id);

        return ResponseEntity.ok(
                "Application deleted successfully"
        );
    }
}

