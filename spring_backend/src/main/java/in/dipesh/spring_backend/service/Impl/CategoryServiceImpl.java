package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import in.dipesh.spring_backend.entity.CategoryEntity;
import in.dipesh.spring_backend.service.CategoryService;
import in.dipesh.spring_backend.service.FileUploadService;
import lombok.AllArgsConstructor;
import in.dipesh.spring_backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {
    private CategoryRepository repository;
    private FileUploadService fileUploadService;

    @Override
    public CategoryResponse addCategory(CategoryRequest request, MultipartFile file) {
        String imgUrl = fileUploadService.uploadFile(file);
        CategoryEntity newEntity = convertToEntity(request);
        newEntity.setImgUrl(imgUrl);
        repository.save(newEntity);
        return convertToResponse(newEntity);
    }

    @Override
    public List<CategoryResponse> getCategories() {
        return repository.findAll().stream().map(this::convertToResponse).toList();
    }

    @Override
    public void deleteCategory(String categoryId) {
        CategoryEntity category = repository.findByCategoryId(categoryId).orElseThrow(()-> new RuntimeException("category not found"+categoryId));
        fileUploadService.deleteFile(category.getImgUrl());
        repository.delete(category);
    }

    private CategoryResponse convertToResponse(CategoryEntity newEntity) {
        return CategoryResponse.builder()
                .categoryId(newEntity.getCategoryId())
                .name(newEntity.getName())
                .description(newEntity.getDescription())
                .bgColor(newEntity.getBgColor())
                .imgUrl(newEntity.getImgUrl())
                .createdAt(newEntity.getCreatedAt())
                .updatedAt(newEntity.getUpdatedAt()).build();
    }
    public CategoryEntity convertToEntity(CategoryRequest request){
        return CategoryEntity.builder()
                .categoryId(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .bgColor(request.getBgColor()).build();
    }
}
