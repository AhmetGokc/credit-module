package com.example.creditmodule.module;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "APP_USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String role; // ADMIN veya CUSTOMER

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer; // CUSTOMER rolü için ilişki

}