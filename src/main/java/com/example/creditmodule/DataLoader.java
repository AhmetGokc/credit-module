package com.example.creditmodule;

import com.example.creditmodule.module.Customer;
import com.example.creditmodule.module.User;
import com.example.creditmodule.repository.CustomerRepository;
import com.example.creditmodule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("adminpass"));
                adminUser.setRole("ADMIN");
                userRepository.save(adminUser);
                System.out.println("Admin user created.");
            } else {
                System.out.println("Admin user already exists.");
            }

            if (userRepository.findByUsername("customer").isEmpty()) {
                Customer customer = new Customer();
                customer.setName("John");
                customer.setSurname("Doe");
                customer.setCreditLimit(BigDecimal.valueOf(50000));
                customer.setUsedCreditLimit(BigDecimal.valueOf(0));
                customer = customerRepository.save(customer);

                User customerUser = new User();
                customerUser.setUsername("customer");
                customerUser.setPassword(passwordEncoder.encode("customerpass"));
                customerUser.setRole("CUSTOMER");
                customerUser.setCustomer(customer);
                userRepository.save(customerUser);
                System.out.println("Customer user and associated customer created.");
            } else {
                System.out.println("Customer user already exists.");
            }
        };
    }
}