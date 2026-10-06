package com.odontogestion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[^0-9]+$", message = "El nombre no puede contener números")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[^0-9]+$", message = "El apellido no puede contener números")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]+$", message = "El DNI debe ser estrictamente numérico")
    private String dni;

    @Pattern(regexp = "^$|^[0-9+\\-\\s()]+$", message = "El teléfono no puede contener letras")
    private String phone;

    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;
}
