package com.shopsphere.orderservice.repository;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Customer order history
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Single order — verify ownership
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Admin — all orders with pagination
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Admin — filter by status
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    // Draft order for checkout
    Optional<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
}