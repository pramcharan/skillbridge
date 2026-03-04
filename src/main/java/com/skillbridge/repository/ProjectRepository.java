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
}