package com.placement.smartplacementmanagement.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.placement.smartplacementmanagement.entity.Company;
import com.placement.smartplacementmanagement.repository.CompanyRepository;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(Integer id) {
        return companyRepository.findById(id).orElse(null);
    }

    public void deleteCompany(Integer id) {
        companyRepository.deleteById(id);
    }
}