package com.skillbridge.repository;

import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    Page<Job> findByStatusAndCategory(JobStatus status, JobCategory category, Pageable pageable);
    Page<Job> findByClient(User client, Pageable pageable);
    List<Job> findByStatusAndAutoExpireAtBefore(JobStatus status, Instant now);

    @Query("SELECT j FROM Job j WHERE j.status = 'OPEN' AND " +
            "(LOWER(j.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
            " LOWER(j.description) LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
            " LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%',:kw,'%')))")
    Page<Job> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = 'OPEN' AND j.category = :category AND " +
            "LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%',:skill,'%')) AND j.id != :excludeId")
    List<Job> findSimilarJobs(@Param("category") JobCategory category,
                              @Param("skill") String skill,
                              @Param("excludeId") Long excludeId,
                              Pageable pageable);

    @Query("SELECT j.category, COUNT(j) FROM Job j WHERE j.status = 'OPEN' GROUP BY j.category")
    List<Object[]> countByCategory();
}