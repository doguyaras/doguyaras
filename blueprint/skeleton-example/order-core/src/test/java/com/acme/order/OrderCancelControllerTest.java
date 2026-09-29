package com.acme.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.acme.order.api.dto.CancelOrderRequest;
import com.acme.order.controller.OrderCancelController;
import com.acme.order.entity.Order;
import com.acme.order.repository.OrderRepository;
import com.acme.order.service.OrderCancelService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderCancelControllerTest {

    @Test
    void cancelsOrder() throws Exception {
        OrderRepository repo = mock(OrderRepository.class);
        OrderCancelService service = mock(OrderCancelService.class);
        when(repo.findById(any())).thenReturn(Optional.of(new Order()));
        OrderCancelController controller = new OrderCancelController();
        ReflectionTestUtils.setField(controller, "orderRepository", repo);
        ReflectionTestUtils.setField(controller, "cancelService", service);
        UUID account = UUID.randomUUID();
        var response = controller.cancel(UUID.randomUUID(), new CancelOrderRequest(account, "vazgectim", "+905551112233"));
        Thread.sleep(100);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("status");
    }
}
