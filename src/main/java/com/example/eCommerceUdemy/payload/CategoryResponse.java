package com.example.eCommerceUdemy.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class CategoryResponse {
    List<CategoryDTO> content;
    Integer pageNumber;
    Integer pageSize;
    Integer totalElements;
    Integer totalPages;
    boolean lastPage;
}
