package com.aplazo.backend.repository;

import com.aplazo.backend.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {
    
    List<Installment> findByLoanId(UUID loanId);
} 