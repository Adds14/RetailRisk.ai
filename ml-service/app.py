"""
retailrisk.ai — Machine Learning Microservice (Final)
======================================================
Flask REST API that accepts a CSV file upload, runs the full preprocessing
pipeline (identical to the training notebook), and returns JSON predictions
for every row — ranked by probability descending.

POST /predict
  Form fields:
    csv_file   : the uploaded .csv file
    model_path : (optional) path to the .pkl — defaults to model_artifacts_optimal.pkl

Response JSON:
  {
    "results": [ { rank, name, age, job, duration, lr_prob, xgb_prob,
                   final_prob, prediction, will_subscribe, tier, tier_class,
                   likelihoodTier, recommendation } ],
    "meta":    { threshold, w_lr, w_xgb, total, subscribers }
  }
"""

import io, os, re, logging, warnings
import joblib, numpy as np, pandas as pd
warnings.filterwarnings("ignore")

from flask import Flask, jsonify, render_template, request

logging.basicConfig(level=logging.INFO, format="%(asctime)s  [%(levelname)s]  %(message)s")
logger = logging.getLogger(__name__)

# sklearn version compatibility patch
from sklearn.linear_model import LogisticRegression as _LR
if not hasattr(_LR, "multi_class"):
    _LR.multi_class = "auto"

app = Flask(__name__)
app.secret_key = "retailrisk_secret_2024"
app.config["MAX_CONTENT_LENGTH"] = 32 * 1024 * 1024

# ── Constants ──────────────────────────────────────────────────────────────────
VALID_JOBS      = ["admin.","blue-collar","entrepreneur","housemaid","management",
                   "retired","self-employed","services","student","technician","unemployed","unknown"]
VALID_MARITAL   = ["divorced","married","single"]
VALID_EDUCATION = ["primary","secondary","tertiary","unknown"]
VALID_CONTACT   = ["cellular","telephone","unknown"]
VALID_MONTHS    = ["jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"]
VALID_POUTCOME  = ["failure","other","success","unknown"]
REQUIRED_COLS   = ["age","job","marital","education","default","balance","housing","loan",
                   "contact","day","month","duration","campaign","pdays","previous","poutcome"]
CAT_COLS = {
    "job":          VALID_JOBS,
    "marital":      VALID_MARITAL,
    "education":    VALID_EDUCATION,
    "contact":      VALID_CONTACT,
    "month":        VALID_MONTHS,
    "age_group":    ["Young","Adult","Senior","Old"],
    "pdays_bucket": ["recent","warm","cold","never"],
}

RECOMMENDATIONS = {
    "Very Likely": (
        "IMMEDIATE PRIORITY — Probability >= 80%\n\n"
        "1. Assign a dedicated Relationship Manager within 24 hours.\n"
        "2. Offer a premium term deposit rate (0.50% above standard) with flexible tenures.\n"
        "3. Send a pre-filled digital application link via SMS and email.\n"
        "4. Offer exclusive welcome bonus for deposits above Rs.1,00,000.\n"
        "5. Escalate to senior banker if no response within 48 hours."
    ),
    "Likely": (
        "HIGH PRIORITY — Probability 60-79%\n\n"
        "1. Contact within 48 hours via preferred channel.\n"
        "2. Present personalised term deposit package with competitive rates.\n"
        "3. Highlight capital safety and guaranteed returns.\n"
        "4. Offer a limited-time rate booster valid for 7 days.\n"
        "5. Follow up once if no response within 3 days."
    ),
    "Moderate": (
        "NURTURE CAMPAIGN — Probability 40-59%\n\n"
        "1. Enrol in a 2-week email/SMS drip campaign.\n"
        "2. Share ROI calculator comparing term deposit vs savings account.\n"
        "3. Invite to a Smart Savings webinar or branch financial planning session.\n"
        "4. Present limited-time offer with 7-day expiry to create urgency.\n"
        "5. Re-score after 2 weeks — escalate if probability improves."
    ),
    "Unlikely": (
        "LOW-COST AWARENESS — Probability 20-39%\n\n"
        "1. Avoid direct agent calls — cost-per-acquisition exceeds margin.\n"
        "2. Serve soft informational banners inside mobile/internet banking app.\n"
        "3. Include term deposit spotlight in monthly e-statement newsletter.\n"
        "4. Set CRM alert for life-event triggers (salary rise, loan repayment, high balance).\n"
        "5. Re-score in 60 days."
    ),
    "Very Unlikely": (
        "PASSIVE MONITORING — Probability < 20%\n\n"
        "1. Do not contact. No outreach recommended at this time.\n"
        "2. Maintain passive brand presence via in-app notifications only.\n"
        "3. Monitor account activity for trigger events.\n"
        "4. Re-score in 90 days.\n"
        "5. Focus campaign budget on higher-tier customers."
    ),
}

