package com.fooddrinks.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider = Provider.LOCAL;

    // ID provided by OAuth2 provider (Google sub, Facebook id, etc.)
    // Not a FK — this is an external identifier from the 3rd party
    private String providerId;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean isActive = true;
}
