package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.TicketStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTicketStatusRequest {

    @NotNull(message = "El estado es obligatorio")
    private TicketStatus status;

    @Size(max = 100, message = "El nombre del asesor no puede exceder 100 caracteres")
    private String assignedAdvisor;

    @Min(value = 1, message = "El número de módulo debe ser mayor a 0")
    private Integer assignedModuleNumber;
}