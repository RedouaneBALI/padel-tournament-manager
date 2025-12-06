package io.github.redouanebali.repository;

import io.github.redouanebali.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

}



