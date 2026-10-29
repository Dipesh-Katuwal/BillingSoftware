package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.ItemRequest;
import in.dipesh.spring_backend.dto.ItemResponse;
import in.dipesh.spring_backend.entity.CategoryEntity;
import in.dipesh.spring_backend.entity.ItemEntity;
import in.dipesh.spring_backend.repository.CategoryRepository;
import in.dipesh.spring_backend.repository.ItemRepository;
import in.dipesh.spring_backend.service.FileUploadService;
import in.dipesh.spring_backend.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final FileUploadService fileUploadService;
    private final CategoryRepository categoryRepository;

    @Override
    public ItemResponse addItem(ItemRequest request, MultipartFile file) {
        String imgUrl = fileUploadService.uploadFile(file);
        ItemEntity entity = convertToEntity(request,imgUrl);
        repository.save(entity);
        return convertToResponse(entity);
    }

    @Override
    public void deleteItem(String itemId) {
        ItemEntity entity = repository.findByItemId(itemId).orElseThrow(()->new RuntimeException("Item not found with id : "+ itemId));
        boolean isFileDelete = fileUploadService.deleteFile(entity.getImgUrl());
        if (isFileDelete){
            repository.delete(entity);
        }else{
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to delete item with id : "+ itemId);
        }
    }

    @Override
    public List<ItemResponse> fetchItems() {
        return repository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    private ItemEntity convertToEntity(ItemRequest request,String imgUrl){
        CategoryEntity entity = categoryRepository.findByCategoryId(request.getCategoryId())
                .orElseThrow(()->new UsernameNotFoundException("Category not found with id :"+ request.getCategoryId()));
          return ItemEntity.builder()
                  .itemId(UUID.randomUUID().toString())
                  .name(request.getName())
                  .price(request.getPrice())
                  .description(request.getDescription())
                  .category(entity)
                  .imgUrl(imgUrl).build();

    }
    private ItemResponse convertToResponse(ItemEntity entity){
        return ItemResponse.builder()
                .itemId(entity.getItemId())
                .name(entity.getName())
                .price(entity.getPrice())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .imgUrl(entity.getImgUrl())
                .categoryId(entity.getCategory().getCategoryId())
                .categoryName(entity.getCategory().getName()).build();

    }
}
