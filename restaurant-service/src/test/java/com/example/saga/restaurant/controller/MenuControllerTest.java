package com.example.saga.restaurant.controller;

import com.example.saga.restaurant.domain.MenuItem;
import com.example.saga.restaurant.domain.MenuCategory;
import com.example.saga.restaurant.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @Test
    void list_returnsAvailableItems() throws Exception {
        MenuItem item = MenuItem.builder()
                .itemCode("MARGHERITA")
                .name("Margherita")
                .description("Tomato, mozzarella, basil")
                .category(MenuCategory.Pizza)
                .price(new BigDecimal("8.50"))
                .available(true)
                .build();
        when(menuService.listAvailable()).thenReturn(List.of(item));

        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("MARGHERITA"))
                .andExpect(jsonPath("$[0].category").value("Pizza"));
    }
}
