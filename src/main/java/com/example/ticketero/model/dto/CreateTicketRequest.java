package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(max = 20, message = "El documento de identidad no puede exceder 20 caracteres")
    private String nationalId;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "El teléfono debe contener solo números y caracteres válidos")
    @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres")
    private String telefono;

    @NotBlank(message = "La sucursal es obligatoria")
    @Size(max = 50, message = "La sucursal no puede exceder 50 caracteres")
    private String branchOffice;

    @NotNull(message = "El tipo de cola es obligatorio")
    private QueueType queueType;
}