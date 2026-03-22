package com.rabbitmqsample.orderservice.repository;

import com.rabbitmqsample.orderservice.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    @Query("select distinct o from OrderEntity o left join fetch o.orderDetails where o.id = :id")
    Optional<OrderEntity> findByIdWithOrderDetails(UUID id);
}
