package com.skillbridge.dto.mapper;

import com.skillbridge.dto.response.ProposalResponse;
import com.skillbridge.dto.response.ProposalSummaryResponse;
import com.skillbridge.entity.Proposal;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProposalMapper {

    public ProposalResponse toResponse(Proposal p) {
        ProposalResponse r = new ProposalResponse();
        r.setId(p.getId());
        r.setCoverLetter(p.getCoverLetter());
        r.setAttachmentUrl(p.getAttachmentUrl());
        r.setExpectedRate(p.getExpectedRate());
        r.setStatus(p.getStatus());
        r.setViewedByClient(p.getViewedByClient());
        r.setCreatedAt(p.getCreatedAt());
        r.setAiMatchScore(p.getAiMatchScore());
        r.setAiMatchBadge(p.getAiMatchBadge());
        r.setAiMatchReason(p.getAiMatchReason());

        if (p.getJob() != null) {
            r.setJobId(p.getJob().getId());
            r.setJobTitle(p.getJob().getTitle());
        }

        if (p.getFreelancer() != null) {
            r.setFreelancerId(p.getFreelancer().getId());
            r.setFreelancerName(p.getFreelancer().getName());
            r.setFreelancerAvatarUrl(p.getFreelancer().getAvatarUrl());
            r.setFreelancerRating(p.getFreelancer().getAvgRating());
            r.setFreelancerSkills(splitSkills(p.getFreelancer().getSkills()));
        }

        // Parse matched/missing skills from aiMatchReason if stored as JSON
        // For now return empty — Day 8 will enrich this
        r.setMatchedSkills(List.of());
        r.setMissingSkills(List.of());

        return r;
    }

    public ProposalSummaryResponse toSummary(Proposal p) {
        ProposalSummaryResponse r = new ProposalSummaryResponse();
        r.setId(p.getId());
        r.setStatus(p.getStatus());
        r.setExpectedRate(p.getExpectedRate());
        r.setAiMatchScore(p.getAiMatchScore());
        r.setAiMatchBadge(p.getAiMatchBadge());
        r.setCreatedAt(p.getCreatedAt());

        if (p.getJob() != null) {
            r.setJobId(p.getJob().getId());
            r.setJobTitle(p.getJob().getTitle());
            r.setJobCategory(p.getJob().getCategory() != null
                    ? p.getJob().getCategory().name() : null);
            if (p.getJob().getClient() != null) {
                r.setClientName(p.getJob().getClient().getName());
            }
        }

        return r;
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) return List.of();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}