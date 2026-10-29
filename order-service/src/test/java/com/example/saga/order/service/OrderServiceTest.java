package com.example.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderStatus;
import com.example.saga.order.domain.OutboxRecord;
import com.example.saga.order.dto.CreateOrderRequest;
import com.example.saga.order.dto.OrderCreatedEvent;
import com.example.saga.order.dto.RestaurantApprovedEvent;
import com.example.saga.order.dto.RestaurantRejectedEvent;
import com.example.saga.order.dto.ReservationConfirmedEvent;
import com.example.saga.order.dto.ReservationRejectedEvent;
import com.example.saga.order.exception.OrderNotFoundException;
import com.example.saga.order.repository.OrderRepository;
import com.example.saga.order.repository.OutboxRepository;
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
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OrderUpdatePublisher orderUpdatePublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxRepository,
                new OutboxRecordFactory(new ObjectMapper()), orderUpdatePublisher);
    }

    private Order pendingOrder(UUID id) {
        Order order = Order.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("19.99"))
                .itemCode("ITEM-1")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();
        return order;
    }

    @Test
    void createOrder_savesOrderAndStagesOutboxWithRealSerializedPayload() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), "ITEM-1", new BigDecimal("19.99"), 2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

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
    void createOrder_publishesOrderUpdateOnCreation() {
        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), "ITEM-1", new BigDecimal("19.99"), 2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        Order saved = orderService.createOrder(request);

        verify(orderUpdatePublisher).publish(saved);
    }

    @Test
    void confirmOrder_shortCircuits_whenAlreadySuccess() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(true);

        orderService.confirmOrder(new RestaurantApprovedEvent(orderId, UUID.randomUUID(), UUID.randomUUID()));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
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
        verify(orderUpdatePublisher).publish(existing);
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
        verify(orderUpdatePublisher, never()).publish(any());
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
        verify(orderUpdatePublisher).publish(existing);
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

    @Test
    void getOrder_returnsPersistedOrder() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        assertThat(orderService.getOrder(orderId)).isEqualTo(existing);
    }

    @Test
    void handleReservationConfirmed_shortCircuits_whenAlreadySuccess() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(true);

        orderService.handleReservationConfirmed(new ReservationConfirmedEvent(orderId, UUID.randomUUID(), "T-1", 4));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void handleReservationConfirmed_transitionsToSuccess_whenPending() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.handleReservationConfirmed(new ReservationConfirmedEvent(orderId, existing.getCustomerId(), "T-1", 4));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        verify(orderRepository).save(existing);
        verify(orderUpdatePublisher).publish(existing);
    }

    @Test
    void handleReservationConfirmed_throwsOrderNotFoundException_whenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.handleReservationConfirmed(
                new ReservationConfirmedEvent(orderId, UUID.randomUUID(), "T-1", 4)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void handleReservationConfirmed_throwsIllegalArgumentException_whenCustomerIdDoesNotMatchOrder() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        UUID mismatchedCustomerId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.handleReservationConfirmed(
                new ReservationConfirmedEvent(orderId, mismatchedCustomerId, "T-1", 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existing.getCustomerId().toString())
                .hasMessageContaining(mismatchedCustomerId.toString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleReservationRejected_shortCircuits_whenAlreadyCancelled() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(true);

        orderService.handleReservationRejected(new ReservationRejectedEvent(orderId, UUID.randomUUID(), "no table"));

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void handleReservationRejected_transitionsToCancelled_whenPending() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.handleReservationRejected(new ReservationRejectedEvent(orderId, existing.getCustomerId(), "no table"));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(existing);
        verify(orderUpdatePublisher).publish(existing);
    }

    @Test
    void handleReservationRejected_throwsOrderNotFoundException_whenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.handleReservationRejected(
                new ReservationRejectedEvent(orderId, UUID.randomUUID(), "no table")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void handleReservationRejected_throwsIllegalArgumentException_whenCustomerIdDoesNotMatchOrder() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        UUID mismatchedCustomerId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.handleReservationRejected(
                new ReservationRejectedEvent(orderId, mismatchedCustomerId, "no table")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existing.getCustomerId().toString())
                .hasMessageContaining(mismatchedCustomerId.toString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrder_transitionsToSuccess_afterDoubleCheck() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        // existsByIdAndStatus first call returns false, then upon find it's already? This exercises the second guard line.
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        orderService.confirmOrder(new RestaurantApprovedEvent(orderId, existing.getCustomerId(), UUID.randomUUID()));
        assertThat(existing.getStatus()).isEqualTo(OrderStatus.SUCCESS);
    }

    @Test
    void confirmOrder_throwsIllegalArgumentException_whenEventIsNull() {
        assertThatThrownBy(() -> orderService.confirmOrder(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event is required");
    }

    @Test
    void confirmOrder_throwsIllegalArgumentException_whenOrderIdIsNull() {
        assertThatThrownBy(() -> orderService.confirmOrder(
                new RestaurantApprovedEvent(null, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required for approval event");
    }

    @Test
    void confirmOrder_skips_whenAlreadySuccessAfterLoad() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        existing.setStatus(OrderStatus.SUCCESS);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.confirmOrder(new RestaurantApprovedEvent(orderId, existing.getCustomerId(), UUID.randomUUID()));

        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void cancelOrder_throwsIllegalArgumentException_whenEventIsNull() {
        assertThatThrownBy(() -> orderService.cancelOrder(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event is required");
    }

    @Test
    void cancelOrder_throwsIllegalArgumentException_whenOrderIdIsNull() {
        assertThatThrownBy(() -> orderService.cancelOrder(
                new RestaurantRejectedEvent(null, UUID.randomUUID(), "Out of stock")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required for rejection event");
    }

    @Test
    void cancelOrder_skips_whenAlreadyCancelledAfterLoad() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        existing.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.cancelOrder(new RestaurantRejectedEvent(orderId, existing.getCustomerId(), "Out of stock"));

        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void handleReservationConfirmed_throwsIllegalArgumentException_whenEventIsNull() {
        assertThatThrownBy(() -> orderService.handleReservationConfirmed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event is required");
    }

    @Test
    void handleReservationConfirmed_throwsIllegalArgumentException_whenOrderIdIsNull() {
        assertThatThrownBy(() -> orderService.handleReservationConfirmed(
                new ReservationConfirmedEvent(null, UUID.randomUUID(), "T-1", 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required for reservation confirmation");
    }

    @Test
    void handleReservationConfirmed_skips_whenAlreadySuccessAfterLoad() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        existing.setStatus(OrderStatus.SUCCESS);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.SUCCESS)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.handleReservationConfirmed(new ReservationConfirmedEvent(orderId, existing.getCustomerId(), "T-1", 4));

        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void handleReservationRejected_throwsIllegalArgumentException_whenEventIsNull() {
        assertThatThrownBy(() -> orderService.handleReservationRejected(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event is required");
    }

    @Test
    void handleReservationRejected_throwsIllegalArgumentException_whenOrderIdIsNull() {
        assertThatThrownBy(() -> orderService.handleReservationRejected(
                new ReservationRejectedEvent(null, UUID.randomUUID(), "no table")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required for reservation rejection");
    }

    @Test
    void handleReservationRejected_skips_whenAlreadyCancelledAfterLoad() {
        UUID orderId = UUID.randomUUID();
        Order existing = pendingOrder(orderId);
        existing.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.existsByIdAndStatus(orderId, OrderStatus.CANCELLED)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        orderService.handleReservationRejected(new ReservationRejectedEvent(orderId, existing.getCustomerId(), "no table"));

        verify(orderRepository, never()).save(any());
        verify(orderUpdatePublisher, never()).publish(any());
    }

    @Test
    void getOrder_throwsIllegalArgumentException_whenOrderIdIsNull() {
        assertThatThrownBy(() -> orderService.getOrder(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required");
    }

}
