package com.example.saga.restaurant.controller;

import com.example.saga.restaurant.domain.MenuItem;
import com.example.saga.restaurant.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @GetMapping
    public List<MenuItem> list() {
        return service.listAvailable();
    }
}
