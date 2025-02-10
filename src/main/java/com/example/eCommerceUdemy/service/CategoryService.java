package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.Category;
import com.example.eCommerceUdemy.payload.CategoryDTO;
import com.example.eCommerceUdemy.payload.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse findAllCategories(Integer pageNumber, Integer pageSize,String sortBy,String sortOrder);
    CategoryDTO save(CategoryDTO categoryDTO);
    CategoryDTO deleteById(Long id);
    CategoryDTO update(CategoryDTO categoryDTO,Long id);
}
