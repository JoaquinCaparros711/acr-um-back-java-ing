package com.odontogestion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^.*[a-zA-ZÀ-ÿ].*$", message = "El nombre debe contener al menos una letra")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^.*[a-zA-ZÀ-ÿ].*$", message = "El apellido debe contener al menos una letra")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de correo electrónico no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 9, message = "La contraseña debe tener más de 8 caracteres")
    private String password;
}

