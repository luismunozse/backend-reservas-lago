package com.luismunozse.reservalago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservation {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private LocalDate visitDate;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String dni;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitorType visitorType = VisitorType.INDIVIDUAL;
    // Datos opcionales si es institución educativa
    private String institutionName;
    private Integer institutionStudents;
    @Column(name = "adults_18_plus")
    private int adults18Plus;
    @Column(name="children_2_to_17")
    private int children2To17;
    @Column(name="babies_less_than_2")
    private int babiesLessThan2;
    @Column(nullable = false)
    private int reducedMobility;
    @Column(nullable = false)
    private int allergies;
    @Column(columnDefinition = "text")
    private String comment;
    @Column(columnDefinition = "text")
    private String originLocation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HowHeard howHeard;
    @Column(nullable = false)
    private boolean acceptedPolicies;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Circuit circuit; // A | B | C | D
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    @PreUpdate void touch() {
        this.updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ReservationVisitor> visitors = new java.util.ArrayList<>();

}
