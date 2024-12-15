package com.example.creditmodule.controller;

import com.example.creditmodule.module.LoanInstallment;
import com.example.creditmodule.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or @loanService.isCustomerOwner(authentication.name, #customerId)")
    public ResponseEntity<String> createLoan(@RequestParam Long customerId,
                                             @RequestParam BigDecimal amount,
                                             @RequestParam BigDecimal interestRate,
                                             @RequestParam int installments) {
        loanService.createLoan(customerId, amount, interestRate, installments);
        return ResponseEntity.ok("Loan created successfully");
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or @loanService.isCustomerOwner(authentication.name, #customerId)")
    public ResponseEntity<List<?>> listLoans(@RequestParam Long customerId) {
        var loans = loanService.listLoans(customerId);
        return ResponseEntity.ok(loans);
    }
    @GetMapping("/{loanId}/installments")
    @PreAuthorize("hasRole('ADMIN') or @loanService.isLoanOwner(authentication.name, #loanId)")
    public ResponseEntity<List<LoanInstallment>> listInstallments(@PathVariable Long loanId) {
        List<LoanInstallment> installments = loanService.listInstallments(loanId);
        return ResponseEntity.ok(installments);
    }
    @PostMapping("/pay")
    @PreAuthorize("hasRole('ADMIN') or @loanService.isLoanOwner(authentication.name, #loanId)")
    public ResponseEntity<String> payLoan(@RequestParam Long loanId,
                                          @RequestParam BigDecimal paymentAmount) {
        String result = loanService.payLoan(loanId, paymentAmount);
        return ResponseEntity.ok(result);
    }
}