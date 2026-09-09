
package com.placement.smartplacementmanagement.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.service.StudentService;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    // ==========================================
    // UPLOAD FOLDER
    // ==========================================

    private final Path uploadPath = Paths.get(
            System.getProperty("user.dir"),
            "uploads"
    ).toAbsolutePath().normalize();

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public StudentController(StudentService studentService) {

        this.studentService = studentService;

        try {

            if (!Files.exists(uploadPath)) {

                Files.createDirectories(uploadPath);

                System.out.println(
                        "Uploads folder created at: "
                                + uploadPath
                );
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    // ==========================================
    // CHECK ADMIN
    // ==========================================

    private boolean isAdmin() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication != null
                && authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                "ROLE_ADMIN".equals(
                                        authority.getAuthority()
                                )
                        );
    }

    // ==========================================
    // GET LOGGED-IN EMAIL
    // ==========================================

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

    // ==========================================
    // CHECK STUDENT OWNERSHIP
    // ==========================================

    private boolean isOwner(Integer studentId) {

        String loggedInEmail =
                getLoggedInEmail();

        if (loggedInEmail == null) {
            return false;
        }

        Student student =
                studentService.getStudentById(studentId);

        if (student == null ||
                student.getEmail() == null) {

            return false;
        }

        return student.getEmail()
                .equalsIgnoreCase(loggedInEmail);
    }

    // ==========================================
    // CHECK ADMIN OR OWNER
    // ==========================================

    private boolean isAdminOrOwner(Integer studentId) {

        return isAdmin() || isOwner(studentId);
    }

    // ==========================================
    // CREATE SAFE FILE NAME
    // ==========================================

    private String createSafeFileName(
            String originalFileName) {

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            return null;
        }

        String cleanFileName =
                Paths.get(originalFileName)
                        .getFileName()
                        .toString();

        if (!cleanFileName
                .toLowerCase()
                .endsWith(".pdf")) {

            return null;
        }

        return UUID.randomUUID()
                + ".pdf";
    }

    // ==========================================
    // GET SAFE FILE PATH
    // ==========================================

    private Path getSafeFilePath(
            String fileName) {

        if (fileName == null ||
                fileName.isBlank()) {

            return null;
        }

        Path filePath =
                uploadPath
                        .resolve(fileName)
                        .normalize();

        if (!filePath.startsWith(uploadPath)) {
            return null;
        }

        return filePath;
    }

    // ==========================================
    // ADD STUDENT + RESUME
    // ==========================================

    @PostMapping
    public ResponseEntity<?> addStudent(

            @RequestParam("name") String name,

            @RequestParam("email") String email,

            @RequestParam("phone") String phone,

            @RequestParam("password") String password,

            @RequestParam("cgpa") Double cgpa,

            @RequestParam("branch") String branch,

            @RequestParam("resume") MultipartFile resume)

            throws IOException {

        // ==========================================
        // BASIC VALIDATION
        // ==========================================

        if (name == null ||
                name.trim().isEmpty() ||

                email == null ||
                email.trim().isEmpty() ||

                password == null ||
                password.trim().isEmpty() ||

                cgpa == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Required student details are missing"
                    );
        }

        // ==========================================
        // CHECK PHONE
        // ==========================================

        if (phone == null ||
                !phone.matches("\\d{10}")) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Phone number must contain exactly 10 digits"
                    );
        }

        // ==========================================
        // CHECK BRANCH
        // ==========================================

        if (branch == null ||
                branch.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Branch is required"
                    );
        }

        // ==========================================
        // CHECK CGPA
        // ==========================================

        if (cgpa < 0 || cgpa > 10) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "CGPA must be between 0 and 10"
                    );
        }

        // ==========================================
        // CHECK EXISTING ACCOUNT
        // ==========================================

        Student existingStudent =
                studentService.getStudentByEmail(
                        email.trim()
                );

        if (existingStudent != null) {

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(
                            URI.create(
                                    "/register.html?existing=true"
                            )
                    )
                    .build();
        }

        // ==========================================
        // CHECK RESUME
        // ==========================================

        if (resume == null ||
                resume.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Resume is required"
                    );
        }

        // ==========================================
        // CHECK PDF
        // ==========================================

        if (!"application/pdf".equalsIgnoreCase(
                resume.getContentType())) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Only PDF resumes are allowed"
                    );
        }

        String originalFileName =
                resume.getOriginalFilename();

        String fileName =
                createSafeFileName(
                        originalFileName
                );

        if (fileName == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid resume file"
                    );
        }

        // ==========================================
        // CREATE UPLOAD FOLDER
        // ==========================================

        if (!Files.exists(uploadPath)) {

            Files.createDirectories(
                    uploadPath
            );
        }

        // ==========================================
        // CREATE SAFE FILE PATH
        // ==========================================

        Path filePath =
                getSafeFilePath(fileName);

        if (filePath == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid file path"
                    );
        }

        // ==========================================
        // SAVE RESUME
        // ==========================================

        Files.write(
                filePath,
                resume.getBytes()
        );

        System.out.println(
                "Resume saved at: "
                        + filePath
        );

        // ==========================================
        // CREATE STUDENT
        // ==========================================

        Student student = new Student(
                name.trim(),
                email.trim(),
                password,
                cgpa,
                fileName,
                phone
        );

        // ==========================================
        // SET BRANCH
        // ==========================================

        student.setBranch(
                branch.trim()
        );

        // ==========================================
        // SAVE STUDENT
        // ==========================================

        studentService.saveStudent(student);

        // ==========================================
        // REGISTRATION SUCCESS REDIRECT
        // ==========================================

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                        URI.create(
                                "/register.html?success=true"
                        )
                )
                .build();
    }

    // ==========================================
    // GET ALL STUDENTS
    // ADMIN ONLY
    // ==========================================

    @GetMapping
    public ResponseEntity<?> getAllStudents() {

        if (!isAdmin()) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Only administrators can view all students"
                    );
        }

        List<Student> students =
                studentService.getAllStudents();

        return ResponseEntity.ok(students);
    }

    // ==========================================
    // GET STUDENT BY ID
    // ADMIN OR OWN STUDENT
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(
            @PathVariable Integer id) {

        if (!isAdminOrOwner(id)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this student"
                    );
        }

        Student student =
                studentService.getStudentById(id);

        if (student == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(student);
    }

    // ==========================================
    // UPDATE STUDENT
    // ADMIN OR OWN STUDENT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(

            @PathVariable Integer id,

            @RequestParam("name") String name,

            @RequestParam(
                    value = "email",
                    required = false
            )
            String email,

            @RequestParam("phone") String phone,

            @RequestParam(
                    value = "password",
                    required = false
            )
            String password,

            @RequestParam("cgpa") Double cgpa,

            @RequestParam(
                    value = "branch",
                    required = false
            )
            String branch,

            @RequestParam(
                    value = "resume",
                    required = false
            )
            MultipartFile resume)

            throws IOException {

        // ==========================================
        // AUTHORIZATION
        // ==========================================

        if (!isAdminOrOwner(id)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to update this student"
                    );
        }

        // ==========================================
        // FIND STUDENT
        // ==========================================

        Student student =
                studentService.getStudentById(id);

        if (student == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // ==========================================
        // BASIC VALIDATION
        // ==========================================

        if (name == null ||
                name.trim().isEmpty() ||

                cgpa == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Required student details are missing"
                    );
        }

        // ==========================================
        // CHECK PHONE
        // ==========================================

        if (phone == null ||
                !phone.matches("\\d{10}")) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Phone number must contain exactly 10 digits"
                    );
        }

        // ==========================================
        // CHECK CGPA
        // ==========================================

        if (cgpa < 0 || cgpa > 10) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "CGPA must be between 0 and 10"
                    );
        }

        // ==========================================
        // STUDENT EMAIL SECURITY
        // ==========================================

        if (!isAdmin()) {

            String loggedInEmail =
                    getLoggedInEmail();

            if (loggedInEmail == null ||
                    student.getEmail() == null ||
                    !student.getEmail()
                            .equalsIgnoreCase(
                                    loggedInEmail
                            )) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                "You are not authorized to update this student"
                        );
            }

            email = student.getEmail();

        } else {

            if (email == null ||
                    email.trim().isEmpty()) {

                email = student.getEmail();

            } else {

                email = email.trim();
            }
        }

        // ==========================================
        // UPDATE BASIC DETAILS
        // ==========================================

        student.setName(
                name.trim()
        );

        student.setEmail(
                email
        );

        student.setPhone(
                phone
        );

        student.setCgpa(
                cgpa
        );

        // ==========================================
        // UPDATE BRANCH
        // ==========================================

        if (branch != null &&
                !branch.trim().isEmpty()) {

            student.setBranch(
                    branch.trim()
            );
        }

        // ==========================================
        // PASSWORD
        // ==========================================

        if (isAdmin() &&
                password != null &&
                !password.trim().isEmpty()) {

            student.setPassword(
                    password
            );
        }

        // ==========================================
        // UPDATE RESUME
        // ==========================================

        if (resume != null &&
                !resume.isEmpty()) {

            // ==========================================
            // CHECK PDF
            // ==========================================

            if (!"application/pdf".equalsIgnoreCase(
                    resume.getContentType())) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Only PDF resumes are allowed"
                        );
            }

            // ==========================================
            // ORIGINAL FILE NAME
            // ==========================================

            String originalFileName =
                    resume.getOriginalFilename();

            String newFileName =
                    createSafeFileName(
                            originalFileName
                    );

            if (newFileName == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid resume file"
                        );
            }

            // ==========================================
            // CREATE UPLOAD FOLDER
            // ==========================================

            if (!Files.exists(uploadPath)) {

                Files.createDirectories(
                        uploadPath
                );
            }

            // ==========================================
            // DELETE OLD RESUME SAFELY
            // ==========================================

            String oldFileName =
                    student.getResume();

            if (oldFileName != null &&
                    !oldFileName.isBlank()) {

                Path oldFilePath =
                        getSafeFilePath(
                                oldFileName
                        );

                if (oldFilePath != null) {

                    try {

                        Files.deleteIfExists(
                                oldFilePath
                        );

                    } catch (IOException e) {

                        System.out.println(
                                "Could not delete old resume: "
                                        + e.getMessage()
                        );
                    }
                }
            }

            // ==========================================
            // CREATE NEW SAFE PATH
            // ==========================================

            Path newFilePath =
                    getSafeFilePath(
                            newFileName
                    );

            if (newFilePath == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid file path"
                        );
            }

            // ==========================================
            // SAVE NEW RESUME
            // ==========================================

            Files.write(
                    newFilePath,
                    resume.getBytes()
            );

            System.out.println(
                    "New resume saved at: "
                            + newFilePath
            );

            // ==========================================
            // UPDATE RESUME NAME
            // ==========================================

            student.setResume(
                    newFileName
            );
        }

        // ==========================================
        // SAVE UPDATED STUDENT
        // ==========================================

        Student updatedStudent =
                studentService.saveStudent(
                        student
                );

        return ResponseEntity.ok(
                updatedStudent
        );
    }

    // ==========================================
    // VIEW RESUME PDF
    // ADMIN OR OWN STUDENT
    // ==========================================

    @GetMapping("/{id}/resume")
    public ResponseEntity<?> getResume(
            @PathVariable Integer id)

            throws IOException {

        // ==========================================
        // AUTHORIZATION
        // ==========================================

        if (!isAdminOrOwner(id)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to access this resume"
                    );
        }

        // ==========================================
        // FIND STUDENT
        // ==========================================

        Student student =
                studentService.getStudentById(id);

        if (student == null ||
                student.getResume() == null ||
                student.getResume().isBlank()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // ==========================================
        // SAFE FILE PATH
        // ==========================================

        Path filePath =
                getSafeFilePath(
                        student.getResume()
                );

        if (filePath == null) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "Invalid resume path"
                    );
        }

        // ==========================================
        // CHECK FILE
        // ==========================================

        if (!Files.exists(filePath) ||
                !Files.isRegularFile(filePath)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // ==========================================
        // READ RESUME
        // ==========================================

        byte[] data =
                Files.readAllBytes(
                        filePath
                );

        ByteArrayResource resource =
                new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(
                        data.length
                )
                .body(resource);
    }

    // ==========================================
    // DELETE STUDENT
    // ADMIN OR OWN STUDENT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(
            @PathVariable Integer id) {

        // ==========================================
        // AUTHORIZATION
        // ==========================================

        if (!isAdminOrOwner(id)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You are not authorized to delete this student"
                    );
        }

        // ==========================================
        // FIND STUDENT
        // ==========================================

        Student student =
                studentService.getStudentById(id);

        if (student == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // ==========================================
        // DELETE STUDENT RESUME SAFELY
        // ==========================================

        if (student.getResume() != null &&
                !student.getResume().isBlank()) {

            Path resumePath =
                    getSafeFilePath(
                            student.getResume()
                    );

            if (resumePath != null) {

                try {

                    Files.deleteIfExists(
                            resumePath
                    );

                } catch (IOException e) {

                    System.out.println(
                            "Could not delete resume: "
                                    + e.getMessage()
                    );
                }
            }
        }

        // ==========================================
        // DELETE STUDENT
        // ==========================================

        studentService.deleteStudent(id);

        return ResponseEntity.ok(
                "Student deleted successfully"
        );
    }
}
