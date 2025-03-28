package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import ch.uzh.ifi.hase.soprafs24.entity.DatabaseEntity;

public class CustomMongoRepositoryImpl<T extends DatabaseEntity> extends SimpleMongoRepository<T, Long> implements CustomMongoRepository<T> {

    public CustomMongoRepositoryImpl(MongoEntityInformation<T, Long> metadata, MongoOperations mongoOperations) {
        super(metadata, mongoOperations);
    }

    @Override
    public <S extends T> S insert(S entity) {
        // If we insert we do want to make sure that below logic correctly assigns a new id, 
        entity.setId(null);
        return save(entity);
    }

    @Override
    public <S extends T> S save(S entity) {
        // If the id was not instantiated, set it to the next available id, 
        // If it was instantiated it is an update and we do not need to set a new id
        if (entity.getId() == null || entity.getId() <= 0) {
            Long id = getMaxId();
            entity.setId(id);
        }
        return super.save(entity);
    }
    
    private Long getMaxId() {
        var orderedFields = super.findAll(Sort.by(Sort.Direction.DESC, "_id"));
        if (orderedFields == null || orderedFields.isEmpty())
            return 1L;
        Long id = orderedFields.get(0).getId();
        return id + 1;
    }
}
