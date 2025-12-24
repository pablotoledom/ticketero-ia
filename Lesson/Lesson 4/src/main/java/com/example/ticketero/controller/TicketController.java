package com.example.ticketero.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody Map<String, Object> request) {
        String codigoReferencia = "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String queueType = (String) request.get("queueType");
        String numero = "PREFERENCIAL".equals(queueType) ? "P001" : "G001";
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1L);
        response.put("codigoReferencia", codigoReferencia);
        response.put("numero", numero);
        response.put("nationalId", request.get("nationalId"));
        response.put("telefono", request.get("telefono"));
        response.put("branchOffice", request.get("branchOffice"));
        response.put("queueType", queueType);
        response.put("status", "WAITING");
        response.put("positionInQueue", 1);
        response.put("estimatedWaitMinutes", 15);
        response.put("createdAt", LocalDateTime.now().toString());
        response.put("updatedAt", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{codigoReferencia}/status")
    public ResponseEntity<Map<String, Object>> getTicketStatus(@PathVariable String codigoReferencia) {
        Map<String, Object> response = new HashMap<>();
        response.put("codigoReferencia", codigoReferencia);
        response.put("numero", "G001");
        response.put("queueType", "GENERAL");
        response.put("status", "WAITING");
        response.put("positionInQueue", 3);
        response.put("estimatedWaitMinutes", 45);
        response.put("createdAt", LocalDateTime.now().minusMinutes(10).toString());
        response.put("updatedAt", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{codigoReferencia}/status")
    public ResponseEntity<Map<String, Object>> updateTicketStatus(
            @PathVariable String codigoReferencia,
            @RequestBody Map<String, Object> request) {
        
        String status = (String) request.get("status");
        boolean inProgress = "IN_PROGRESS".equals(status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1L);
        response.put("codigoReferencia", codigoReferencia);
        response.put("numero", "G001");
        response.put("nationalId", "12345678");
        response.put("telefono", "555-0001");
        response.put("branchOffice", "Sucursal Centro");
        response.put("queueType", "GENERAL");
        response.put("status", status);
        response.put("positionInQueue", inProgress ? 0 : 1);
        response.put("estimatedWaitMinutes", inProgress ? 0 : 15);
        response.put("assignedAdvisor", request.get("assignedAdvisor"));
        response.put("assignedModuleNumber", request.get("assignedModuleNumber"));
        response.put("createdAt", LocalDateTime.now().minusMinutes(5).toString());
        response.put("updatedAt", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<Map<String, Object>>> getWaitingTickets(
            @RequestParam String branchOffice,
            @RequestParam String queueType) {
        
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("id", 1L);
        ticket.put("codigoReferencia", "REF12345678");
        ticket.put("numero", "PREFERENCIAL".equals(queueType) ? "P001" : "G001");
        ticket.put("nationalId", "12345678");
        ticket.put("telefono", "555-0001");
        ticket.put("branchOffice", branchOffice);
        ticket.put("queueType", queueType);
        ticket.put("status", "WAITING");
        ticket.put("positionInQueue", 1);
        ticket.put("estimatedWaitMinutes", 15);
        ticket.put("createdAt", LocalDateTime.now().toString());
        ticket.put("updatedAt", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(List.of(ticket));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Ticketero API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTickets", 25);
        stats.put("waitingTickets", 8);
        stats.put("inProgressTickets", 3);
        stats.put("completedTickets", 14);
        stats.put("averageWaitTime", 22);
        stats.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{codigoReferencia}")
    public ResponseEntity<Map<String, String>> cancelTicket(@PathVariable String codigoReferencia) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Ticket " + codigoReferencia + " cancelado exitosamente");
        response.put("status", "CANCELLED");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}