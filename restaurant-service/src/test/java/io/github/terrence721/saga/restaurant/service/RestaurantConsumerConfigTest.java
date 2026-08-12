package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.restaurant.dto.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestaurantConsumerConfigTest {

    @Mock
    private RestaurantService restaurantService;

    private RestaurantConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new RestaurantConsumerConfig(restaurantService, new ObjectMapper());
    }

    @Test
    void onPaymentProcessed_deserializesPayloadAndDelegatesToProcessRestaurantStep() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new PaymentProcessedEvent(orderId, customerId, "PIZZA_01", 2, new BigDecimal("19.99"), PaymentStatus.APPROVED));

        consumerConfig.onPaymentProcessed(payload);

        ArgumentCaptor<PaymentProcessedEvent> captor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(restaurantService).processRestaurantStep(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(new BigDecimal("19.99"));
    }
}
