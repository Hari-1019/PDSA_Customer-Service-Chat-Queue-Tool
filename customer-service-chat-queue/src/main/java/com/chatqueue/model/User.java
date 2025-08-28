package com.chatqueue.model;

import com.chatqueue.model.enums.AccountType;
import com.chatqueue.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "app_user")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    private AccountType accountType = AccountType.INDIVIDUAL;
}
