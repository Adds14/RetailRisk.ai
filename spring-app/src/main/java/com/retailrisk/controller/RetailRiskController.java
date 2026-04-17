package com.retailrisk.controller;

import com.retailrisk.model.MarketingPolicy;
import com.retailrisk.service.CustomerPredictionService;
import com.retailrisk.service.RetailRiskService;
import com.retailrisk.service.RetailRiskService.RetailRiskServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * RetailRiskController
 * Handles CSV file upload, delegates to RetailRiskService,
 * and passes the list of polymorphic MarketingPolicy results to Thymeleaf.
 * Now also persists results to the database via CustomerPredictionService.
 */
@Controller
public class RetailRiskController {

    private static final Logger logger = LoggerFactory.getLogger(RetailRiskController.class);

    private final RetailRiskService retailRiskService;
    private final CustomerPredictionService predictionService;

    public RetailRiskController(RetailRiskService retailRiskService,
                                CustomerPredictionService predictionService) {
        this.retailRiskService = retailRiskService;
        this.predictionService = predictionService;
    }

    /** GET / — render the upload form */
    @GetMapping("/")
    public String showForm(Model model) {
        // Pass saved predictions count for the nav badge
        model.addAttribute("savedCount", predictionService.count());
        return "index";
    }

    /**
     * POST /analyse — receives CSV file upload, runs predictions,
     * saves results to database, injects results list into Thymeleaf model.
     */
    @PostMapping("/analyse")
    public String analyseCustomers(
            @RequestParam("csvFile") MultipartFile csvFile,
            Model model) {

        logger.info("Received /analyse POST — file: '{}', size: {} bytes",
                    csvFile.getOriginalFilename(), csvFile.getSize());

        // ── Validation ─────────────────────────────────────────────────────────
        if (csvFile.isEmpty()) {
            model.addAttribute("errorMessage", "Please upload a CSV file before submitting.");
            return "index";
        }
        String filename = csvFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            model.addAttribute("errorMessage", "Only .csv files are accepted.");
            return "index";
        }

        try {
            // ── Get list of policies (one per customer row) ────────────────────
            List<MarketingPolicy> policies = retailRiskService.analyseCustomers(csvFile);

            if (policies.isEmpty()) {
                model.addAttribute("errorMessage", "No results returned. Check your CSV has data rows.");
                return "index";
            }

            // ── Save to database (CREATE operation) ────────────────────────────
            String batchId = UUID.randomUUID().toString().substring(0, 8);
            predictionService.saveAll(policies, batchId, filename);
            logger.info("Saved batch '{}' with {} predictions to database.", batchId, policies.size());

            // ── Summary stats ──────────────────────────────────────────────────
            long subscribers   = policies.stream().filter(p -> p.getPredictionResult().isWillSubscribe()).count();
            long highCount     = policies.stream().filter(p -> "High".equals(p.getPredictionResult().getLikelihoodTier())).count();
            long moderateCount = policies.stream().filter(p -> "Moderate".equals(p.getPredictionResult().getLikelihoodTier())).count();
            long lowCount      = policies.stream().filter(p -> "Low".equals(p.getPredictionResult().getLikelihoodTier())).count();

            // ── Polymorphism: generateRecommendation() called on abstract type ─
            // Each policy in the list resolves to the correct subclass at runtime.
            model.addAttribute("policies",       policies);
            model.addAttribute("totalCustomers", policies.size());
            model.addAttribute("subscribers",    subscribers);
            model.addAttribute("highCount",      highCount);
            model.addAttribute("moderateCount",  moderateCount);
            model.addAttribute("lowCount",       lowCount);
            model.addAttribute("fileName",       filename);
            model.addAttribute("batchId",        batchId);
            model.addAttribute("savedCount",     predictionService.count());

            logger.info("Analysis complete — {} customers, {} predicted subscribers", policies.size(), subscribers);

        } catch (RetailRiskServiceException e) {
            logger.error("Service error: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please check your CSV and try again.");
        }

        return "index";
    }
}
