package com.example.demo.Repository;

import com.example.demo.DBEntity.SchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<SchemaEntity, Integer> {
    
    Optional<SchemaEntity> findBySchemaId(Integer schemaId);
    
    @Query("SELECT s FROM SchemaEntity s ORDER BY s.schemaId")
    List<SchemaEntity> findAllOrderedById();
    
    boolean existsBySchemaId(Integer schemaId);
    
    @Query("SELECT MAX(s.schemaId) FROM SchemaEntity s")
    Optional<Integer> findMaxSchemaId();
}