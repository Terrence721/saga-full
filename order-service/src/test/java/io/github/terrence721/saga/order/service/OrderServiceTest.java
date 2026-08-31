package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import io.github.terrence721.saga.order.domain.OutboxRecord;
import io.github.terrence721.saga.order.dto.CreateOrderRequest;
import io.github.terrence721.saga.order.dto.OrderCreatedEvent;
import io.github.terrence721.saga.order.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.order.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.order.exception.OrderNotFoundException;
import io.github.terrence721.saga.order.repository.OrderRepository;
import io.github.terrence721.saga.order.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxRepository, new ObjectMapper());
    }

    private Order pendingOrder(UUID id) {
        return Order.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("19.99"))
                .itemCode("ITEM-1")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void createOrder_savesOrderAndStagesOutboxWithRealSerializedPayload() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), new BigDecimal("19.99"), "ITEM-1", 2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(outboxRepository.save(any(OutboxRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order saved = orderService.createOrder(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomerId()).isEqualTo(request.customerId());

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxRecord outboxRecord = outboxCaptor.getValue();

        assertThat(outboxRecord.getAggregateId()).isEqualTo(saved.getId().toString());
        assertThat(outboxRecord.getEventType()).isEqualTo("OrderCreatedEvent");

        OrderCreatedEvent deserialized = new ObjectMapper().readValue(outboxRecord.getPayload(), OrderCreatedEvent.class);
        assertThat(deserialized.orderId()).isEqualTo(saved.getId());
        assertThat(deserialized.customerId()).isEqualTo(saved.getCustomerId());
        assertThat(deserialized.totalAmount()).isEqualByComparingTo(saved.getTotalAmount());
        assertThat(deserialized.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void confirmOrder_shortCircuits_whenAlreadySuccess() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(true);

        orderService.confirmOrder(new RestaurantApprovedEvent(orderId, UUID.randomUUID(), UUID.randomUUID()));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrder_transitionsToSuccess_whenPending() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.confirmOrder(new RestaurantApprovedEvent(orderId, existing.getCustomerId(), UUID.randomUUID()));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        verify(orderRepository).save(existing);
    }

    @Test
    void confirmOrder_throwsOrderNotFoundException_whenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrder(
                new RestaurantApprovedEvent(orderId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void confirmOrder_throwsIllegalArgumentException_whenTicketIdMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);

        assertThatThrownBy(() -> orderService.confirmOrder(
                new RestaurantApprovedEvent(orderId, UUID.randomUUID(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(orderId.toString());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void confirmOrder_throwsIllegalArgumentException_whenCustomerIdDoesNotMatchOrder() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        UUID mismatchedCustomerId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.confirmOrder(
                new RestaurantApprovedEvent(orderId, mismatchedCustomerId, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existing.getCustomerId().toString())
                .hasMessageContaining(mismatchedCustomerId.toString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_shortCircuits_whenAlreadyCancelled() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(true);

        orderService.cancelOrder(new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock"));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_transitionsToCancelled_whenPending() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.cancelOrder(new RestaurantRejectedEvent(orderId, existing.getCustomerId(), "Out of stock"));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(existing);
    }

    @Test
    void cancelOrder_throwsOrderNotFoundException_whenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(
                new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_throwsIllegalArgumentException_whenCustomerIdDoesNotMatchOrder() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        UUID mismatchedCustomerId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.cancelOrder(
                new RestaurantRejectedEvent(orderId, mismatchedCustomerId, "Out of stock")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existing.getCustomerId().toString())
                .hasMessageContaining(mismatchedCustomerId.toString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_stillCancelsOrder_whenReasonContainsCrLf() {
        // CR/LF sanitization itself is verified statically by CodeQL's java/log-injection
        // query (the same gate that caught this vulnerability class three times already in
        // this module - #54, #58) rather than by inspecting log output here, matching this
        // repo's established convention. This test instead proves the sanitizing
        // replaceAll call doesn't disturb normal processing of a reason containing CR/LF.
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        String forgedReason = "Out of stock\r\nFAKE LOG LINE: order approved";

        orderService.cancelOrder(new RestaurantRejectedEvent(orderId, existing.getCustomerId(), forgedReason));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(existing);
    }
}
