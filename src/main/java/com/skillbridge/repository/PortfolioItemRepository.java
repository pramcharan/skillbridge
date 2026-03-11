package com.skillbridge.repository;

import com.skillbridge.entity.PortfolioItem;
import com.skillbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByUserOrderByCreatedAtDesc(User user);
    List<PortfolioItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}