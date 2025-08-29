package com.chatqueue.dto;


import lombok.*;


@Getter @Setter
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String role;        // customer/agent/admin
    private String accountType; // enterprise/individual
    private String companyName; // optional


}
