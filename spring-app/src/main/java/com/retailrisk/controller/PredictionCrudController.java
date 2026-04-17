package com.retailrisk.controller;

import com.retailrisk.entity.CustomerPrediction;
import com.retailrisk.model.PredictionResult;
import com.retailrisk.service.CustomerPredictionService;
import com.retailrisk.service.RetailRiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * PredictionCrudController — CRUD Web Endpoints
 * ================================================
 * Provides full Create, Read, Update, Delete operations on saved
 * customer prediction records via Thymeleaf-rendered pages.
 *
 * Routes:
 *   GET  /predictions                     → List all saved batches
 *   GET  /predictions/batch/{batchId}     → View predictions from one batch
 *   GET  /predictions/{id}                → View single prediction detail
 *   GET  /predictions/{id}/edit           → Show edit form
 *   POST /predictions/{id}/edit           → Submit edit (UPDATE)
 *   POST /predictions/{id}/delete         → Delete single prediction (DELETE)
 *   POST /predictions/batch/{batchId}/delete → Delete entire batch (DELETE)
 *   POST /predictions/delete-all          → Delete all predictions (DELETE)
 */
@Controller
@RequestMapping("/predictions")
public class PredictionCrudController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionCrudController.class);

    private final CustomerPredictionService predictionService;
    private final RetailRiskService retailRiskService;

    public PredictionCrudController(CustomerPredictionService predictionService,
                                    RetailRiskService retailRiskService) {
        this.predictionService = predictionService;
        this.retailRiskService = retailRiskService;
    }

    // ── READ: List all batches ─────────────────────────────────────────────────

    /** GET /predictions — shows all saved prediction batches. */
    @GetMapping
    public String listAll(Model model) {
        List<Map<String, Object>> batches = predictionService.findAllBatches();
        long totalPredictions = predictionService.count();

        model.addAttribute("batches", batches);
        model.addAttribute("totalPredictions", totalPredictions);
        model.addAttribute("savedCount", totalPredictions);
        return "predictions";
    }

    // ── READ: View a specific batch ────────────────────────────────────────────

    /** GET /predictions/batch/{batchId} — shows all predictions from one CSV upload. */
    @GetMapping("/batch/{batchId}")
    public String viewBatch(@PathVariable String batchId, Model model) {
        List<CustomerPrediction> predictions = predictionService.findByBatchId(batchId);

        if (predictions.isEmpty()) {
            model.addAttribute("errorMessage", "No predictions found for batch: " + batchId);
            return "redirect:/predictions";
        }

        // Stats
        long subscribers   = predictions.stream().filter(CustomerPrediction::isWillSubscribe).count();
        long highCount     = predictions.stream().filter(p -> "High".equals(p.getLikelihoodTier())).count();
        long moderateCount = predictions.stream().filter(p -> "Moderate".equals(p.getLikelihoodTier())).count();
        long lowCount      = predictions.stream().filter(p -> "Low".equals(p.getLikelihoodTier())).count();

        model.addAttribute("predictions",    predictions);
        model.addAttribute("batchId",        batchId);
        model.addAttribute("fileName",       predictions.get(0).getFileName());
        model.addAttribute("totalCustomers", predictions.size());
        model.addAttribute("subscribers",    subscribers);
        model.addAttribute("highCount",      highCount);
        model.addAttribute("moderateCount",  moderateCount);
        model.addAttribute("lowCount",       lowCount);
        model.addAttribute("savedCount",     predictionService.count());

        return "prediction-batch";
    }

    // ── READ: View single prediction ───────────────────────────────────────────

    /** GET /predictions/{id} — shows full detail for one prediction. */
    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<CustomerPrediction> opt = predictionService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Prediction not found with ID: " + id);
            return "redirect:/predictions";
        }

        model.addAttribute("prediction", opt.get());
        model.addAttribute("savedCount", predictionService.count());
        return "prediction-detail";
    }

    // ── UPDATE: Show edit form ─────────────────────────────────────────────────

    /** GET /predictions/{id}/edit — shows the edit form for a prediction. */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<CustomerPrediction> opt = predictionService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Prediction not found with ID: " + id);
            return "redirect:/predictions";
        }

        model.addAttribute("prediction", opt.get());
        model.addAttribute("savedCount", predictionService.count());
        return "prediction-edit";
    }

    // ── UPDATE: Submit edit ────────────────────────────────────────────────────

    /** POST /predictions/{id}/edit — updates non-ML fields, then re-runs the ML model. */
    @PostMapping("/{id}/edit")
    public String submitEdit(@PathVariable Long id,
                             @ModelAttribute CustomerPrediction updated,
                             RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Save all non-ML field changes to the database
            CustomerPrediction saved = predictionService.update(id, updated);
            logger.info("Updated non-ML fields for prediction ID {}", id);

            // Step 2: Re-run ML model with the updated input fields
            try {
                PredictionResult newResult = retailRiskService.rePredictSingle(saved);
                predictionService.updateWithMlResults(id, newResult);
                predictionService.recalculateRanks(saved.getBatchId());
                redirectAttributes.addFlashAttribute("successMessage",
                    "Prediction updated and ML model re-run successfully. New tier: " +
                    newResult.getTier() + " (" + newResult.getFinalProbPercent() + "%)");
            } catch (Exception mlError) {
                // Non-ML fields were saved, but re-prediction failed
                logger.warn("Re-prediction failed for ID {}: {}", id, mlError.getMessage());
                redirectAttributes.addFlashAttribute("successMessage",
                    "Customer data updated, but ML re-prediction failed: " + mlError.getMessage() +
                    ". ML fields retain their previous values.");
            }
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/predictions/" + id;
    }

    // ── DELETE: Single prediction ──────────────────────────────────────────────

    /** POST /predictions/{id}/delete — deletes a single prediction. */
    @PostMapping("/{id}/delete")
    public String deleteSingle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            predictionService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Prediction deleted successfully.");
            logger.info("Deleted prediction ID {}", id);
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/predictions";
    }

    // ── DELETE: Entire batch ───────────────────────────────────────────────────

    /** POST /predictions/batch/{batchId}/delete — deletes all predictions in a batch. */
    @PostMapping("/batch/{batchId}/delete")
    public String deleteBatch(@PathVariable String batchId, RedirectAttributes redirectAttributes) {
        predictionService.deleteByBatchId(batchId);
        redirectAttributes.addFlashAttribute("successMessage", "Batch '" + batchId + "' deleted successfully.");
        logger.info("Deleted batch '{}'", batchId);
        return "redirect:/predictions";
    }

    // ── DELETE: All predictions ────────────────────────────────────────────────

    /** POST /predictions/delete-all — deletes everything. */
    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        predictionService.deleteAll();
        redirectAttributes.addFlashAttribute("successMessage", "All predictions deleted.");
        logger.info("Deleted all predictions.");
        return "redirect:/predictions";
    }
}
