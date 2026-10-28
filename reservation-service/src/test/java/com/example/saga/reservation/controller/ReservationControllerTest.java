package com.example.saga.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.reservation.domain.Reservation;
import com.example.saga.reservation.domain.ReservationStatus;
import com.example.saga.reservation.dto.CreateReservationRequest;
import com.example.saga.reservation.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@SuppressWarnings("null")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void create_returnsCreated_whenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        CreateReservationRequest request = new CreateReservationRequest(
                "customer-1", "T4", 4, Instant.parse("2026-10-28T18:00:00Z"));
        Reservation saved = Reservation.builder()
                .id(id)
                .customerId("customer-1")
                .tableNumber("T4")
                .partySize(4)
                .startTime(Instant.parse("2026-10-28T18:00:00Z"))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationService.create(any(CreateReservationRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void list_returnsAll() throws Exception {
        when(reservationService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    void get_returnsOk_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        Reservation saved = Reservation.builder()
                .id(id).customerId("customer-1").tableNumber("T4").partySize(4)
                .startTime(Instant.parse("2026-10-28T18:00:00Z"))
                .status(ReservationStatus.CONFIRMED).build();
        when(reservationService.findById(id)).thenReturn(saved);

        mockMvc.perform(get("/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.tableNumber").value("T4"));
    }

    @Test
    void cancel_returnsNoContent_whenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/reservations/{id}", id))
                .andExpect(status().isNoContent());
        verify(reservationService).cancel(id);
    }

    @Test
    void create_returnsBadRequest_whenRequestInvalid() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest("", "T4", 0, Instant.parse("2026-10-28T18:00:00Z"));
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
