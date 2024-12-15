package com.example.creditmodule.module;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long customerId;
    private BigDecimal loanAmount;
    private int numberOfInstallments;
    private LocalDate createDate;
    private boolean isPaid;
}