package one.ampadu.dsv.repository;


import one.ampadu.dsv.entity.LLMModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AIModelRepository extends JpaRepository<LLMModel, Long> {

    @Query("""
        SELECT m FROM LLMModel m
        WHERE (m.blocked IS NULL OR m.blocked < :now)
        ORDER BY m.id ASC
        LIMIT 1
    """)
    Optional<LLMModel> findNextAvailable(
            @Param("now") java.time.LocalDateTime now
    );

    @Query("""
        SELECT m FROM LLMModel m
        WHERE m.provider = :provider
        AND (m.blocked IS NULL OR m.blocked < :now)
        ORDER BY m.id ASC
        LIMIT 1
    """)
    Optional<LLMModel> findNextAvailableByProvider(
            @Param("provider") String provider,
            @Param("now") java.time.LocalDateTime now
    );
}