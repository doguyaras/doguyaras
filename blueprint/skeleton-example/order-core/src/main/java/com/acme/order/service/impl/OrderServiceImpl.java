package com.acme.order.service.impl;
import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.exception.ErrorCode;
import com.acme.order.repository.OrderRepository;
import com.acme.order.service.OrderService;
import com.acme.platform.core.ServiceException;
import java.util.UUID;
import org.springframework.stereotype.Service;
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repo;
    public OrderServiceImpl(OrderRepository repo) { this.repo = repo; }
    public UUID create(UUID a, UUID k, CreateOrderRequest r) { return UUID.randomUUID(); }
    public void cancel(UUID a, UUID id) { repo.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND)); }
}
