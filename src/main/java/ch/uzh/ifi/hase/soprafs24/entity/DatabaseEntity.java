package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public abstract class DatabaseEntity implements Serializable {
    @Id
    @Indexed
    private Long id;

    public Long getId() {
      return id;
    }
  
    public void setId(Long id) {
      this.id = id;
    }
}
