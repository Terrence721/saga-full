package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.domain.InventoryStatus;
import io.github.terrence721.saga.restaurant.domain.OutboxRecord;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicket;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicketStatus;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.restaurant.dto.PaymentStatus;
import io.github.terrence721.saga.restaurant.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.restaurant.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.restaurant.repository.OutboxRepository;
import io.github.terrence721.saga.restaurant.repository.RestaurantTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantTicketRepository ticketRepository;

    @Mock
    private RestaurantInventoryService inventoryService;

    @Mock
    private OutboxRepository outboxRepository;

    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantService(ticketRepository, inventoryService, outboxRepository, new ObjectMapper());
    }

    private PaymentProcessedEvent paymentProcessedEvent(UUID orderId, UUID customerId) {
        return new PaymentProcessedEvent(orderId, customerId, "PIZZA_01", 2, new BigDecimal("19.99"), PaymentStatus.APPROVED);
    }

    @Test
    void processRestaurantStep_skipsDuplicate_whenTicketAlreadyExists() {
        UUID orderId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(true);

        restaurantService.processRestaurantStep(paymentProcessedEvent(orderId, UUID.randomUUID()));

        verify(ticketRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processRestaurantStep_throwsIllegalArgumentException_whenQuantityNotPositive() {
        UUID orderId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                orderId, UUID.randomUUID(), "PIZZA_01", 0, new BigDecimal("19.99"), PaymentStatus.APPROVED);

        assertThatThrownBy(() -> restaurantService.processRestaurantStep(event))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void processRestaurantStep_throwsIllegalArgumentException_whenAmountNotPositive() {
        UUID orderId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                orderId, UUID.randomUUID(), "PIZZA_01", 2, BigDecimal.ZERO, PaymentStatus.APPROVED);

        assertThatThrownBy(() -> restaurantService.processRestaurantStep(event))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void processRestaurantStep_createsPreparingTicketAndApprovedOutbox_whenAllocated() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryService.verifyAndDeductStock("PIZZA_01", 2)).thenReturn(InventoryStatus.ALLOCATED);
        when(ticketRepository.save(any(RestaurantTicket.class))).thenAnswer(invocation -> {
            RestaurantTicket ticket = invocation.getArgument(0);
            ticket.setId(ticketId);
            return ticket;
        });

        restaurantService.processRestaurantStep(paymentProcessedEvent(orderId, customerId));

        ArgumentCaptor<RestaurantTicket> ticketCaptor = ArgumentCaptor.forClass(RestaurantTicket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(RestaurantTicketStatus.PREPARING);

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("RestaurantApprovedEvent");
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(orderId.toString());

        RestaurantApprovedEvent deserialized = new ObjectMapper()
                .readValue(outboxCaptor.getValue().getPayload(), RestaurantApprovedEvent.class);
        assertThat(deserialized.orderId()).isEqualTo(orderId);
        assertThat(deserialized.customerId()).isEqualTo(customerId);
        assertThat(deserialized.ticketId()).isEqualTo(ticketId);
    }

    @Test
    void processRestaurantStep_createsRejectedTicketAndRejectedOutbox_whenInsufficientStock() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryService.verifyAndDeductStock("PIZZA_01", 2)).thenReturn(InventoryStatus.INSUFFICIENT_STOCK);
        when(ticketRepository.save(any(RestaurantTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        restaurantService.processRestaurantStep(paymentProcessedEvent(orderId, customerId));

        ArgumentCaptor<RestaurantTicket> ticketCaptor = ArgumentCaptor.forClass(RestaurantTicket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(RestaurantTicketStatus.REJECTED);

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("RestaurantRejectedEvent");

        RestaurantRejectedEvent deserialized = new ObjectMapper()
                .readValue(outboxCaptor.getValue().getPayload(), RestaurantRejectedEvent.class);
        assertThat(deserialized.reason()).contains("Out of stock");
    }

    @Test
    void processRestaurantStep_createsRejectedTicketAndRejectedOutbox_whenPaymentFailed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        when(ticketRepository.save(any(RestaurantTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                orderId, customerId, "PIZZA_01", 2, new BigDecimal("19.99"), PaymentStatus.FAILED);

        restaurantService.processRestaurantStep(event);

        verifyNoInteractions(inventoryService);

        ArgumentCaptor<RestaurantTicket> ticketCaptor = ArgumentCaptor.forClass(RestaurantTicket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(RestaurantTicketStatus.REJECTED);

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("RestaurantRejectedEvent");

        RestaurantRejectedEvent deserialized = new ObjectMapper()
                .readValue(outboxCaptor.getValue().getPayload(), RestaurantRejectedEvent.class);
        assertThat(deserialized.reason()).contains("Payment failed");
    }

    @Test
    void processRestaurantStep_createsRejectedTicketAndRejectedOutbox_whenItemNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(ticketRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryService.verifyAndDeductStock("PIZZA_01", 2)).thenReturn(InventoryStatus.ITEM_NOT_FOUND);
        when(ticketRepository.save(any(RestaurantTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        restaurantService.processRestaurantStep(paymentProcessedEvent(orderId, customerId));

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        RestaurantRejectedEvent deserialized = new ObjectMapper()
                .readValue(outboxCaptor.getValue().getPayload(), RestaurantRejectedEvent.class);
        assertThat(deserialized.reason()).contains("Invalid item code");
    }
}
