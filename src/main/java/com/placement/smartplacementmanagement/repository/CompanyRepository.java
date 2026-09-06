package com.placement.smartplacementmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placement.smartplacementmanagement.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

}