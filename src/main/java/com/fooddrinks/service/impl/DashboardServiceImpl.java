package com.fooddrinks.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddrinks.dto.response.DashboardStatsResponse;
import com.fooddrinks.entity.OrderStatus;
import com.fooddrinks.entity.SuggestionStatus;
import com.fooddrinks.repository.OrderRepository;
import com.fooddrinks.repository.ProductRepository;
import com.fooddrinks.repository.SuggestionRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.DashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SuggestionRepository suggestionRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardStatsResponse getStats() {
        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByIsActive(true))
                .totalProducts(productRepository.count())
                .activeProducts(productRepository.countByIsActive(true))
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .completedOrders(orderRepository.countByStatus(OrderStatus.COMPLETED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .totalRevenue(orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED))
                .totalSuggestions(suggestionRepository.count())
                .pendingSuggestions(suggestionRepository.countByStatus(SuggestionStatus.PENDING))
                .build();
    }
}
