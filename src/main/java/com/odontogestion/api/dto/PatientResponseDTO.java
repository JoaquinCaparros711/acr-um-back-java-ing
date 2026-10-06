package com.odontogestion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String dni;
    private String phone;
    private String email;
    private LocalDate birthDate;
}
