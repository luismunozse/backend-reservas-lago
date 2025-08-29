package com.luismunozse.reservalago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "availability_rules")
@Getter
@Setter
public class AvailabilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate day;

    @Column(nullable = false)
    private int capacity;
}