TIER_TO_3TIER = {
    "Very Likely": "High", "Likely": "High",
    "Moderate": "Moderate",
    "Unlikely": "Low", "Very Unlikely": "Low",
}

# ── Model loading ──────────────────────────────────────────────────────────────
_model_cache = {}

def load_model(path="model_artifacts_optimal.pkl"):
    if path in _model_cache:
        return _model_cache[path], None
    if not os.path.exists(path):
        return None, f"Model file not found: '{path}'. Place model_artifacts_optimal.pkl next to app.py."
    try:
        ma = joblib.load(path)
        required_keys = ["lr_model","xgb_calibrated","scaler","pt","train_columns","best_w_lr","best_w_xgb","best_thresh"]
        missing = [k for k in required_keys if k not in ma]
        if missing:
            return None, f"Model artifact missing keys: {missing}"
        lr = ma["lr_model"]
        if not hasattr(lr, "multi_class"):
            lr.multi_class = "auto"
        _model_cache[path] = ma
        logger.info("Model loaded | Columns: %d | Threshold: %.4f", len(ma["train_columns"]), ma["best_thresh"])
        return ma, None
    except Exception as exc:
        return None, f"Failed to load model: {exc}"

# ── CSV parsing ────────────────────────────────────────────────────────────────
def parse_csv(file_bytes):
    try:
        raw = file_bytes.decode("utf-8", errors="replace")
        fixed = []
        for line in raw.strip().splitlines():
            line = line.strip()
            if line.startswith('"') and line.endswith('"'):
                line = line[1:-1]
            line = re.sub(r'""([^";]*)""', r"\1", line)
            line = line.replace('"', "")
            fixed.append(line)
        df = None
        for sep in [";", ","]:
            try:
                candidate = pd.read_csv(io.StringIO("\n".join(fixed)), sep=sep)
                candidate.columns = candidate.columns.str.strip()
                if all(c in candidate.columns for c in REQUIRED_COLS[:3]):
                    df = candidate
                    break
            except Exception:
                continue
        if df is None:
            return None, "Could not parse CSV. Ensure it uses comma or semicolon separators."
        missing = [c for c in REQUIRED_COLS if c not in df.columns]
        if missing:
            return None, f"CSV is missing required columns: {missing}"
        if "name" not in df.columns:
            df["name"] = [f"Customer {i+1}" for i in range(len(df))]
        return df[["name"] + REQUIRED_COLS].to_dict(orient="records"), None
    except Exception as exc:
        return None, f"CSV parse error: {exc}"

# ── Feature engineering ────────────────────────────────────────────────────────
def _manual_dummies(df, col, all_categories):
    for cat in sorted(all_categories)[1:]:
        df[f"{col}_{cat}"] = (df[col] == cat).astype(int)
    df.drop(columns=[col], inplace=True)
    return df

def preprocess(raw_list, ma):
    pt, scaler, train_columns = ma["pt"], ma["scaler"], ma["train_columns"]
    df = pd.DataFrame(raw_list).drop(columns=["name","y"], errors="ignore")
    for col in ["default","housing","loan"]:
        df[col] = df[col].astype(str).str.strip().str.lower().map({"yes":1,"no":0})
    df["previous_contacted"] = df["poutcome"].apply(lambda x: 0 if x=="unknown" else 1)
    df["previous_success"]   = df["poutcome"].apply(lambda x: 1 if x=="success" else 0)
    df["age_group"] = pd.cut(df["age"], bins=[18,30,45,60,100], labels=["Young","Adult","Senior","Old"]).astype(str)
    df.drop("poutcome", axis=1, inplace=True)
    df[["balance","duration"]] = pt.transform(df[["balance","duration"]])
    df["total_contacts"]         = df["campaign"] + df["previous"]
    df["contact_pressure"]       = df["campaign"] / (df["previous"] + 1)
    df["high_contact_flag"]      = (df["campaign"] > 3).astype(int)
    df["never_contacted_before"] = (df["pdays"] == -1).astype(int)
    df["recent_contact"]         = df["pdays"].apply(lambda x: 1 if 0 <= x <= 30 else 0)
    df["pdays_bucket"] = pd.cut(df["pdays"].replace(-1,999), bins=[-1,30,90,180,999],
                                labels=["recent","warm","cold","never"]).astype(str)
    df["prev_success"]      = (df["previous_success"] == 1).astype(int)
    df["prev_contact_flag"] = (df["previous"] > 0).astype(int)
    df["engagement_score"]  = df["previous"] * df["prev_success"]
    for col, categories in CAT_COLS.items():
        df = _manual_dummies(df, col, categories)
    for col in train_columns:
        if col not in df.columns:
            df[col] = 0
    df = df[train_columns].astype(float)
    return df, scaler.transform(df)

