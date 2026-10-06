package com.odontogestion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpoPushMessageDTO {

    private String to;
    private String title;
    private String body;
    private Map<String, Object> data;
    private String sound;
    private String priority;
}
