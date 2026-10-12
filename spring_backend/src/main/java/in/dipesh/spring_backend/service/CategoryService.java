package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import org.springframework.stereotype.Service;

public interface CategoryService {
    public CategoryResponse addCategory(CategoryRequest request);
}
