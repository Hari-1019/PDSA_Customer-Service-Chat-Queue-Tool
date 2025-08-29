package com.chatqueue.model;

import com.chatqueue.enums.*;
import com.chatqueue.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.util.UUID;

@Entity @Table(name = "users")
@Getter @Setter
public class User {
    @Id
    @Column(name="user_id")
    private UUID userId = UUID.randomUUID();

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name="account_type")
    private AccountType accountType = AccountType.individual;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.available;

    @Column(name="company_name")
    private String companyName;

    @Column(name="priority_level")
    private Integer priorityLevel = 1;



}
