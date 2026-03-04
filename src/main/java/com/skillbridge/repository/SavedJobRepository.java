package com.skillbridge.repository;

import com.skillbridge.entity.Job;
import com.skillbridge.entity.SavedJob;
import com.skillbridge.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    boolean existsByUserAndJob(User user, Job job);
    Optional<SavedJob> findByUserAndJob(User user, Job job);
    Page<SavedJob> findByUserOrderBySavedAtDesc(User user, Pageable pageable);
}