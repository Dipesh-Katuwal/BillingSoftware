package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.CategoryRequest;
import in.dipesh.spring_backend.dto.CategoryResponse;
import in.dipesh.spring_backend.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@AllArgsConstructor
public class CategoryController {
    CategoryService service;

    @PostMapping("/addcategory")
    public ResponseEntity<CategoryResponse> addCategory(@RequestBody CategoryRequest request){
         CategoryResponse res = service.addCategory(request);
         return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
