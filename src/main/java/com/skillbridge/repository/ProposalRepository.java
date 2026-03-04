package com.skillbridge.repository;

import com.skillbridge.entity.Job;
import com.skillbridge.entity.Proposal;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    boolean existsByJobAndFreelancer(Job job, User freelancer);
    Optional<Proposal> findByJobAndFreelancer(Job job, User freelancer);
    Page<Proposal> findByFreelancer(User freelancer, Pageable pageable);
    List<Proposal> findByJobOrderByAiMatchScoreDesc(Job job);
    List<Proposal> findByJobAndStatusNot(Job job, ProposalStatus status);
    long countByJobAndStatus(Job job, ProposalStatus status);
}