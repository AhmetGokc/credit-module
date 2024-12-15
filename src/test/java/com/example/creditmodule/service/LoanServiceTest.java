package com.example.creditmodule.service;

import com.example.creditmodule.module.Customer;
import com.example.creditmodule.module.Loan;
import com.example.creditmodule.module.LoanInstallment;
import com.example.creditmodule.repository.CustomerRepository;
import com.example.creditmodule.repository.LoanInstallmentRepository;
import com.example.creditmodule.repository.LoanRepository;

import com.example.creditmodule.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createLoan_shouldCreateLoanSuccessfully() {
        Long customerId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        BigDecimal interestRate = BigDecimal.valueOf(0.2);
        int installments = 12;

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCreditLimit(BigDecimal.valueOf(50000));
        customer.setUsedCreditLimit(BigDecimal.ZERO);

        Loan loan = new Loan();
        loan.setId(1L);
        loan.setCustomerId(customerId);
        loan.setLoanAmount(BigDecimal.valueOf(12000)); // Total loan amount
        loan.setNumberOfInstallments(installments);
        loan.setCreateDate(LocalDate.now());
        loan.setPaid(false);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.createLoan(customerId, amount, interestRate, installments);

        verify(loanRepository, times(1)).save(any(Loan.class));
        verify(installmentRepository, times(installments)).save(any(LoanInstallment.class));
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void createLoan_shouldThrowExceptionForInsufficientCreditLimit() {
        Long customerId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50000);
        BigDecimal interestRate = BigDecimal.valueOf(0.3);
        int installments = 12;

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCreditLimit(BigDecimal.valueOf(10000)); // Insufficient limit
        customer.setUsedCreditLimit(BigDecimal.ZERO);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThrows(RuntimeException.class, () ->
                loanService.createLoan(customerId, amount, interestRate, installments)
        );
    }

    @Test
    void createLoan_shouldThrowExceptionForInvalidInstallments() {
        Long customerId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        BigDecimal interestRate = BigDecimal.valueOf(0.2);
        int invalidInstallments = 10; // Invalid installment count

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new Customer()));

        assertThrows(IllegalArgumentException.class, () ->
                loanService.createLoan(customerId, amount, interestRate, invalidInstallments)
        );
    }

    @Test
    void payLoan_shouldProcessPaymentsCorrectly() {
        // Arrange
        Long loanId = 1L;
        BigDecimal paymentAmount = BigDecimal.valueOf(25000);

        // Loan Mock
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setCustomerId(1L);
        loan.setPaid(false);

        // Customer Mock
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCreditLimit(BigDecimal.valueOf(50000));
        customer.setUsedCreditLimit(BigDecimal.valueOf(20000));

        // Loan Installments Mock
        LoanInstallment installment1 = new LoanInstallment();
        installment1.setId(1L);
        installment1.setAmount(BigDecimal.valueOf(10000)); // İlk taksit
        installment1.setPaidAmount(BigDecimal.ZERO);
        installment1.setDueDate(LocalDate.now().minusDays(10)); // Geç ödeme
        installment1.setPaid(false);

        LoanInstallment installment2 = new LoanInstallment();
        installment2.setId(2L);
        installment2.setAmount(BigDecimal.valueOf(10000)); // İkinci taksit
        installment2.setPaidAmount(BigDecimal.ZERO);
        installment2.setDueDate(LocalDate.now().plusDays(5)); // Erken ödeme
        installment2.setPaid(false);

        List<LoanInstallment> installments = List.of(installment1, installment2);

        // Mocking
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(installmentRepository.findByLoanId(loanId)).thenReturn(installments);
        when(customerRepository.getReferenceById(loan.getCustomerId())).thenReturn(customer);
        when(installmentRepository.save(any(LoanInstallment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = loanService.payLoan(loanId, paymentAmount);

        // Assert
        assertTrue(result.contains("Paid 2 installments"), "The result should indicate 2 installments were paid.");
        assertTrue(installment1.isPaid(), "Installment 1 should be marked as paid.");
        assertTrue(installment2.isPaid(), "Installment 2 should be marked as paid.");
        assertEquals(BigDecimal.ZERO, customer.getUsedCreditLimit(), "Customer's used credit limit should be reduced.");
        verify(installmentRepository, times(2)).save(any(LoanInstallment.class));
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void payLoan_shouldReturnNoInstallmentsAvailableMessage() {
        Long loanId = 1L;
        BigDecimal paymentAmount = BigDecimal.valueOf(20000);

        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setCustomerId(1L);
        loan.setPaid(false);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(installmentRepository.findByLoanId(loanId)).thenReturn(List.of());

        String result = loanService.payLoan(loanId, paymentAmount);

        assertEquals("No installments available for payment.", result);
    }

    @Test
    void payLoan_shouldThrowExceptionForNonExistingLoan() {
        Long loanId = 1L;
        BigDecimal paymentAmount = BigDecimal.valueOf(10000);

        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                loanService.payLoan(loanId, paymentAmount)
        );
    }

    @Test
    void listLoans_shouldReturnLoansForCustomer() {
        Long customerId = 1L;
        Loan loan1 = new Loan();
        Loan loan2 = new Loan();

        when(loanRepository.findByCustomerId(customerId)).thenReturn(List.of(loan1, loan2));

        List<Loan> loans = loanService.listLoans(customerId);

        assertEquals(2, loans.size());
        verify(loanRepository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void listLoans_shouldReturnEmptyListForNonExistingCustomer() {
        Long customerId = 1L;

        when(loanRepository.findByCustomerId(customerId)).thenReturn(List.of());

        List<Loan> loans = loanService.listLoans(customerId);

        assertTrue(loans.isEmpty());
    }

    @Test
    void listInstallments_shouldReturnInstallmentsForLoan() {
        Long loanId = 1L;
        LoanInstallment installment1 = new LoanInstallment();
        LoanInstallment installment2 = new LoanInstallment();

        when(installmentRepository.findByLoanId(loanId)).thenReturn(List.of(installment1, installment2));

        List<LoanInstallment> installments = loanService.listInstallments(loanId);

        assertEquals(2, installments.size());
        verify(installmentRepository, times(1)).findByLoanId(loanId);
    }

    @Test
    void listInstallments_shouldReturnEmptyListForNonExistingLoan() {
        Long loanId = 1L;

        when(installmentRepository.findByLoanId(loanId)).thenReturn(List.of());

        List<LoanInstallment> installments = loanService.listInstallments(loanId);

        assertTrue(installments.isEmpty());
    }
}