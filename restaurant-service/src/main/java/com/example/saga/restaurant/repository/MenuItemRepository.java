package com.example.saga.restaurant.repository;

import com.example.saga.restaurant.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
}
