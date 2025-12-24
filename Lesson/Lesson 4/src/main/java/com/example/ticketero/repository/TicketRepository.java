package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigoReferencia(String codigoReferencia);

    List<Ticket> findByStatusAndQueueTypeOrderByCreatedAt(TicketStatus status, QueueType queueType);

    List<Ticket> findByBranchOfficeAndStatusOrderByCreatedAt(String branchOffice, TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status AND t.queueType = :queueType AND t.branchOffice = :branchOffice")
    long countByStatusAndQueueTypeAndBranchOffice(@Param("status") TicketStatus status, 
                                                  @Param("queueType") QueueType queueType, 
                                                  @Param("branchOffice") String branchOffice);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'WAITING' AND t.queueType = :queueType AND t.branchOffice = :branchOffice ORDER BY t.createdAt ASC")
    List<Ticket> findWaitingTicketsByQueueTypeAndBranchOffice(@Param("queueType") QueueType queueType, 
                                                              @Param("branchOffice") String branchOffice);
}