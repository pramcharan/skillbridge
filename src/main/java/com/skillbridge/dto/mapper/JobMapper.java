package com.skillbridge.dto.mapper;

import com.skillbridge.dto.request.JobAttachmentDTO;
import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.dto.response.JobDetailResponse;
import com.skillbridge.entity.Job;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobMapper {

    public JobCardResponse toCardResponse(Job job) {
        JobCardResponse r = new JobCardResponse();
        r.setId(job.getId());
        r.setTitle(job.getTitle());
        r.setCategory(job.getCategory());
        r.setStatus(job.getStatus());
        r.setBudget(job.getBudget());
        r.setDeadline(job.getDeadline());
        r.setProposalCount(job.getProposalCount());
        r.setRequiredSkills(splitSkills(job.getRequiredSkills()));
        r.setClientName(job.getClient() != null ? job.getClient().getName() : null);
        r.setCreatedAt(job.getCreatedAt());
        return r;
    }

    public JobDetailResponse toDetailResponse(Job job) {
        JobDetailResponse r = new JobDetailResponse();
        r.setId(job.getId());
        r.setTitle(job.getTitle());
        r.setDescription(job.getDescription());
        r.setCategory(job.getCategory());
        r.setStatus(job.getStatus());
        r.setBudget(job.getBudget());
        r.setDeadline(job.getDeadline());
        r.setAutoExpireAt(job.getAutoExpireAt());
        r.setProposalCount(job.getProposalCount());
        r.setRequiredSkills(splitSkills(job.getRequiredSkills()));
        r.setCreatedAt(job.getCreatedAt());
        if (job.getClient() != null) {
            r.setClientId(job.getClient().getId());
            r.setClientName(job.getClient().getName());
            r.setClientRating(job.getClient().getAvgRating());
        }
        r.setAttachments(parseAttachments(job));
        return r;
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    private List<JobAttachmentDTO> parseAttachments(Job job) {
        List<String> urls  = parseCsv(job.getAttachmentUrls());
        List<String> names = parseCsv(job.getAttachmentNames());
        List<JobAttachmentDTO> result = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            result.add(new JobAttachmentDTO(
                    urls.get(i),
                    i < names.size() ? names.get(i) : "attachment-" + (i + 1)
            ));
        }
        return result;
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) return List.of();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}