package com.example.saga.restaurant.service;

import com.example.saga.restaurant.domain.MenuItem;
import com.example.saga.restaurant.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository repository;

    public MenuService(MenuItemRepository repository) {
        this.repository = repository;
    }

    public List<MenuItem> listAvailable() {
        return repository.findAll().stream().filter(MenuItem::isAvailable).toList();
    }
}
