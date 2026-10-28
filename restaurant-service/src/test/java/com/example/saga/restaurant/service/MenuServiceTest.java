package com.example.saga.restaurant.service;

import com.example.saga.restaurant.domain.MenuItem;
import com.example.saga.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class MenuServiceTest {

    @Mock
    private MenuItemRepository repository;

    @Test
    void listAvailable_filtersUnavailable() {
        MenuItem available = com.example.saga.restaurant.domain.MenuItem.builder()
                .itemCode("P1").name("P1").description("d").category(com.example.saga.restaurant.domain.MenuCategory.Pizza)
                .price(new BigDecimal("1.00")).available(true).build();
        MenuItem unavailable = com.example.saga.restaurant.domain.MenuItem.builder()
                .itemCode("P2").name("P2").description("d").category(com.example.saga.restaurant.domain.MenuCategory.Pizza)
                .price(new BigDecimal("1.00")).available(false).build();
        when(repository.findAll()).thenReturn(List.of(available, unavailable));

        MenuService service = new MenuService(repository);
        assertThat(service.listAvailable()).containsExactly(available);
    }
}
