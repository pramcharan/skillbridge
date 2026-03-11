package com.skillbridge.service;

import com.skillbridge.dto.request.PortfolioItemRequest;
import com.skillbridge.dto.response.PortfolioItemResponse;
import com.skillbridge.entity.PortfolioItem;
import com.skillbridge.entity.User;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.PortfolioItemRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioItemRepository portfolioRepository;
    private final UserRepository          userRepository;

    private static final int MAX_PORTFOLIO_ITEMS = 20;

    // ── Get portfolio by userId ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> getPortfolio(Long userId) {
        return portfolioRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get my portfolio ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> getMyPortfolio(String email) {
        User user = getUser(email);
        return getPortfolio(user.getId());
    }

    // ── Add item ───────────────────────────────────────────────────────
    @Transactional
    public PortfolioItemResponse addItem(String email,
                                         PortfolioItemRequest req) {
        User user = getUser(email);

        if (portfolioRepository.countByUserId(user.getId())
                >= MAX_PORTFOLIO_ITEMS) {
            throw new BadRequestException(
                    "Portfolio limit reached (" + MAX_PORTFOLIO_ITEMS + " items max).");
        }

        PortfolioItem item = new PortfolioItem();
        item.setUser(user);
        applyRequest(item, req);

        return toResponse(portfolioRepository.save(item));
    }

    // ── Update item ────────────────────────────────────────────────────
    @Transactional
    public PortfolioItemResponse updateItem(Long itemId,
                                            String email,
                                            PortfolioItemRequest req) {
        PortfolioItem item = getOwnedItem(itemId, email);
        applyRequest(item, req);
        return toResponse(portfolioRepository.save(item));
    }

    // ── Delete item ────────────────────────────────────────────────────
    @Transactional
    public void deleteItem(Long itemId, String email) {
        PortfolioItem item = getOwnedItem(itemId, email);
        portfolioRepository.delete(item);
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private PortfolioItem getOwnedItem(Long itemId, String email) {
        PortfolioItem item = portfolioRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio item not found: " + itemId));
        if (!item.getUser().getEmail().equals(email)) {
            throw new BadRequestException(
                    "You can only edit your own portfolio items.");
        }
        return item;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private void applyRequest(PortfolioItem item, PortfolioItemRequest req) {
        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setProjectUrl(req.getProjectUrl());
        item.setImageUrl(req.getImageUrl());
        item.setTags(req.getTags());
        item.setCategory(req.getCategory());
    }

    private PortfolioItemResponse toResponse(PortfolioItem item) {
        PortfolioItemResponse r = new PortfolioItemResponse();
        r.setId(item.getId());
        r.setTitle(item.getTitle());
        r.setDescription(item.getDescription());
        r.setProjectUrl(item.getProjectUrl());
        r.setImageUrl(item.getImageUrl());
        r.setCategory(item.getCategory());
        r.setCreatedAt(item.getCreatedAt());
        r.setOwnerName(item.getUser().getName());
        r.setOwnerId(item.getUser().getId());
        r.setTags(item.getTags() != null
                ? Arrays.stream(item.getTags().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList())
                : List.of());
        return r;
    }
}