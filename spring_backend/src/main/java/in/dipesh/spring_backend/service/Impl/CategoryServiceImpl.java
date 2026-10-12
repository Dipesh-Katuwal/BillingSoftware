package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import in.dipesh.spring_backend.entity.CategoryEntity;
import in.dipesh.spring_backend.service.CategoryService;
import lombok.AllArgsConstructor;
import in.dipesh.spring_backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {
    private CategoryRepository repository;

    @Override
    public CategoryResponse addCategory(CategoryRequest request) {
        CategoryEntity newEntity = convertToEntity(request);
        repository.save(newEntity);
        return convertToResponse(newEntity);
    }

    @Override
    public List<CategoryResponse> getCategories() {
        return repository.findAll().stream().map(this::convertToResponse).toList();
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
