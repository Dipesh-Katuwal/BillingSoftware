package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.ItemRequest;
import in.dipesh.spring_backend.dto.ItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ItemService {
    ItemResponse addItem(ItemRequest request, MultipartFile file);
    void deleteItem(String itemId);
    List<ItemResponse> fetchItems();
}
