package com.retailrisk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CustomerPrediction — JPA Entity
 * =================================
 * Maps each ML prediction result to a row in the CUSTOMER_PREDICTIONS table.
 * Stores all fields returned by the Flask ML microservice plus metadata
 * (batch ID, timestamps) for grouping and auditing.
 *
 * OOP Principle: ENCAPSULATION
 *   All fields are private with controlled access via getters/setters.
 *   The @Entity annotation lets Hibernate manage the database mapping
 *   without exposing SQL details to the rest of the application.
 */
@Entity
@Table(name = "customer_predictions")
public class CustomerPrediction {

    // ── Primary Key ────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Customer Info ──────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String name;

    private int age;

    private String job;

    // ── CSV Input Fields (editable, used for re-prediction) ───────────────────
    private String marital;
    private String education;

    @Column(name = "default_credit")
    private String defaultCredit;

    private int balance;
    private String housing;
    private String loan;
    private String contact;
    private int day;
    private String month;
    private int duration;
    private int campaign;
    private int pdays;
    private int previous;
    private String poutcome;

    // ── ML Prediction Fields (read-only, set by ML service) ──────────────────
    private double probability;

    @Column(name = "final_prob_percent")
    private double finalProbPercent;

    private String tier;

    @Column(name = "tier_class")
    private String tierClass;

    @Column(name = "likelihood_tier")
    private String likelihoodTier;

    private String prediction;

    @Column(name = "will_subscribe")
    private boolean willSubscribe;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String recommendation;

    @Column(name = "rank_position")
    private int rankPosition;

    // ── Batch & Metadata ──────────────────────────────────────────────────────
    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────
    public CustomerPrediction() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Pre-persist hook ──────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public Long getId()                 { return id; }
    public String getName()             { return name; }
    public int getAge()                 { return age; }
    public String getJob()              { return job; }
    public String getMarital()          { return marital; }
    public String getEducation()        { return education; }
    public String getDefaultCredit()    { return defaultCredit; }
    public int getBalance()             { return balance; }
    public String getHousing()          { return housing; }
    public String getLoan()             { return loan; }
    public String getContact()          { return contact; }
    public int getDay()                 { return day; }
    public String getMonth()            { return month; }
    public int getDuration()            { return duration; }
    public int getCampaign()            { return campaign; }
    public int getPdays()               { return pdays; }
    public int getPrevious()            { return previous; }
    public String getPoutcome()         { return poutcome; }
    public double getProbability()      { return probability; }
    public double getFinalProbPercent() { return finalProbPercent; }
    public String getTier()             { return tier; }
    public String getTierClass()        { return tierClass; }
    public String getLikelihoodTier()   { return likelihoodTier; }
    public String getPrediction()       { return prediction; }
    public boolean isWillSubscribe()    { return willSubscribe; }
    public String getRecommendation()   { return recommendation; }
    public int getRankPosition()        { return rankPosition; }
    public String getBatchId()          { return batchId; }
    public String getFileName()         { return fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(Long id)                          { this.id = id; }
    public void setName(String name)                    { this.name = name; }
    public void setAge(int age)                         { this.age = age; }
    public void setJob(String job)                      { this.job = job; }
    public void setMarital(String marital)              { this.marital = marital; }
    public void setEducation(String education)          { this.education = education; }
    public void setDefaultCredit(String defaultCredit)  { this.defaultCredit = defaultCredit; }
    public void setBalance(int balance)                 { this.balance = balance; }
    public void setHousing(String housing)              { this.housing = housing; }
    public void setLoan(String loan)                    { this.loan = loan; }
    public void setContact(String contact)              { this.contact = contact; }
    public void setDay(int day)                         { this.day = day; }
    public void setMonth(String month)                  { this.month = month; }
    public void setDuration(int duration)               { this.duration = duration; }
    public void setCampaign(int campaign)               { this.campaign = campaign; }
    public void setPdays(int pdays)                     { this.pdays = pdays; }
    public void setPrevious(int previous)               { this.previous = previous; }
    public void setPoutcome(String poutcome)            { this.poutcome = poutcome; }
    public void setProbability(double probability)      { this.probability = probability; }
    public void setFinalProbPercent(double v)            { this.finalProbPercent = v; }
    public void setTier(String tier)                    { this.tier = tier; }
    public void setTierClass(String tierClass)          { this.tierClass = tierClass; }
    public void setLikelihoodTier(String likelihoodTier){ this.likelihoodTier = likelihoodTier; }
    public void setPrediction(String prediction)        { this.prediction = prediction; }
    public void setWillSubscribe(boolean willSubscribe) { this.willSubscribe = willSubscribe; }
    public void setRecommendation(String recommendation){ this.recommendation = recommendation; }
    public void setRankPosition(int rankPosition)       { this.rankPosition = rankPosition; }
    public void setBatchId(String batchId)              { this.batchId = batchId; }
    public void setFileName(String fileName)            { this.fileName = fileName; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    /** Formatted percentage string for display, e.g. "78.20%" */
    public String getProbabilityAsPercent() {
        return String.format("%.2f%%", finalProbPercent > 0 ? finalProbPercent : probability * 100.0);
    }

    /**
     * Builds a CSV row string from the input fields for re-prediction.
     * Format matches the required CSV columns for the Flask ML service.
     */
    public String toCsvRow() {
        return String.join(";",
            String.valueOf(age), job, marital, education, defaultCredit,
            String.valueOf(balance), housing, loan, contact,
            String.valueOf(day), month, String.valueOf(duration),
            String.valueOf(campaign), String.valueOf(pdays),
            String.valueOf(previous), poutcome
        );
    }

    /** CSV header for re-prediction. */
    public static String csvHeader() {
        return "name;age;job;marital;education;default;balance;housing;loan;contact;day;month;duration;campaign;pdays;previous;poutcome";
    }

    /** Full CSV row including name. */
    public String toCsvRowWithName() {
        return name + ";" + toCsvRow();
    }

    @Override
    public String toString() {
        return "CustomerPrediction{id=" + id + ", name='" + name + "', tier='" + tier +
               "', prob=" + finalProbPercent + "%, batch='" + batchId + "'}";
    }
}
