package com.example.creditmodule.service;

import com.example.creditmodule.module.Customer;
import com.example.creditmodule.module.Loan;
import com.example.creditmodule.module.LoanInstallment;
import com.example.creditmodule.module.User;
import com.example.creditmodule.repository.CustomerRepository;
import com.example.creditmodule.repository.LoanInstallmentRepository;
import com.example.creditmodule.repository.LoanRepository;
import com.example.creditmodule.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final UserRepository userRepository;

    public LoanService(CustomerRepository customerRepository,
                       LoanRepository loanRepository,
                       LoanInstallmentRepository installmentRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.userRepository = userRepository;
    }

    public void createLoan(Long customerId, BigDecimal amount, BigDecimal interestRate, int installments) {
        Customer customer = validateCustomer(customerId);
        validateLoanParameters(amount, interestRate, installments);

        BigDecimal totalLoanAmount = calculateTotalLoanAmount(amount, interestRate);
        validateCreditLimit(customer, totalLoanAmount);

        Loan loan = saveLoan(customerId, totalLoanAmount, installments);
        createInstallments(loan, totalLoanAmount, installments);

        updateCustomerCreditLimit(customer, totalLoanAmount);
    }
    public List<Loan> listLoans(Long customerId) {
        return loanRepository.findByCustomerId(customerId);
    }
    public List<LoanInstallment> listInstallments(Long loanId) {
        return installmentRepository.findByLoanId(loanId);
    }
    public String payLoan(Long loanId, BigDecimal paymentAmount) {
        Loan loan = validateLoan(loanId);
        List<LoanInstallment> installments = getPayableInstallments(loanId);

        if (installments.isEmpty()) {
            return "No installments available for payment.";
        }

        PaymentResult paymentResult = processPayments(installments, paymentAmount);
        updateLoanAndCustomerAfterPayment(loan, paymentResult.totalPrincipalPaid);

        return buildPaymentResultMessage(paymentResult, loan);
    }
    private Customer validateCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    private void validateLoanParameters(BigDecimal amount, BigDecimal interestRate, int installments) {
        if (interestRate.compareTo(BigDecimal.valueOf(0.1)) < 0 || interestRate.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0.1 and 0.5");
        }

        if (!List.of(6, 9, 12, 24).contains(installments)) {
            throw new IllegalArgumentException("Number of installments must be one of 6, 9, 12, or 24");
        }
    }

    private BigDecimal calculateTotalLoanAmount(BigDecimal amount, BigDecimal interestRate) {
        return amount.multiply(BigDecimal.ONE.add(interestRate));
    }

    private void validateCreditLimit(Customer customer, BigDecimal totalLoanAmount) {
        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());
        if (availableCredit.compareTo(totalLoanAmount) < 0) {
            throw new RuntimeException("Insufficient credit limit");
        }
    }

    private Loan saveLoan(Long customerId, BigDecimal totalLoanAmount, int installments) {
        Loan loan = new Loan();
        loan.setCustomerId(customerId);
        loan.setLoanAmount(totalLoanAmount);
        loan.setNumberOfInstallments(installments);
        loan.setCreateDate(LocalDate.now());
        loan.setPaid(false);
        return loanRepository.save(loan);
    }

    private void createInstallments(Loan loan, BigDecimal totalLoanAmount, int installments) {
        BigDecimal installmentAmount = totalLoanAmount.divide(BigDecimal.valueOf(installments), BigDecimal.ROUND_HALF_UP);
        LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < installments; i++) {
            LoanInstallment installment = new LoanInstallment();
            installment.setLoanId(loan.getId());
            installment.setAmount(installmentAmount);
            installment.setPaidAmount(BigDecimal.ZERO);
            installment.setDueDate(nextMonth.plusMonths(i));
            installment.setPaid(false);
            installmentRepository.save(installment);
        }
    }

    private void updateCustomerCreditLimit(Customer customer, BigDecimal totalLoanAmount) {
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalLoanAmount));
        customerRepository.save(customer);
    }
    private Loan validateLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
    }

    private List<LoanInstallment> getPayableInstallments(Long loanId) {
        LocalDate maxDueDate = LocalDate.now().plusMonths(3);
        return installmentRepository.findByLoanId(loanId).stream()
                .filter(installment -> !installment.isPaid() && installment.getDueDate().isBefore(maxDueDate))
                .toList();
    }

    private PaymentResult processPayments(List<LoanInstallment> installments, BigDecimal paymentAmount) {
        BigDecimal remainingAmount = paymentAmount;
        int paidInstallmentsCount = 0;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalPenalty = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

        for (LoanInstallment installment : installments) {
            BigDecimal adjustment = calculateAdjustment(installment);
            BigDecimal adjustedInstallmentAmount = installment.getAmount().add(adjustment);

            if (remainingAmount.compareTo(adjustedInstallmentAmount) >= 0) {
                remainingAmount = remainingAmount.subtract(adjustedInstallmentAmount);
                totalPaid = totalPaid.add(adjustedInstallmentAmount);

                totalPrincipalPaid = totalPrincipalPaid.add(installment.getAmount());
                installment.setPaidAmount(adjustedInstallmentAmount);
                installment.setPaymentDate(LocalDate.now());
                installment.setPaid(true);
                installmentRepository.save(installment);

                if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
                    totalDiscount = totalDiscount.add(adjustment.abs());
                } else {
                    totalPenalty = totalPenalty.add(adjustment);
                }

                paidInstallmentsCount++;
            } else {
                break;
            }
        }

        return new PaymentResult(paidInstallmentsCount, totalPaid, totalDiscount, totalPenalty, totalPrincipalPaid);
    }

    private BigDecimal calculateAdjustment(LoanInstallment installment) {
        LocalDate paymentDate = LocalDate.now();
        LocalDate dueDate = installment.getDueDate();
        BigDecimal adjustment = BigDecimal.ZERO;

        if (paymentDate.isBefore(dueDate)) {
            long daysBeforeDue = ChronoUnit.DAYS.between(paymentDate, dueDate);
            adjustment = installment.getAmount().multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(daysBeforeDue)).negate();
        } else if (paymentDate.isAfter(dueDate)) {
            long daysAfterDue = ChronoUnit.DAYS.between(dueDate, paymentDate);
            adjustment = installment.getAmount().multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(daysAfterDue));
        }

        return adjustment;
    }

    private void updateLoanAndCustomerAfterPayment(Loan loan, BigDecimal totalPrincipalPaid) {
        boolean allPaid = installmentRepository.findByLoanId(loan.getId()).stream().allMatch(LoanInstallment::isPaid);
        if (allPaid) {
            loan.setPaid(true);
            loanRepository.save(loan);
        }

        Customer customer = customerRepository.getReferenceById(loan.getCustomerId());
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(totalPrincipalPaid));
        customerRepository.save(customer);
    }

    private String buildPaymentResultMessage(PaymentResult result, Loan loan) {
        return String.format(
                "Paid %d installments, total paid: %s. Discount: %s, Penalty: %s. Loan fully paid: %b",
                result.paidInstallmentsCount,
                result.totalPaid,
                result.totalDiscount.abs(),
                result.totalPenalty,
                loan.isPaid()
        );
    }

    private static class PaymentResult {
        int paidInstallmentsCount;
        BigDecimal totalPaid;
        BigDecimal totalDiscount;
        BigDecimal totalPenalty;
        BigDecimal totalPrincipalPaid;

        public PaymentResult(int paidInstallmentsCount, BigDecimal totalPaid, BigDecimal totalDiscount, BigDecimal totalPenalty, BigDecimal totalPrincipalPaid) {
            this.paidInstallmentsCount = paidInstallmentsCount;
            this.totalPaid = totalPaid;
            this.totalDiscount = totalDiscount;
            this.totalPenalty = totalPenalty;
            this.totalPrincipalPaid = totalPrincipalPaid;
        }
    }
    public boolean isCustomerOwner(String username, Long customerId) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent() && user.get().getCustomer() != null && user.get().getCustomer().getId().equals(customerId);
    }

    public boolean isLoanOwner(String username, Long loanId) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getCustomer() != null) {
            Long customerId = user.get().getCustomer().getId();
            return loanRepository.findById(loanId)
                    .map(loan -> loan.getCustomerId().equals(customerId))
                    .orElse(false);
        }
        return false;
    }
}