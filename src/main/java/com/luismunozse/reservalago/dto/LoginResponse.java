package com.luismunozse.reservalago.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;

    public LoginResponse(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }
}



