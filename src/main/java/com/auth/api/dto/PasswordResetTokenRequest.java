package com.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetTokenRequest {
    @NotBlank
    private String token;
    
    @NotBlank
    private String newPassword;
}