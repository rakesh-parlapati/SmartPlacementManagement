
package com.placement.smartplacementmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String email;

    @JsonIgnore
    private String password;

    private Double cgpa;
    private String resume;
    private String phone;
    private String branch;

    // ==========================================
    // DEFAULT CONSTRUCTOR
    // ==========================================

    public Student() {
    }

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Student(
            String name,
            String email,
            String password,
            Double cgpa,
            String resume,
            String phone) {

        this.name = name;
        this.email = email;
        this.password = password;
        this.cgpa = cgpa;
        this.resume = resume;
        this.phone = phone;
    }

    // ==========================================
    // GET ID
    // ==========================================

    public Integer getId() {
        return id;
    }

    // ==========================================
    // SET ID
    // ==========================================

    public void setId(Integer id) {
        this.id = id;
    }

    // ==========================================
    // GET NAME
    // ==========================================

    public String getName() {
        return name;
    }

    // ==========================================
    // SET NAME
    // ==========================================

    public void setName(String name) {
        this.name = name;
    }

    // ==========================================
    // GET EMAIL
    // ==========================================

    public String getEmail() {
        return email;
    }

    // ==========================================
    // SET EMAIL
    // ==========================================

    public void setEmail(String email) {
        this.email = email;
    }

    // ==========================================
    // GET PASSWORD
    // ==========================================

    public String getPassword() {
        return password;
    }

    // ==========================================
    // SET PASSWORD
    // ==========================================

    public void setPassword(String password) {
        this.password = password;
    }

    // ==========================================
    // GET CGPA
    // ==========================================

    public Double getCgpa() {
        return cgpa;
    }

    // ==========================================
    // SET CGPA
    // ==========================================

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    // ==========================================
    // GET RESUME
    // ==========================================

    public String getResume() {
        return resume;
    }

    // ==========================================
    // SET RESUME
    // ==========================================

    public void setResume(String resume) {
        this.resume = resume;
    }

    // ==========================================
    // GET PHONE
    // ==========================================

    public String getPhone() {
        return phone;
    }

    // ==========================================
    // SET PHONE
    // ==========================================

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // ==========================================
    // GET BRANCH
    // ==========================================

    public String getBranch() {
        return branch;
    }

    // ==========================================
    // SET BRANCH
    // ==========================================

    public void setBranch(String branch) {
        this.branch = branch;
    }
}

