package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import in.dipesh.spring_backend.entity.CategoryEntity;
import in.dipesh.spring_backend.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173/")
@RestController
@RequestMapping("/api/categories")
@AllArgsConstructor
public class CategoryController {
    CategoryService service;

    @PostMapping("/addCategory")
    public ResponseEntity<CategoryResponse> addCategory(@RequestPart("category")CategoryRequest request,@RequestPart(value="file",required = false) MultipartFile file){
         CategoryResponse res = service.addCategory(request,file);
         return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
    @GetMapping("/getCategories")
    public ResponseEntity<List<CategoryResponse>> getCategories(){
        List<CategoryResponse> categories = service.getCategories();
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }
    @DeleteMapping("/deleteCategory/{categoryId}")
    public void deleteCategory(@PathVariable String categoryId){
        try {
            service.deleteCategory(categoryId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,e.getMessage());
        }
    }
}
