package com.retailrisk.model;

/**
 * LowTierPolicy — Inheritance + Polymorphism
 * =============================================
 * Concrete marketing policy for customers with LOW subscription likelihood
 * (probability < 0.35). These customers are unlikely to convert in the short
 * term; the strategy focuses on low-cost awareness and long-term relationship.
 *
 * OOP Principle: INHERITANCE
 *   Extends MarketingPolicy, inheriting shared state and utility methods,
 *   while delivering a cost-efficient, awareness-only recommendation.
 */
public class LowTierPolicy extends MarketingPolicy {

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs a LowTierPolicy with the given prediction result.
     *
     * @param predictionResult the ML prediction output (probability < 0.35)
     */
    public LowTierPolicy(PredictionResult predictionResult) {
        super(predictionResult);  // calls MarketingPolicy(predictionResult)
    }

    // ── Abstract method implementations ────────────────────────────────────────

    /**
     * Generates a low-cost, awareness-driven marketing recommendation for
     * low-likelihood term deposit prospects.
     *
     * Strategy rationale:
     *   - Aggressively targeting low-probability customers wastes campaign budget
     *     and risks triggering opt-outs (damaging the long-term relationship).
     *   - Brand awareness via low-cost channels (in-app banners, push notifications)
     *     keeps the bank top-of-mind without heavy cost.
     *   - Life-event triggers (salary increment, loan repayment completion) can
     *     cause rapid re-tier movement — maintain passive monitoring.
     *   - Focus resources on High and Moderate tiers for maximum ROI.
     *
     * @return actionable recommendation for the digital marketing / CRM team
     */
    @Override
    public String generateRecommendation() {
        return "LOW-COST AWARENESS — Low Conversion Probability: "
             + predictionResult.getProbabilityAsPercent() + "\n\n"
             + "1. DO NOT COLD CALL: Avoid direct agent outreach for this customer "
             + "at this time. The cost-per-acquisition would exceed the product margin.\n\n"
             + "2. PASSIVE DIGITAL TOUCHPOINTS: Serve informational banner ads "
             + "within the mobile banking app and internet banking portal. "
             + "Use soft CTAs: 'Learn about Term Deposits' rather than 'Apply Now'.\n\n"
             + "3. QUARTERLY NEWSLETTER: Include a term deposit spotlight in the "
             + "regular monthly e-statement or product newsletter.\n\n"
             + "4. LIFE-EVENT TRIGGERS: Set up CRM alerts to re-evaluate this "
             + "customer if any of these occur: salary increase > 15%, "
             + "existing loan fully repaid, FD maturity in another bank, or "
             + "balance > ₹2,00,000 for 30 consecutive days.\n\n"
             + "5. RE-SCORING: Schedule next propensity model run in 90 days. "
             + "Track if customer profile shifts to Moderate or High tier.";
    }

    /** {@inheritDoc} */
    @Override
    public String getTierDisplayName() {
        return "Low Likelihood";
    }

    /** {@inheritDoc} */
    @Override
    public String getTierCssClass() {
        return "tier-low";
    }
}
