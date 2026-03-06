package com.skillbridge.repository;

import com.skillbridge.entity.Proposal;
import com.skillbridge.entity.enums.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    // Check for duplicate application
    boolean existsByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    // All proposals for a specific job (client view) — sorted by AI score desc
    @Query("SELECT p FROM Proposal p " +
            "JOIN FETCH p.freelancer " +
            "JOIN FETCH p.job " +
            "WHERE p.job.id = :jobId " +
            "ORDER BY p.aiMatchScore DESC NULLS LAST")
    List<Proposal> findByJobIdOrderByAiMatchScoreDesc(@Param("jobId") Long jobId);

    // Freelancer's own proposals
    @Query("SELECT p FROM Proposal p " +
            "JOIN FETCH p.job j " +
            "JOIN FETCH j.client " +
            "WHERE p.freelancer.id = :freelancerId " +
            "ORDER BY p.createdAt DESC")
    Page<Proposal> findByFreelancerId(
            @Param("freelancerId") Long freelancerId, Pageable pageable);

    // Single proposal with full details
    @Query("SELECT p FROM Proposal p " +
            "JOIN FETCH p.freelancer " +
            "JOIN FETCH p.job j " +
            "JOIN FETCH j.client " +
            "WHERE p.id = :id")
    Optional<Proposal> findByIdWithDetails(@Param("id") Long id);

    // Count proposals per job
    int countByJobId(Long jobId);

    // Check if freelancer already applied
    Optional<Proposal> findByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    // Proposals by status for a job
    List<Proposal> findByJobIdAndStatus(Long jobId, ProposalStatus status);
}