package com.minibank.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "Il nome dell'intestatario è obbligatorio")
        @Size(max = 100, message = "Il nome dell'intestatario può avere al massimo 100 caratteri")
        String ownerName
) {}
