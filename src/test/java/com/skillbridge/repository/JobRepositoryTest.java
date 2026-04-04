package com.skillbridge.repository;

import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("JobRepository Slice Tests")
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User client;
    private Job techJob;
    private Job designJob;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();

        client = new User();
        client.setEmail("client@test.com");
        client.setName("Alice Wonder");
        client.setPasswordHash("hashed");
        client.setRole(Role.CLIENT);
        client.setIsActive(true);
        client = userRepository.save(client);

        techJob = new Job();
        techJob.setTitle("Spring Boot API Development");
        techJob.setDescription("Build a REST API with Spring Boot and MySQL");
        techJob.setCategory(JobCategory.TECHNOLOGY);
        techJob.setBudget(1500.0);
        techJob.setStatus(JobStatus.OPEN);
        techJob.setClient(client);
        techJob.setRequiredSkills("Java,Spring Boot,MySQL");

        designJob = new Job();
        designJob.setTitle("Logo Design Project");
        designJob.setDescription("Design a modern logo for a tech startup");
        designJob.setCategory(JobCategory.PHOTOGRAPHY);
        designJob.setBudget(300.0);
        designJob.setStatus(JobStatus.OPEN);
        designJob.setClient(client);
        designJob.setRequiredSkills("Photoshop,Illustrator");

        jobRepository.saveAll(List.of(techJob, designJob));
    }

    @Test
    @DisplayName("findById loads job")
    void findById_loadsJob() {
        Optional<Job> found = jobRepository.findById(techJob.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Spring Boot API Development");
    }

    @Test
    @DisplayName("searchJobs returns all jobs when no filter applied")
    void searchJobs_noFilter_returnsAll() {
        Page<Job> page = jobRepository.searchJobs(
                null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("searchJobs filters by category")
    void searchJobs_categoryFilter() {
        Page<Job> page = jobRepository.searchJobs(
                null, JobCategory.TECHNOLOGY, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Spring Boot API Development");
    }

    @Test
    @DisplayName("searchJobs keyword search matches title and description")
    void searchJobs_keywordFilter() {
        Page<Job> page = jobRepository.searchJobs(
                "MySQL", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Spring Boot API Development");
    }

    @Test
    @DisplayName("searchJobs filters by budget range")
    void searchJobs_budgetFilter() {
        Page<Job> page = jobRepository.searchJobs(
                null, null, 400.0, 600.0, PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByClient returns jobs for specific client")
    void findByClient_returnsClientJobs() {
        Page<Job> jobs = jobRepository.findByClient(client, PageRequest.of(0, 10));

        assertThat(jobs.getTotalElements()).isEqualTo(2);
        assertThat(jobs.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByStatus returns only OPEN jobs")
    void findByStatus_openOnly() {
        techJob.setStatus(JobStatus.CLOSED);
        jobRepository.save(techJob);

        Page<Job> open = jobRepository.findByStatus(JobStatus.OPEN, PageRequest.of(0, 10));

        assertThat(open.getTotalElements()).isEqualTo(1);
        assertThat(open.getContent().get(0).getTitle()).isEqualTo("Logo Design Project");
    }
}