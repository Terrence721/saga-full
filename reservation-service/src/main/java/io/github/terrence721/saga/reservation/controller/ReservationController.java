package io.github.terrence721.saga.reservation.controller;

import io.github.terrence721.saga.reservation.domain.Reservation;
import io.github.terrence721.saga.reservation.dto.CreateReservationRequest;
import io.github.terrence721.saga.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation create(@Valid @RequestBody CreateReservationRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Reservation> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Reservation get(@PathVariable @NonNull UUID id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable @NonNull UUID id) {
        service.cancel(id);
    }
}
