package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.ItemRequest;
import in.dipesh.spring_backend.dto.ItemResponse;
import in.dipesh.spring_backend.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/admin/addItem")
    public ResponseEntity<ItemResponse> addItem(@RequestPart("item") ItemRequest request, @RequestPart(value = "file", required = false)MultipartFile file){
        ItemResponse response = itemService.addItem(request,file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/admin/item/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id){
        itemService.deleteItem(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getItems(){
        List<ItemResponse> allItems = itemService.fetchItems();
        return ResponseEntity.ok().body(allItems);
    }
}
