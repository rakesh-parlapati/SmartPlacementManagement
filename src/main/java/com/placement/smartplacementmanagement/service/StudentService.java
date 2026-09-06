
package com.placement.smartplacementmanagement.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.placement.smartplacementmanagement.entity.Student;
import com.placement.smartplacementmanagement.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student saveStudent(Student student) {

        /*
         * Check whether this student already exists.
         */
        if (student.getId() != null) {

            Student existingStudent =
                    studentRepository.findById(student.getId())
                            .orElse(null);

            if (existingStudent != null) {

                String newPassword =
                        student.getPassword();

                /*
                 * If password is empty/null,
                 * keep the existing password.
                 */
                if (newPassword == null
                        || newPassword.trim().isEmpty()) {

                    student.setPassword(
                            existingStudent.getPassword()
                    );

                } else if (
                        !newPassword.startsWith("$2a$")
                        && !newPassword.startsWith("$2b$")
                        && !newPassword.startsWith("$2y$")) {

                    /*
                     * New password supplied.
                     * Hash it before saving.
                     */
                    student.setPassword(
                            passwordEncoder.encode(newPassword)
                    );
                }
            }
        }

        /*
         * New student
         */
        else {

            String password =
                    student.getPassword();

            if (password != null
                    && !password.trim().isEmpty()
                    && !password.startsWith("$2a$")
                    && !password.startsWith("$2b$")
                    && !password.startsWith("$2y$")) {

                student.setPassword(
                        passwordEncoder.encode(password)
                );
            }
        }

        return studentRepository.save(student);
    }


    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }


    public Student getStudentById(Integer id) {
        return studentRepository.findById(id).orElse(null);
    }


    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email);
    }


    public Student login(String email, String password) {

        Student student =
                studentRepository.findByEmail(email);

        if (student == null) {
            return null;
        }

        String storedPassword =
                student.getPassword();

        if (storedPassword == null) {
            return null;
        }

        /*
         * BCrypt password
         */
        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {

            if (passwordEncoder.matches(
                    password,
                    storedPassword)) {

                return student;
            }

            return null;
        }

        /*
         * Old plaintext password.
         * Automatically migrate it to BCrypt.
         */
        if (storedPassword.equals(password)) {

            student.setPassword(
                    passwordEncoder.encode(password)
            );

            studentRepository.save(student);

            return student;
        }

        return null;
    }


    public void deleteStudent(Integer id) {
        studentRepository.deleteById(id);
    }
}

