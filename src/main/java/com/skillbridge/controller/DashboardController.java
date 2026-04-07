package com.skillbridge.controller;

import com.skillbridge.dto.response.ClientDashboardStats;
import com.skillbridge.dto.response.FreelancerDashboardStats;
import com.skillbridge.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
//
//    @GetMapping("/client/stats")
//    public ResponseEntity<ClientDashboardStats> getClientStats(
//            Principal principal) {
//
//        return ResponseEntity.ok(
//                dashboardService.getClientStats(principal.getName()));
//    }
//
//
//    @GetMapping("/freelancer/stats")
//    public ResponseEntity<FreelancerDashboardStats> getFreelancerStats(
//            Principal principal) {
//
//        return ResponseEntity.ok(
//                dashboardService.getFreelancerStats(principal.getName()));
//    }
    @GetMapping("/freelancer/stats")
    public ResponseEntity<FreelancerDashboardStats> getFreelancerStats(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                dashboardService.getFreelancerStats(email));
    }

    @GetMapping("/client/stats")
    public ResponseEntity<ClientDashboardStats> getClientStats(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                dashboardService.getClientStats(email));
    }
}