package com.skillbridge.service;

import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.SavedJob;
import com.skillbridge.entity.User;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.SavedJobRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository      jobRepository;
    private final UserRepository     userRepository;
    private final JobService         jobService;

    @Transactional
    public void saveJob(Long jobId, String email) {
        User user = findUser(email);
        if (savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw new BadRequestException("Job already saved.");
        }
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        SavedJob sj = new SavedJob();
        sj.setUser(user);
        sj.setJob(job);
        savedJobRepository.save(sj);
    }

    @Transactional
    public void unsaveJob(Long jobId, String email) {
        User user = findUser(email);
        savedJobRepository.deleteByUserIdAndJobId(user.getId(), jobId);
    }

    @Transactional(readOnly = true)
    public List<JobCardResponse> getSavedJobs(String email) {
        User user = findUser(email);
        return savedJobRepository.findByUserId(user.getId())
                .stream()
                .map(sj -> jobService.toCardResponse(sj.getJob()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<Long> getSavedJobIds(String email) {
        User user = findUser(email);
        return savedJobRepository.findByUserId(user.getId())
                .stream()
                .map(sj -> sj.getJob().getId())
                .collect(Collectors.toSet());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}