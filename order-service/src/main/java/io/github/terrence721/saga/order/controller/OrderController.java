package io.github.terrence721.saga.order.controller;

import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.dto.CreateOrderRequest;
import io.github.terrence721.saga.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-Perimeter-User-Id", required = false) String perimeterUserId) {

        // api-gateway-service's JwtPerimeterGuardGatewayFilterFactory injects this
        // header from a verified JWT before forwarding here - without this check,
        // any caller could set customerId in the body to someone else's ID and
        // create orders in their name. A missing header (e.g. a direct call that
        // bypassed the gateway) is treated the same as a mismatch: fail closed.
        if (perimeterUserId == null || !perimeterUserId.equals(request.customerId().toString())) {
            // perimeterUserId is client-controlled (a raw request header) whenever
            // this check actually fires - a caller that bypassed the gateway could
            // set it to anything, so it needs the same CR/LF sanitizing as itemCode
            // below before it's safe to log. A plain if-block reassignment, not a
            // String.valueOf(...)-wrapped or ternary-embedded call, is what CodeQL's
            // log-injection sanitizer detection actually recognizes as a barrier
            // (both of those were tried first and still flagged post-PR #55).
            String sanitizedPerimeterUserId = "null";
            if (perimeterUserId != null) {
                sanitizedPerimeterUserId = perimeterUserId.replaceAll("[\r\n]", "_");
            }
            log.warn("Rejected create order request: X-Perimeter-User-Id ({}) does not match customerId ({})",
                    sanitizedPerimeterUserId, request.customerId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated caller does not match customerId");
        }

        // Logs fields individually, not the raw request - itemCode is free-form
        // client input with no character restrictions, so interpolating the
        // whole record's toString() would let CR/LF bytes forge fake log lines.
        log.info("Received create order request: customerId={}, itemCode={}, quantity={}, totalAmount={}",
                request.customerId(), request.itemCode().replaceAll("[\r\n]", "_"),
                request.quantity(), request.totalAmount());
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
