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
            "ORDER BY COALESCE(p.aiMatchScore, 0) DESC")
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

    // Count by freelancer
    long countByFreelancerId(Long freelancerId);

    // Count by freelancer + status
    long countByFreelancerIdAndStatus(Long freelancerId, ProposalStatus status);

    // Average AI match score for a freelancer
    @Query("SELECT AVG(p.aiMatchScore) FROM Proposal p " +
            "WHERE p.freelancer.id = :freelancerId " +
            "AND p.aiMatchScore IS NOT NULL")
    java.util.Optional<Double> findAverageMatchScoreByFreelancerId(
            @Param("freelancerId") Long freelancerId);

    // Total proposals received by client
    @Query("SELECT COUNT(p) FROM Proposal p " +
            "WHERE p.job.client.id = :clientId")
    int countProposalsForClient(@Param("clientId") Long clientId);

    // Unread proposals for client
    @Query("SELECT COUNT(p) FROM Proposal p " +
            "WHERE p.job.client.id = :clientId " +
            "AND p.viewedByClient = false " +
            "AND p.status = 'PENDING'")
    int countUnreadProposalsForClient(@Param("clientId") Long clientId);

    long count();


    @Query("SELECT p FROM Proposal p JOIN FETCH p.freelancer JOIN FETCH p.job " +
            "WHERE p.job.id = :jobId ORDER BY p.aiMatchScore DESC")
    List<Proposal> findByJobIdWithDetails(@Param("jobId") Long jobId);

    @Query("SELECT p FROM Proposal p JOIN FETCH p.job j JOIN FETCH j.client " +
            "WHERE p.freelancer.email = :email ORDER BY p.createdAt DESC")
    List<Proposal> findByFreelancerEmailWithDetails(@Param("email") String email);


    @Query("SELECT p.status, COUNT(p) FROM Proposal p GROUP BY p.status")
    List<Object[]> countGroupByStatus();



    @Query("SELECT AVG(p.aiScore) FROM Proposal p WHERE p.status = :status AND p.aiScore IS NOT NULL")
    Optional<Double> avgScoreByStatus(@Param("status") ProposalStatus status);

    @Query("""
    SELECT
      CASE
        WHEN p.aiScore BETWEEN 0  AND 20  THEN '0–20'
        WHEN p.aiScore BETWEEN 21 AND 40  THEN '21–40'
        WHEN p.aiScore BETWEEN 41 AND 60  THEN '41–60'
        WHEN p.aiScore BETWEEN 61 AND 80  THEN '61–80'
        ELSE '81–100'
      END,
      COUNT(p)
    FROM Proposal p
    WHERE p.aiScore IS NOT NULL
    GROUP BY 1
    ORDER BY 1
    """)
    List<Object[]> scoreDistribution();

    @Query("""
    SELECT j.title, AVG(p.aiScore)
    FROM Proposal p JOIN p.job j
    WHERE p.aiScore IS NOT NULL
    GROUP BY j.id, j.title
    ORDER BY AVG(p.aiScore) DESC
    """)
    List<Object[]> topJobsByAvgScore(Pageable pageable);

    default List<Object[]> topJobsByAvgScore() {
        return topJobsByAvgScore(
                org.springframework.data.domain.PageRequest.of(0, 5));
    }

}