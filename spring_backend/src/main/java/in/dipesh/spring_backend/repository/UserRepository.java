package in.dipesh.spring_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import in.dipesh.spring_backend.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    
}
