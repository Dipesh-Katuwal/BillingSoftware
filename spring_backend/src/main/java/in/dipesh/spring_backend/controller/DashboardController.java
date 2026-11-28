package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.DashboardResponse;
import in.dipesh.spring_backend.service.Impl.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public DashboardResponse summary() {
        return dashboardService.getSummary();
    }
}
