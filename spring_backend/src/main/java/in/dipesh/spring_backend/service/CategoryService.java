package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    public CategoryResponse addCategory(CategoryRequest request, MultipartFile file);
    public List<CategoryResponse> getCategories();
    public void deleteCategory(String categoryId);
}
