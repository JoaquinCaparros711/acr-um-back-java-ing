package com.odontogestion.api.service;

import com.odontogestion.api.dto.PatientRequestDTO;
import com.odontogestion.api.dto.PatientResponseDTO;
import com.odontogestion.api.entity.Patient;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.DuplicateDniException;
import com.odontogestion.api.exception.PatientNotFoundException;
import com.odontogestion.api.repository.PatientRepository;
import com.odontogestion.api.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuthenticationFacade authenticationFacade;

    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getPatients(String search) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        List<Patient> patients;
        if (search == null || search.isBlank()) {
            patients = patientRepository.findByUserId(dentist.getId());
        } else {
            patients = patientRepository.searchPatients(dentist.getId(), search);
        }
        return patients.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Patient patient = findPatientAndValidateOwnership(id, dentist.getId());
        return mapToResponse(patient);
    }

    @Transactional
    public PatientResponseDTO createPatient(PatientRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        if (patientRepository.findByDniAndUserId(request.getDni(), dentist.getId()).isPresent()) {
            throw new DuplicateDniException("DNI is already registered in your patient directory");
        }

        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dni(request.getDni())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .user(dentist)
                .build();

        try {
            Patient saved = patientRepository.saveAndFlush(patient);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateDniException("DNI is already registered in your patient directory");
        }
    }

    @Transactional
    public PatientResponseDTO updatePatient(Long id, PatientRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Patient patient = findPatientAndValidateOwnership(id, dentist.getId());

        if (!patient.getDni().equals(request.getDni())) {
            if (patientRepository.findByDniAndUserId(request.getDni(), dentist.getId()).isPresent()) {
                throw new DuplicateDniException("DNI is already registered in your patient directory");
            }
        }

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDni(request.getDni());
        patient.setPhone(request.getPhone());
        patient.setEmail(request.getEmail());
        patient.setBirthDate(request.getBirthDate());

        try {
            Patient updated = patientRepository.saveAndFlush(patient);
            return mapToResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateDniException("DNI is already registered in your patient directory");
        }
    }

    @Transactional
    public void deletePatient(Long id) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Patient patient = findPatientAndValidateOwnership(id, dentist.getId());
        patientRepository.delete(patient);
    }

    private Patient findPatientAndValidateOwnership(Long id, Long userId) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));

        if (!patient.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied for patient ID: " + id);
        }
        return patient;
    }

    private PatientResponseDTO mapToResponse(Patient patient) {
        return PatientResponseDTO.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dni(patient.getDni())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .birthDate(patient.getBirthDate())
                .build();
    }
}
