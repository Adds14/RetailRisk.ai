package com.retailrisk.model;

/**
 * ModerateTierPolicy — Inheritance + Polymorphism
 * =================================================
 * Concrete marketing policy for customers with MODERATE subscription likelihood
 * (probability between 0.35 and 0.59). These customers are "on the fence" —
 * they need targeted nurturing to convert.
 *
 * OOP Principle: INHERITANCE
 *   Extends MarketingPolicy, inheriting shared state and utility methods,
 *   while providing a distinct nurturing-focused recommendation.
 */
public class ModerateTierPolicy extends MarketingPolicy {

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs a ModerateTierPolicy with the given prediction result.
     *
     * @param predictionResult the ML prediction output (0.35 <= probability < 0.60)
     */
    public ModerateTierPolicy(PredictionResult predictionResult) {
        super(predictionResult);  // calls MarketingPolicy(predictionResult)
    }

    // ── Abstract method implementations ────────────────────────────────────────

    /**
     * Generates a nurturing-focused marketing recommendation for moderate-tier
     * term deposit prospects.
     *
     * Strategy rationale:
     *   - This segment has meaningful but uncertain intent — over-investing in
     *     direct calls before warming them up wastes resources.
     *   - Educational content about benefits of term deposits builds trust.
     *   - A limited-time offer creates urgency without feeling pushy.
     *   - Re-scoring after campaign exposure can move them to High tier.
     *
     * @return actionable recommendation string for the marketing automation team
     */
    @Override
    public String generateRecommendation() {
        return "NURTURE CAMPAIGN — Moderate Conversion Probability: "
             + predictionResult.getProbabilityAsPercent() + "\n\n"
             + "1. DIGITAL NURTURE SEQUENCE: Enrol the customer in a 2-week "
             + "email/SMS drip campaign highlighting benefits of term deposits "
             + "(capital safety, guaranteed returns, tax advantages under Section 80C).\n\n"
             + "2. LIMITED-TIME OFFER: Present a 'Rate Booster' promotion valid "
             + "for 7 days — an additional 0.10% p.a. for first-time term deposit "
             + "customers. Mention offer expiry to create urgency.\n\n"
             + "3. COMPARISON TOOL: Share an interactive ROI calculator showing "
             + "how a ₹50,000 deposit grows versus a savings account over 12 months.\n\n"
             + "4. BRANCH EVENT INVITE: Invite to a 'Smart Savings' webinar or "
             + "in-branch financial planning session this month.\n\n"
             + "5. RE-EVALUATION: Re-run the propensity model after 2 weeks of "
             + "campaign exposure. Escalate to HighTierPolicy if score improves.";
    }
     /** {@inheritDoc} */
    @Override
    public String getTierDisplayName() {
        return "Moderate Likelihood";
    }

    /** {@inheritDoc} */
    @Override
    public String getTierCssClass() {
        return "tier-moderate";
    }
}
