package com.retailrisk.model;

/**
 * HighTierPolicy — Inheritance + Polymorphism
 * =============================================
 * Concrete marketing policy for customers with HIGH subscription likelihood
 * (probability >= 0.60). These customers show strong intent signals and
 * should receive immediate, premium engagement.
 *
 * OOP Principle: INHERITANCE
 *   Extends MarketingPolicy, inheriting predictionResult, getBaseDetails(),
 *   and getPredictionResult() — and provides specific implementations for
 *   the three abstract methods declared in the parent.
 */
public class HighTierPolicy extends MarketingPolicy {

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs a HighTierPolicy with the given prediction result.
     *
     * @param predictionResult the ML prediction output (probability >= 0.60)
     */
    public HighTierPolicy(PredictionResult predictionResult) {
        super(predictionResult);  // calls MarketingPolicy(predictionResult)
    }

    // ── Abstract method implementations ────────────────────────────────────────

    /**
     * Generates a premium, priority-action marketing recommendation for
     * high-likelihood term deposit subscribers.
     *
     * Strategy rationale:
     *   - This segment has the highest conversion rate (Likely + Very Likely
     *     tiers capture the most actual subscribers per the notebook's analysis).
     *   - Personalised outreach with premium incentives closes deals fastest.
     *   - Direct relationship manager involvement adds trust for large deposits.
     *
     * @return actionable recommendation string for the sales/marketing team
     */
    @Override
    public String generateRecommendation() {
        return "PRIORITY ACTION — High Conversion Probability: "
             + predictionResult.getProbabilityAsPercent() + "\n\n"
             + "1. IMMEDIATE OUTREACH: Assign a dedicated Relationship Manager "
             + "to contact this customer within 24 hours via their preferred channel.\n\n"
             + "2. PERSONALISED OFFER: Present a tailored term deposit package with "
             + "a competitive interest rate (offer 0.25–0.50% above standard rates) "
             + "and flexible lock-in periods (3, 6, or 12 months).\n\n"
             + "3. PREMIUM INCENTIVES: Offer a welcome bonus for deposits above "
             + "₹1,00,000 — e.g., complimentary locker facility or loyalty reward points.\n\n"
             + "4. DIGITAL FAST-TRACK: Send a pre-filled digital application link "
             + "via email and SMS. Reduce friction with one-click confirmation.\n\n"
             + "5. FOLLOW-UP: Schedule a callback within 48 hours if no response. "
             + "Escalate to senior banker if the first contact does not convert.";
    }

    /** {@inheritDoc} */
    @Override
    public String getTierDisplayName() {
        return "High Likelihood";
    }

    /** {@inheritDoc} */
    @Override
    public String getTierCssClass() {
        return "tier-high";
    }
}
