package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    public CategoryResponse addCategory(CategoryRequest request);
    public List<CategoryResponse> getCategories();
}
