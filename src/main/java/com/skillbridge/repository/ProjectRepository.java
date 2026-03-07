package com.skillbridge.repository;

import com.skillbridge.entity.Project;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByClient(User client, Pageable pageable);
    Page<Project> findByFreelancer(User freelancer, Pageable pageable);
    Optional<Project> findByProposalId(Long proposalId);

    @Query("SELECT p FROM Project p WHERE p.status = 'ACTIVE' AND " +
            "p.lastMessageAt < :cutoff")
    List<Project> findInactiveProjects(@Param("cutoff") Instant cutoff);

    long countByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.client " +
            "JOIN FETCH p.freelancer " +
            "WHERE p.client.id = :clientId " +
            "ORDER BY p.createdAt DESC")
    Page<Project> findByClientId(
            @Param("clientId") Long clientId, Pageable pageable);

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.client " +
            "JOIN FETCH p.freelancer " +
            "WHERE p.freelancer.id = :freelancerId " +
            "ORDER BY p.createdAt DESC")
    Page<Project> findByFreelancerId(
            @Param("freelancerId") Long freelancerId, Pageable pageable);

    List<Project> findByStatusAndLastMessageAtBefore(
            ProjectStatus status, Instant cutoff);

    int countByClientIdAndStatus(Long clientId, ProjectStatus status);

    int countByFreelancerIdAndStatus(Long freelancerId, ProjectStatus status);

    // Single project with full details
    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.client " +
            "JOIN FETCH p.freelancer " +
            "JOIN FETCH p.job " +
            "WHERE p.id = :id")
    java.util.Optional<Project> findByIdWithDetails(@Param("id") Long id);
}