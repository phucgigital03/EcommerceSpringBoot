package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    public Category findByCategoryName(String name);
}
