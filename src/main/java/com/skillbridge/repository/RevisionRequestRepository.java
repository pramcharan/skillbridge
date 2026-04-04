package com.skillbridge.repository;

import com.skillbridge.entity.RevisionRequest;
import com.skillbridge.entity.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RevisionRequestRepository
        extends JpaRepository<RevisionRequest, Long> {

    @Query("SELECT r FROM RevisionRequest r JOIN FETCH r.requester " +
            "WHERE r.project.id = :projectId ORDER BY r.createdAt DESC")
    List<RevisionRequest> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT r FROM RevisionRequest r JOIN FETCH r.requester " +
            "WHERE r.project.id = :projectId AND r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<RevisionRequest> findByProjectIdAndStatus(
            @Param("projectId") Long projectId,
            @Param("status") RevisionStatus status);

    long countByProjectIdAndStatus(Long projectId, RevisionStatus status);
}