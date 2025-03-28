package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;

import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends CustomMongoRepository<User> {
  @Query("{'username': ?0}")
  User findByUsername(String username);

  @Query("{'token': ?0}")
  User findByToken(String token);
}
