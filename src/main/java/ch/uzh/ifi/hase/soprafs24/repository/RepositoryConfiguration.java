package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(repositoryBaseClass = CustomMongoRepositoryImpl.class)
public class RepositoryConfiguration {
}
