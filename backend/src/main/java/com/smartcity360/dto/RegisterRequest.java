package com.smartcity360.dto;

import com.smartcity360.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    /** CITIZEN, OFFICER or ADMIN. Defaults to CITIZEN when omitted. */
    private Role role;

    /** Only used when role = OFFICER. */
    private String department;
    private String ward;
}
