package com.example.creditmodule.controller;

import com.example.creditmodule.service.LoanService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerTest {

    private final LoanService loanService = Mockito.mock(LoanService.class);
    private final LoanController loanController = new LoanController(loanService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(loanController).build();

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLoan_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/loans/create")
                        .param("customerId", "1")
                        .param("amount", "10000")
                        .param("interestRate", "0.2")
                        .param("installments", "12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan created successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listLoans_shouldReturnLoans() throws Exception {
        Mockito.when(loanService.listLoans(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/loans/list")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listInstallments_shouldReturnInstallments() throws Exception {
        mockMvc.perform(get("/api/loans/1/installments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void payLoan_shouldReturnSuccess() throws Exception {
        Mockito.when(loanService.payLoan(1L, BigDecimal.valueOf(1000)))
                .thenReturn("Paid 1 installments");

        mockMvc.perform(post("/api/loans/pay")
                        .param("loanId", "1")
                        .param("paymentAmount", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Paid 1 installments"));
    }
}