package com.retailrisk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailrisk.factory.PolicyFactory;
import com.retailrisk.model.MarketingPolicy;
import com.retailrisk.model.PredictionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * RetailRiskService
 * Sends the uploaded CSV file to the Flask ML microservice as multipart form data,
 * parses the batch JSON response, and returns a list of MarketingPolicy objects —
 * one per customer row — using the PolicyFactory (polymorphism).
 */
@Service
public class RetailRiskService {

    private static final Logger logger = LoggerFactory.getLogger(RetailRiskService.class);

    private final RestTemplate  restTemplate;
    private final PolicyFactory policyFactory;
    private final ObjectMapper  objectMapper;

    @Value("${ml.service.url:http://localhost:5000/predict}")
    private String mlServiceUrl;

    public RetailRiskService(RestTemplate restTemplate, PolicyFactory policyFactory) {
        this.restTemplate  = restTemplate;
        this.policyFactory = policyFactory;
        this.objectMapper  = new ObjectMapper();
    }

    /**
     * Sends the CSV file to the Python Flask API as multipart/form-data,
     * maps each row in the JSON response to a PredictionResult,
     * and creates a MarketingPolicy per row via the factory.
     *
     * @param csvFile  the uploaded MultipartFile from the controller
     * @return list of MarketingPolicy (one per customer, sorted by probability desc)
     */
    public List<MarketingPolicy> analyseCustomers(MultipartFile csvFile) {
        logger.info("Sending CSV '{}' ({} bytes) to ML service at {}",
                    csvFile.getOriginalFilename(), csvFile.getSize(), mlServiceUrl);
        try {
            // ── Build multipart form request ───────────────────────────────────
            byte[] csvBytes = csvFile.getBytes();
            ByteArrayResource csvResource = new ByteArrayResource(csvBytes) {
                @Override public String getFilename() {
                    return csvFile.getOriginalFilename() != null
                           ? csvFile.getOriginalFilename() : "data.csv";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("csv_file", csvResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // ── Call Flask API ─────────────────────────────────────────────────
            ResponseEntity<String> response = restTemplate.exchange(
                mlServiceUrl, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new RetailRiskServiceException("ML service returned an empty response.");
            }

            // ── Parse JSON response ────────────────────────────────────────────
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                throw new RetailRiskServiceException("ML service error: " + root.get("error").asText());
            }

            JsonNode resultsNode = root.get("results");
            if (resultsNode == null || !resultsNode.isArray()) {
                throw new RetailRiskServiceException("Unexpected ML service response format.");
            }

            // ── Map each result row to a MarketingPolicy ───────────────────────
            List<MarketingPolicy> policies = new ArrayList<>();
            for (JsonNode node : resultsNode) {
                PredictionResult pr = objectMapper.treeToValue(node, PredictionResult.class);
                // Polymorphic factory call — returns High/Moderate/LowTierPolicy
                policies.add(policyFactory.createPolicy(pr));
            }

            logger.info("Received {} predictions from ML service.", policies.size());
            return policies;

        } catch (RetailRiskServiceException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("ML service unreachable: {}", e.getMessage());
            throw new RetailRiskServiceException(
                "Cannot reach the ML prediction service. Ensure the Python Flask app is running on port 5000.");
        } catch (HttpClientErrorException e) {
            logger.error("ML service 4xx error: {}", e.getResponseBodyAsString());
            throw new RetailRiskServiceException(
                "Invalid request to ML service (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("ML service 5xx error: {}", e.getResponseBodyAsString());
            throw new RetailRiskServiceException(
                "ML service internal error. Check the Python Flask logs.");
        } catch (Exception e) {
            logger.error("Unexpected error calling ML service: {}", e.getMessage(), e);
            throw new RetailRiskServiceException("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Re-runs the ML model for a single customer prediction.
     * Builds a 1-row CSV from the entity's input fields, sends it to Flask,
     * and returns the fresh PredictionResult.
     *
     * @param entity the CustomerPrediction with updated input fields
     * @return the new PredictionResult from the ML service
     */
    public PredictionResult rePredictSingle(com.retailrisk.entity.CustomerPrediction entity) {
        logger.info("Re-predicting for customer '{}' (ID: {})", entity.getName(), entity.getId());
        try {
            // Build a 1-row CSV
            String csvContent = com.retailrisk.entity.CustomerPrediction.csvHeader() + "\n"
                              + entity.toCsvRowWithName();

            byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayResource csvResource = new ByteArrayResource(csvBytes) {
                @Override public String getFilename() { return "repredict.csv"; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("csv_file", csvResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                mlServiceUrl, HttpMethod.POST, httpEntity, String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new RetailRiskServiceException("ML service returned an empty response during re-prediction.");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                throw new RetailRiskServiceException("ML service error: " + root.get("error").asText());
            }

            JsonNode resultsNode = root.get("results");
            if (resultsNode == null || !resultsNode.isArray() || resultsNode.isEmpty()) {
                throw new RetailRiskServiceException("No results returned from ML service during re-prediction.");
            }

            PredictionResult pr = objectMapper.treeToValue(resultsNode.get(0), PredictionResult.class);
            logger.info("Re-prediction complete: tier='{}', prob={}%", pr.getTier(), pr.getFinalProbPercent());
            return pr;

        } catch (RetailRiskServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Re-prediction failed: {}", e.getMessage(), e);
            throw new RetailRiskServiceException("Re-prediction failed: " + e.getMessage());
        }
    }

    /** Unchecked exception for ML service layer failures. */
    public static class RetailRiskServiceException extends RuntimeException {
        public RetailRiskServiceException(String message) { super(message); }
    }
}
