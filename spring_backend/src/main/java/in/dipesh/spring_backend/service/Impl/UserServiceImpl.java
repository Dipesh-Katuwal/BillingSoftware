package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.dto.UserRequest;
import in.dipesh.spring_backend.dto.UserResponse;
import in.dipesh.spring_backend.entity.UserEntity;
import in.dipesh.spring_backend.repository.UserRepository;
import in.dipesh.spring_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserRequest request) {
        UserEntity newUser = convertToEntity(request);
        repository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public String getUserRole(String email) {
        UserEntity existingUser = repository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("No user found with email : "+email));
        return existingUser.getRole();
    }

    @Override
    public List<UserResponse> readUsers() {
        return repository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(String id) {
        UserEntity existingUser = repository.findByUserId(id)
                .orElseThrow(()->new UsernameNotFoundException("User Not Founf with id : "+id));
        repository.delete(existingUser);
    }

    private UserEntity convertToEntity(UserRequest request) {
        return UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().toUpperCase())
                .name(request.getName()).build();
    }

    private UserResponse convertToResponse(UserEntity entity){
        return UserResponse.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .role(entity.getRole())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt()).build();
    }
}
