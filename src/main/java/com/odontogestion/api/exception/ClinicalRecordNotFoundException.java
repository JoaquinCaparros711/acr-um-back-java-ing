package com.odontogestion.api.exception;

public class ClinicalRecordNotFoundException extends RuntimeException {

    public ClinicalRecordNotFoundException(String message) {
        super(message);
    }
}
