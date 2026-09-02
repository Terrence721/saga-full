package io.github.terrence721.saga.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import io.github.terrence721.saga.order.dto.CreateOrderRequest;
import io.github.terrence721.saga.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_returnsCreated_whenRequestIsValid() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order savedOrder = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("25.50"))
                .itemCode("BURGER_01")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(savedOrder);

        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(customerId, new BigDecimal("25.50"), "BURGER_01", 2));
        mockMvc.perform(post("/orders")
                        .header("X-Perimeter-User-Id", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_returnsBadRequest_whenPayloadFailsValidation() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(null, new BigDecimal("-10.00"), "", 0));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returnsBadRequest_whenItemCodeExceedsMaxLength() throws Exception {
        String tooLongItemCode = "A".repeat(256);

        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(UUID.randomUUID(), new BigDecimal("25.50"), tooLongItemCode, 2));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returnsBadRequest_whenTotalAmountHasTooManyIntegerDigits() throws Exception {
        BigDecimal tooLarge = new BigDecimal("123456789012345678.00");

        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(UUID.randomUUID(), tooLarge, "BURGER_01", 2));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returnsForbidden_whenPerimeterHeaderMissing() throws Exception {
        UUID customerId = UUID.randomUUID();

        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(customerId, new BigDecimal("25.50"), "BURGER_01", 2));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_returnsForbidden_whenPerimeterHeaderDoesNotMatchCustomerId() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID differentCallerId = UUID.randomUUID();

        String requestBody = objectMapper.writeValueAsString(
                new CreateOrderRequest(customerId, new BigDecimal("25.50"), "BURGER_01", 2));
        mockMvc.perform(post("/orders")
                        .header("X-Perimeter-User-Id", differentCallerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }
}
