package com.example.saga.restaurant.repository;

import com.example.saga.restaurant.domain.MenuItem;

import java.util.List;

/**
 * Narrow, domain-facing view of the menu catalogue (ISP/DIP): services depend on
 * this instead of the full JpaRepository surface.
 */
public interface MenuCatalog {
    List<MenuItem> findAll();
}