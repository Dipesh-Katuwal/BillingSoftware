package in.dipesh.spring_backend.dto;


import in.dipesh.spring_backend.entity.CategoryEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {
    private String name;
    private BigDecimal price;
    private String description;
    private String categoryId;
}
