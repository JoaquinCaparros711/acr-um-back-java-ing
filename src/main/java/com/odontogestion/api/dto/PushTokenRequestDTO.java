package com.odontogestion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenRequestDTO {

    @NotBlank(message = "Push token must not be blank")
    @Pattern(
            regexp = "^(ExponentPushToken\\[.+\\]|ExpoPushToken\\[.+\\]|[a-zA-Z0-9_-]+)$",
            message = "Invalid push token format"
    )
    private String pushToken;
}
