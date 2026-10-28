package in.dipesh.spring_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import in.dipesh.spring_backend.entity.UserEntity;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUserId(String id);
}
