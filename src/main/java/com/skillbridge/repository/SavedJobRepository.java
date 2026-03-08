package com.skillbridge.repository;

import com.skillbridge.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    Optional<SavedJob> findByUserIdAndJobId(Long userId, Long jobId);

    @Query("SELECT sj FROM SavedJob sj JOIN FETCH sj.job j JOIN FETCH j.client " +
            "WHERE sj.user.id = :userId ORDER BY sj.createdAt DESC")
    List<SavedJob> findByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndJobId(Long userId, Long jobId);
}