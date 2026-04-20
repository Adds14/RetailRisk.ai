package com.retailrisk.factory;

import com.retailrisk.model.HighTierPolicy;
import com.retailrisk.model.LowTierPolicy;
import com.retailrisk.model.MarketingPolicy;
import com.retailrisk.model.ModerateTierPolicy;
import com.retailrisk.model.PredictionResult;
import org.springframework.stereotype.Component;

/**
 * PolicyFactory — Polymorphism
 * =============================
 * Factory class that creates the correct {@link MarketingPolicy} subclass
 * based on the likelihood tier inside a {@link PredictionResult}.
 *
 * OOP Principle: POLYMORPHISM
 *   The factory returns the abstract type {@code MarketingPolicy}. The caller
 *   (RetailRiskService / RetailRiskController) never references the concrete
 *   subclass directly. When {@code policy.generateRecommendation()} is called,
 *   Java's dynamic dispatch automatically invokes the correct subclass method
 *   at runtime — this is runtime polymorphism in action.
 *
 * Design Pattern: Factory Method
 *   Centralises object creation logic, decoupling the controller/service from
 *   the concrete policy classes. Adding a new tier only requires:
 *     1. A new subclass extending MarketingPolicy.
 *     2. A new case in this factory — nowhere else.
 */
@Component
public class PolicyFactory {

    // ── Tier constants (mirrors the Python tier mapping) ──────────────────────
    private static final String TIER_HIGH     = "High";
    private static final String TIER_MODERATE = "Moderate";
    private static final String TIER_LOW      = "Low";

    /**
     * Creates and returns the appropriate {@link MarketingPolicy} subclass
     * for the given prediction result.
     *
     * The return type is the abstract base class {@code MarketingPolicy};
     * the caller is completely decoupled from the specific subclass.
     *
     * @param result the {@link PredictionResult} from the ML microservice
     * @return a concrete {@link MarketingPolicy} instance matching the tier
     * @throws IllegalArgumentException if the tier in the result is unrecognised
     */
    public MarketingPolicy createPolicy(PredictionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Cannot create a policy from a null PredictionResult.");
        }

        String tier = result.getLikelihoodTier();

        // ── Polymorphic dispatch: returns abstract type, concrete subclass chosen here ──
        return switch (tier) {
            case TIER_HIGH     -> new HighTierPolicy(result);
            case TIER_MODERATE -> new ModerateTierPolicy(result);
            case TIER_LOW      -> new LowTierPolicy(result);
            default -> throw new IllegalArgumentException(
                "Unknown likelihood tier: '" + tier + "'. Expected one of: High, Moderate, Low."
            );
        };
    }
}
