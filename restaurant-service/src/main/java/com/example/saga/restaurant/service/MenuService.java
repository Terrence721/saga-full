package com.example.saga.restaurant.service;

import com.example.saga.restaurant.domain.MenuItem;
import com.example.saga.restaurant.repository.MenuCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuCatalog repository;

    public MenuService(MenuCatalog repository) {
        this.repository = repository;
    }

    public List<MenuItem> listAvailable() {
        return repository.findAll().stream()
            .filter(item -> item != null && item.isAvailable())
            .toList();
    }
}
