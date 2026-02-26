package com.ms.email.dtos;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class EmailDto {

    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String userEmail;
    private String companyName;
    private String telephone;
    @NotBlank
    private String message;
}
