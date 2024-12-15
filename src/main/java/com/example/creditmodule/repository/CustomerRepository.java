package com.example.creditmodule.repository;

import com.example.creditmodule.module.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
