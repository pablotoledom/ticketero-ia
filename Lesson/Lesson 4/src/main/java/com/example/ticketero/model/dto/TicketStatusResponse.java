package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketStatusResponse {

    private String codigoReferencia;
    private String numero;
    private QueueType queueType;
    private TicketStatus status;
    private Integer positionInQueue;
    private Integer estimatedWaitMinutes;
    private String assignedAdvisor;
    private Integer assignedModuleNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}