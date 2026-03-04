package com.skillbridge.repository;

import com.skillbridge.entity.DisputeTicket;
import com.skillbridge.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisputeTicketRepository extends JpaRepository<DisputeTicket, Long> {
    Optional<DisputeTicket> findByProjectId(Long projectId);
    Page<DisputeTicket> findByStatusIn(java.util.List<DisputeStatus> statuses, Pageable pageable);
}