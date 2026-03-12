package com.skillbridge.repository;

import com.skillbridge.entity.DisputeTicket;
import com.skillbridge.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository
        extends JpaRepository<DisputeTicket, Long> {

    @Query("""
        SELECT d FROM DisputeTicket d
        JOIN FETCH d.project
        JOIN FETCH d.reporter
        JOIN FETCH d.respondent
        WHERE d.reporter.id = :userId OR d.respondent.id = :userId
        ORDER BY d.createdAt DESC
        """)
    List<DisputeTicket> findByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT d FROM DisputeTicket d
        JOIN FETCH d.project
        JOIN FETCH d.reporter
        JOIN FETCH d.respondent
        WHERE d.status = :status
        ORDER BY d.createdAt ASC
        """)
    Page<DisputeTicket> findByStatus(
            @Param("status") DisputeStatus status, Pageable pageable);

    @Query("""
        SELECT d FROM DisputeTicket d
        JOIN FETCH d.project
        JOIN FETCH d.reporter
        JOIN FETCH d.respondent
        ORDER BY d.createdAt DESC
        """)
    Page<DisputeTicket> findAllForAdmin(Pageable pageable);

    Optional<DisputeTicket> findByProjectIdAndReporterId(
            Long projectId, Long reporterId);

    long countByStatus(DisputeStatus status);
}