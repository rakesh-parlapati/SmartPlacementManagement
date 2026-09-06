
package com.placement.smartplacementmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.placement.smartplacementmanagement.entity.Application;
import com.placement.smartplacementmanagement.repository.ApplicationRepository;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository) {

        this.applicationRepository =
                applicationRepository;
    }

    // =========================================================
    // SAVE APPLICATION
    // =========================================================

    public Application saveApplication(
            Application application) {

        return applicationRepository.save(application);
    }

    // =========================================================
    // CHECK DUPLICATE APPLICATION
    // =========================================================

    public boolean alreadyApplied(
            String studentEmail,
            String companyName) {

        return applicationRepository
                .existsByStudentEmailAndCompanyName(
                        studentEmail,
                        companyName
                );
    }

    // =========================================================
    // GET ALL APPLICATIONS
    // =========================================================

    public List<Application> getAllApplications() {

        return applicationRepository.findAll();
    }

    // =========================================================
    // GET APPLICATION BY ID
    // =========================================================

    public Application getApplicationById(
            Integer id) {

        return applicationRepository
                .findById(id)
                .orElse(null);
    }

    // =========================================================
    // DELETE APPLICATION
    // =========================================================

    public void deleteApplication(
            Integer id) {

        applicationRepository.deleteById(id);
    }

    // =========================================================
    // UPLOAD RESUME
    // =========================================================

    public void uploadResume(
            Integer id,
            MultipartFile resume)
            throws IOException {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElse(null);

        if (application == null) {

            throw new IOException(
                    "Application not found"
            );
        }

        if (resume == null ||
                resume.isEmpty()) {

            throw new IOException(
                    "Resume file is empty"
            );
        }

        String originalFileName =
                resume.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            throw new IOException(
                    "Invalid resume file"
            );
        }

        // -----------------------------------------------------
        // Validate PDF
        // -----------------------------------------------------

        if (!originalFileName
                .toLowerCase()
                .endsWith(".pdf")) {

            throw new IOException(
                    "Only PDF resume is allowed"
            );
        }

        // -----------------------------------------------------
        // Create upload folder
        // -----------------------------------------------------

        Path uploadPath =
                Paths.get("uploads/resumes");

        if (!Files.exists(uploadPath)) {

            Files.createDirectories(uploadPath);
        }

        // -----------------------------------------------------
        // Delete old resume if present
        // -----------------------------------------------------

        String oldResume =
                application.getResume();

        if (oldResume != null &&
                !oldResume.isBlank()) {

            Path oldPath =
                    Paths.get(oldResume);

            try {

                Files.deleteIfExists(oldPath);

            } catch (IOException e) {

                System.out.println(
                        "Could not delete old resume: "
                                + e.getMessage()
                );
            }
        }

        // -----------------------------------------------------
        // Create unique file name
        // -----------------------------------------------------

        String fileName =
                System.currentTimeMillis()
                        + "_"
                        + originalFileName;

        Path filePath =
                uploadPath.resolve(fileName);

        // -----------------------------------------------------
        // Save resume
        // -----------------------------------------------------

        Files.write(
                filePath,
                resume.getBytes()
        );

        // -----------------------------------------------------
        // Save file path in database
        // -----------------------------------------------------

        application.setResume(
                "uploads/resumes/" + fileName
        );

        applicationRepository.save(application);
    }
}

