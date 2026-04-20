package com.retailrisk.repository;

import com.retailrisk.entity.CustomerPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CustomerPredictionRepository — Data Access Layer
 * ==================================================
 * Spring Data JPA repository for {@link CustomerPrediction} entities.
 *
 * Spring auto-generates all CRUD SQL at runtime — no boilerplate needed.
 * Custom query methods follow the Spring Data naming convention.
 */
@Repository
public interface CustomerPredictionRepository extends JpaRepository<CustomerPrediction, Long> {

    /** Find all predictions from a specific CSV upload batch, ordered by rank. */
    List<CustomerPrediction> findByBatchIdOrderByRankPositionAsc(String batchId);

    /** Find all predictions ordered by most recent first. */
    List<CustomerPrediction> findAllByOrderByCreatedAtDesc();

    /** Find predictions by likelihood tier (High / Moderate / Low). */
    List<CustomerPrediction> findByLikelihoodTier(String likelihoodTier);

    /** Delete all predictions from a specific batch. */
    void deleteByBatchId(String batchId);

    /** Count predictions in a batch. */
    long countByBatchId(String batchId);
}