# ── Tier & prediction ──────────────────────────────────────────────────────────
def _tier(p):
    if p >= 0.80: return "Very Likely",   "tier-very-likely"
    if p >= 0.60: return "Likely",        "tier-likely"
    if p >= 0.43: return "Moderate",      "tier-moderate"
    if p >= 0.20: return "Unlikely",      "tier-unlikely"
    return               "Very Unlikely", "tier-very-unlikely"

def run_predictions(raw_list, ma):
    w_lr, w_xgb, thresh = ma["best_w_lr"], ma["best_w_xgb"], ma["best_thresh"]
    X_raw, X_scaled = preprocess(raw_list, ma)
    lr_prob   = ma["lr_model"].predict_proba(X_scaled)[:, 1]
    xgb_prob  = ma["xgb_calibrated"].predict_proba(X_raw.values)[:, 1]
    fin_prob  = w_lr * lr_prob + w_xgb * xgb_prob
    fin_pred  = (fin_prob >= thresh).astype(int)
    results = []
    for i, row in enumerate(raw_list):
        tl, tc = _tier(fin_prob[i])
        results.append({
            "rank": i+1, "name": row["name"], "age": row["age"],
            "job": row["job"], "duration": row["duration"],
            "marital": row.get("marital", ""), "education": row.get("education", ""),
            "default": row.get("default", ""), "balance": row.get("balance", 0),
            "housing": row.get("housing", ""), "loan": row.get("loan", ""),
            "contact": row.get("contact", ""), "day": row.get("day", 0),
            "month": row.get("month", ""), "campaign": row.get("campaign", 0),
            "pdays": row.get("pdays", 0), "previous": row.get("previous", 0),
            "poutcome": row.get("poutcome", ""),
            "final_prob": round(float(fin_prob[i])*100, 1),
            "probability": round(float(fin_prob[i]), 6),
            "prediction": "Subscribe" if fin_pred[i]==1 else "No Subscribe",
            "will_subscribe": bool(fin_pred[i]==1),
            "tier": tl, "tier_class": tc,
            "likelihoodTier": TIER_TO_3TIER[tl],
            "recommendation": RECOMMENDATIONS[tl],
        })
    results.sort(key=lambda x: x["final_prob"], reverse=True)
    for i, r in enumerate(results): r["rank"] = i+1
    meta = {"threshold": float(thresh),
            "total": len(results), "subscribers": sum(1 for r in results if r["will_subscribe"])}
    return results, meta

# ── Routes ─────────────────────────────────────────────────────────────────────
@app.route("/health")
def health():
    return jsonify({"status": "ok", "service": "retailrisk-ml"}), 200

@app.route("/")
def index():
    return "ML Service is Running", 200

@app.route("/predict", methods=["POST"])
def predict():
    model_path = request.form.get("model_path", "model_artifacts_optimal.pkl").strip() or "model_artifacts_optimal.pkl"
    ma, err = load_model(model_path)
    if err:
        return jsonify({"error": err}), 400
    if "csv_file" not in request.files:
        return jsonify({"error": "No file uploaded. Send CSV as 'csv_file' in form-data."}), 400
    f = request.files["csv_file"]
    if f.filename == "":
        return jsonify({"error": "No file selected."}), 400
    if not f.filename.lower().endswith(".csv"):
        return jsonify({"error": "Only .csv files are accepted."}), 400
    raw_list, err = parse_csv(f.read())
    if err:
        return jsonify({"error": err}), 422
    if not raw_list:
        return jsonify({"error": "CSV is empty or could not be read."}), 422
    logger.info("Parsed %d row(s) from uploaded CSV.", len(raw_list))
    try:
        results, meta = run_predictions(raw_list, ma)
    except Exception as exc:
        logger.exception("Prediction failed")
        return jsonify({"error": f"Prediction failed: {exc}"}), 500
    logger.info("Done. Subscribers: %d / %d", meta["subscribers"], meta["total"])
    return jsonify({"results": results, "meta": meta}), 200

if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=5000, debug=False)
