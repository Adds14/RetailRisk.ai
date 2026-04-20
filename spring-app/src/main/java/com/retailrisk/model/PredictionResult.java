package com.retailrisk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * PredictionResult — Encapsulation
 * Maps the full JSON response from the Python Flask ML microservice.
 * Contains both the batch results list and the meta summary object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionResult {

    // ── Encapsulated fields ────────────────────────────────────────────────────
    @JsonProperty("probability")    private double  probability;
    @JsonProperty("likelihoodTier") private String  likelihoodTier;
    @JsonProperty("will_subscribe") private boolean willSubscribe;
    @JsonProperty("tier")           private String  tier;
    @JsonProperty("tier_class")     private String  tierClass;
    @JsonProperty("recommendation") private String  recommendation;
    @JsonProperty("final_prob")     private double  finalProbPercent;
    @JsonProperty("lr_prob")        private double  lrProbPercent;
    @JsonProperty("xgb_prob")       private double  xgbProbPercent;
    @JsonProperty("name")           private String  name;
    @JsonProperty("age")            private int     age;
    @JsonProperty("job")            private String  job;
    @JsonProperty("rank")           private int     rank;
    @JsonProperty("prediction")     private String  prediction;

    // ── CSV input fields (for re-prediction) ──────────────────────────────────
    @JsonProperty("marital")        private String  marital;
    @JsonProperty("education")      private String  education;
    @JsonProperty("default")        private String  defaultCredit;
    @JsonProperty("balance")        private int     balance;
    @JsonProperty("housing")        private String  housing;
    @JsonProperty("loan")           private String  loan;
    @JsonProperty("contact")        private String  contact;
    @JsonProperty("day")            private int     day;
    @JsonProperty("month")          private String  month;
    @JsonProperty("duration")       private int     duration;
    @JsonProperty("campaign")       private int     campaign;
    @JsonProperty("pdays")          private int     pdays;
    @JsonProperty("previous")       private int     previous;
    @JsonProperty("poutcome")       private String  poutcome;

    // ── Constructors ───────────────────────────────────────────────────────────
    public PredictionResult() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    public double  getProbability()       { return probability; }
    public String  getLikelihoodTier()    { return likelihoodTier; }
    public boolean isWillSubscribe()      { return willSubscribe; }
    public String  getTier()             { return tier; }
    public String  getTierClass()        { return tierClass; }
    public String  getRecommendation()   { return recommendation; }
    public double  getFinalProbPercent() { return finalProbPercent; }
    public double  getLrProbPercent()    { return lrProbPercent; }
    public double  getXgbProbPercent()   { return xgbProbPercent; }
    public String  getName()             { return name; }
    public int     getAge()              { return age; }
    public String  getJob()             { return job; }
    public int     getRank()             { return rank; }
    public String  getPrediction()       { return prediction; }
    public String  getMarital()          { return marital; }
    public String  getEducation()        { return education; }
    public String  getDefaultCredit()    { return defaultCredit; }
    public int     getBalance()          { return balance; }
    public String  getHousing()          { return housing; }
    public String  getLoan()             { return loan; }
    public String  getContact()          { return contact; }
    public int     getDay()              { return day; }
    public String  getMonth()            { return month; }
    public int     getDuration()         { return duration; }
    public int     getCampaign()         { return campaign; }
    public int     getPdays()            { return pdays; }
    public int     getPrevious()         { return previous; }
    public String  getPoutcome()         { return poutcome; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setProbability(double v)       { this.probability = v; }
    public void setLikelihoodTier(String v)    { this.likelihoodTier = v; }
    public void setWillSubscribe(boolean v)    { this.willSubscribe = v; }
    public void setTier(String v)             { this.tier = v; }
    public void setTierClass(String v)        { this.tierClass = v; }
    public void setRecommendation(String v)   { this.recommendation = v; }
    public void setFinalProbPercent(double v) { this.finalProbPercent = v; }
    public void setLrProbPercent(double v)    { this.lrProbPercent = v; }
    public void setXgbProbPercent(double v)   { this.xgbProbPercent = v; }
    public void setName(String v)             { this.name = v; }
    public void setAge(int v)                 { this.age = v; }
    public void setJob(String v)             { this.job = v; }
    public void setRank(int v)               { this.rank = v; }
    public void setPrediction(String v)       { this.prediction = v; }
    public void setMarital(String v)          { this.marital = v; }
    public void setEducation(String v)        { this.education = v; }
    public void setDefaultCredit(String v)    { this.defaultCredit = v; }
    public void setBalance(int v)             { this.balance = v; }
    public void setHousing(String v)          { this.housing = v; }
    public void setLoan(String v)             { this.loan = v; }
    public void setContact(String v)          { this.contact = v; }
    public void setDay(int v)                 { this.day = v; }
    public void setMonth(String v)            { this.month = v; }
    public void setDuration(int v)            { this.duration = v; }
    public void setCampaign(int v)            { this.campaign = v; }
    public void setPdays(int v)               { this.pdays = v; }
    public void setPrevious(int v)            { this.previous = v; }
    public void setPoutcome(String v)         { this.poutcome = v; }

    /** Formatted percentage string for display, e.g. "78.20%" */
    public String getProbabilityAsPercent() {
        return String.format("%.2f%%", finalProbPercent > 0 ? finalProbPercent : probability * 100.0);
    }

    @Override
    public String toString() {
        return "PredictionResult{name='" + name + "', tier='" + tier +
               "', finalProb=" + finalProbPercent + "%, willSubscribe=" + willSubscribe + "}";
    }
}
