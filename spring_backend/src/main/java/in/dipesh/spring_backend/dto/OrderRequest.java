package in.dipesh.spring_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotBlank
    private String customerName;
    @NotBlank
    private String phoneNumber;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
}
