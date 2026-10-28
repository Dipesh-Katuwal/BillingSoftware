package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.UserRequest;
import in.dipesh.spring_backend.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRequest request);
    String getUserRole(String email);
    List<UserResponse> readUsers();
    void deleteUser(String id);
}
