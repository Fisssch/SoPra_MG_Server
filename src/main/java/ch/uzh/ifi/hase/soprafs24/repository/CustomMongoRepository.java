package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import ch.uzh.ifi.hase.soprafs24.entity.DatabaseEntity;

@NoRepositoryBean
public interface CustomMongoRepository<T extends DatabaseEntity> extends  MongoRepository<T, Long> {
    /** This method is called when we want to insert a new element into the database.
    * It automatically assigns a new id to the element.
    * If you want to insert a new element with a specific id, use save instead.
    */
    @Override
    <S extends T> S insert(S entity);
    @Override
    <S extends T> S save(S entity);
}
