package com.retailrisk.model;

/**
 * MarketingPolicy — Abstraction
 * ==============================
 * Abstract base class representing a marketing policy for term deposit campaigns.
 *
 * OOP Principle: ABSTRACTION
 *   This class hides the complexity of how each tier policy generates its
 *   recommendations behind a clean interface. Callers interact only with
 *   the abstract contract (generateRecommendation), not with specific subclass logic.
 *
 *   Concrete details are deferred to the three subclasses:
 *     - HighTierPolicy
 *     - ModerateTierPolicy
 *     - LowTierPolicy
 */
public abstract class MarketingPolicy {

    // ── Protected shared state ─────────────────────────────────────────────────
    // Accessible by all subclasses but hidden from external code.

    /** The ML prediction result that informs this policy. */
    protected final PredictionResult predictionResult;

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Initialises the policy with the ML prediction result.
     * Subclasses must call super(predictionResult) in their own constructors.
     *
     * @param predictionResult the result from the Python ML microservice
     */
    protected MarketingPolicy(PredictionResult predictionResult) {
        if (predictionResult == null) {
            throw new IllegalArgumentException("PredictionResult must not be null.");
        }
        this.predictionResult = predictionResult;
    }

    // ── Shared utility (concrete method) ──────────────────────────────────────

    /**
     * Returns a base summary string shared by all policies.
     * Demonstrates that abstract classes CAN provide concrete shared behaviour.
     *
     * @return summary of probability and tier
     */
    public String getBaseDetails() {
        return String.format(
            "Customer Probability: %s | Tier: %s | Predicted to Subscribe: %s",
            predictionResult.getProbabilityAsPercent(),
            predictionResult.getLikelihoodTier(),
            predictionResult.isWillSubscribe() ? "Yes" : "No"
        );
    }

    /**
     * Accessor for the encapsulated PredictionResult, available to subclasses
     * and to the Spring Boot layer for passing data to templates.
     *
     * @return the PredictionResult associated with this policy
     */
    public PredictionResult getPredictionResult() {
        return predictionResult;
    }

    // ── Abstract contract ──────────────────────────────────────────────────────

    /**
     * Generates a specific, actionable marketing recommendation for this tier.
     *
     * Each subclass MUST provide its own implementation tailored to the
     * subscription likelihood of the customer segment it represents.
     *
     * @return a detailed, human-readable marketing recommendation string
     */
    public abstract String generateRecommendation();

    /**
     * Returns the display-friendly name of this policy tier.
     * Subclasses override this to return their specific tier label.
     *
     * @return tier name, e.g., "High Likelihood"
     */
    public abstract String getTierDisplayName();

    /**
     * Returns the CSS colour class associated with this tier for UI rendering.
     * Subclasses override this to drive colour-coded badges in Thymeleaf.
     *
     * @return CSS class string, e.g., "tier-high", "tier-moderate", "tier-low"
     */
    public abstract String getTierCssClass();
}
