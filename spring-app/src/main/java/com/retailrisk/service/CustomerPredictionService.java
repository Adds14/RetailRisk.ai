package com.retailrisk.service;

import com.retailrisk.entity.CustomerPrediction;
import com.retailrisk.model.MarketingPolicy;
import com.retailrisk.model.PredictionResult;
import com.retailrisk.repository.CustomerPredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CustomerPredictionService — CRUD Business Logic
 * ==================================================
 * Provides Create, Read, Update, Delete operations on saved prediction records.
 * Converts between MarketingPolicy (domain model) and CustomerPrediction (JPA entity).
 */
@Service
public class CustomerPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerPredictionService.class);

    private final CustomerPredictionRepository repository;

    public CustomerPredictionService(CustomerPredictionRepository repository) {
        this.repository = repository;
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────

    /**
     * Saves a list of MarketingPolicy results to the database after ML prediction.
     * Each policy is converted to a CustomerPrediction entity and persisted.
     *
     * @param policies  list of MarketingPolicy objects from the ML analysis
     * @param batchId   unique identifier for this CSV upload batch
     * @param fileName  original CSV file name
     * @return list of saved entities
     */
    @Transactional
    public List<CustomerPrediction> saveAll(List<MarketingPolicy> policies, String batchId, String fileName) {
        logger.info("Saving {} predictions to database (batchId: {})", policies.size(), batchId);

        List<CustomerPrediction> entities = new ArrayList<>();
        for (MarketingPolicy policy : policies) {
            PredictionResult pr = policy.getPredictionResult();

            CustomerPrediction entity = new CustomerPrediction();
            // Non-ML fields (customer info from CSV)
            entity.setName(pr.getName());
            entity.setAge(pr.getAge());
            entity.setJob(pr.getJob());
            entity.setMarital(pr.getMarital());
            entity.setEducation(pr.getEducation());
            entity.setDefaultCredit(pr.getDefaultCredit());
            entity.setBalance(pr.getBalance());
            entity.setHousing(pr.getHousing());
            entity.setLoan(pr.getLoan());
            entity.setContact(pr.getContact());
            entity.setDay(pr.getDay());
            entity.setMonth(pr.getMonth());
            entity.setDuration(pr.getDuration());
            entity.setCampaign(pr.getCampaign());
            entity.setPdays(pr.getPdays());
            entity.setPrevious(pr.getPrevious());
            entity.setPoutcome(pr.getPoutcome());
            // ML-generated fields
            entity.setProbability(pr.getProbability());
            entity.setFinalProbPercent(pr.getFinalProbPercent());
            entity.setTier(pr.getTier());
            entity.setTierClass(pr.getTierClass());
            entity.setLikelihoodTier(pr.getLikelihoodTier());
            entity.setPrediction(pr.getPrediction());
            entity.setWillSubscribe(pr.isWillSubscribe());
            entity.setRecommendation(policy.generateRecommendation());
            entity.setRankPosition(pr.getRank());
            // Metadata
            entity.setBatchId(batchId);
            entity.setFileName(fileName);
            entity.setCreatedAt(LocalDateTime.now());

            entities.add(entity);
        }

        List<CustomerPrediction> saved = repository.saveAll(entities);
        logger.info("Successfully saved {} predictions.", saved.size());
        return saved;
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    /** Returns all saved predictions, most recent first. */
    public List<CustomerPrediction> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** Finds a single prediction by its database ID. */
    public Optional<CustomerPrediction> findById(Long id) {
        return repository.findById(id);
    }

    /** Returns all predictions from a specific CSV upload batch. */
    public List<CustomerPrediction> findByBatchId(String batchId) {
        return repository.findByBatchIdOrderByRankPositionAsc(batchId);
    }

    /** Returns all predictions filtered by likelihood tier. */
    public List<CustomerPrediction> findByTier(String tier) {
        return repository.findByLikelihoodTier(tier);
    }

    /** Returns all unique batch IDs with their metadata (for batch listing). */
    public List<Map<String, Object>> findAllBatches() {
        List<CustomerPrediction> all = repository.findAllByOrderByCreatedAtDesc();

        // Group by batchId, preserve insertion order
        Map<String, List<CustomerPrediction>> grouped = new LinkedHashMap<>();
        for (CustomerPrediction cp : all) {
            grouped.computeIfAbsent(cp.getBatchId(), k -> new ArrayList<>()).add(cp);
        }

        List<Map<String, Object>> batches = new ArrayList<>();
        for (Map.Entry<String, List<CustomerPrediction>> entry : grouped.entrySet()) {
            List<CustomerPrediction> batchPredictions = entry.getValue();
            CustomerPrediction first = batchPredictions.get(0);

            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("batchId", entry.getKey());
            batch.put("fileName", first.getFileName());
            batch.put("createdAt", first.getCreatedAt());
            batch.put("totalCustomers", batchPredictions.size());
            batch.put("subscribers", batchPredictions.stream()
                    .filter(CustomerPrediction::isWillSubscribe).count());
            batch.put("highCount", batchPredictions.stream()
                    .filter(p -> "High".equals(p.getLikelihoodTier())).count());
            batch.put("moderateCount", batchPredictions.stream()
                    .filter(p -> "Moderate".equals(p.getLikelihoodTier())).count());
            batch.put("lowCount", batchPredictions.stream()
                    .filter(p -> "Low".equals(p.getLikelihoodTier())).count());

            batches.add(batch);
        }
        return batches;
    }

    /** Returns total count of saved predictions. */
    public long count() {
        return repository.count();
    }

    // ── UPDATE ──────────────────────────────────────────────────────────────────

    /**
     * Updates an existing prediction record with all non-ML input fields.
     * ML-generated fields are NOT changed here — use updateWithMlResults() after re-prediction.
     *
     * @param id      the database ID of the prediction to update
     * @param updated the updated entity fields
     * @return the saved entity
     * @throws NoSuchElementException if no prediction with this ID exists
     */
    @Transactional
    public CustomerPrediction update(Long id, CustomerPrediction updated) {
        CustomerPrediction existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prediction not found with ID: " + id));

        // Update ALL non-ML fields (customer info from CSV)
        existing.setName(updated.getName());
        existing.setAge(updated.getAge());
        existing.setJob(updated.getJob());
        existing.setMarital(updated.getMarital());
        existing.setEducation(updated.getEducation());
        existing.setDefaultCredit(updated.getDefaultCredit());
        existing.setBalance(updated.getBalance());
        existing.setHousing(updated.getHousing());
        existing.setLoan(updated.getLoan());
        existing.setContact(updated.getContact());
        existing.setDay(updated.getDay());
        existing.setMonth(updated.getMonth());
        existing.setDuration(updated.getDuration());
        existing.setCampaign(updated.getCampaign());
        existing.setPdays(updated.getPdays());
        existing.setPrevious(updated.getPrevious());
        existing.setPoutcome(updated.getPoutcome());

        logger.info("Updated non-ML fields for prediction ID {} (name: '{}')", id, existing.getName());
        return repository.save(existing);
    }

    /**
     * Updates ML-generated fields on an existing prediction after re-running the model.
     *
     * @param id             the database ID
     * @param newPrediction  the fresh PredictionResult from Flask ML service
     * @return the updated entity
     */
    @Transactional
    public CustomerPrediction updateWithMlResults(Long id, PredictionResult newPrediction) {
        CustomerPrediction existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prediction not found with ID: " + id));

        existing.setProbability(newPrediction.getProbability());
        existing.setFinalProbPercent(newPrediction.getFinalProbPercent());
        existing.setTier(newPrediction.getTier());
        existing.setTierClass(newPrediction.getTierClass());
        existing.setLikelihoodTier(newPrediction.getLikelihoodTier());
        existing.setPrediction(newPrediction.getPrediction());
        existing.setWillSubscribe(newPrediction.isWillSubscribe());
        existing.setRecommendation(newPrediction.getRecommendation());

        logger.info("Updated ML results for prediction ID {} — new tier: '{}', prob: {}%",
                    id, existing.getTier(), existing.getFinalProbPercent());
        return repository.save(existing);
    }

    /**
     * Recalculates rank positions for all predictions in a batch.
     * Sorts by finalProbPercent descending and assigns rank 1, 2, 3...
     * Should be called after any ML re-prediction to keep rankings accurate.
     *
     * @param batchId the batch to recalculate
     */
    @Transactional
    public void recalculateRanks(String batchId) {
        List<CustomerPrediction> batch = repository.findByBatchIdOrderByRankPositionAsc(batchId);
        if (batch.isEmpty()) return;

        // Sort by probability descending (highest probability = rank 1)
        batch.sort((a, b) -> Double.compare(b.getFinalProbPercent(), a.getFinalProbPercent()));

        for (int i = 0; i < batch.size(); i++) {
            batch.get(i).setRankPosition(i + 1);
        }

        repository.saveAll(batch);
        logger.info("Recalculated ranks for batch '{}' ({} predictions)", batchId, batch.size());
    }

    // ── DELETE ──────────────────────────────────────────────────────────────────

    /**
     * Deletes a single prediction by its ID.
     *
     * @param id the database ID
     * @throws NoSuchElementException if no prediction with this ID exists
     */
    @Transactional
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Prediction not found with ID: " + id);
        }
        repository.deleteById(id);
        logger.info("Deleted prediction ID {}", id);
    }

    /**
     * Deletes all predictions from a specific batch.
     *
     * @param batchId the batch identifier
     */
    @Transactional
    public void deleteByBatchId(String batchId) {
        long count = repository.countByBatchId(batchId);
        repository.deleteByBatchId(batchId);
        logger.info("Deleted {} predictions from batch '{}'", count, batchId);
    }

    /**
     * Deletes all saved predictions from the database.
     */
    @Transactional
    public void deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        logger.info("Deleted all {} predictions from database.", count);
    }
}
