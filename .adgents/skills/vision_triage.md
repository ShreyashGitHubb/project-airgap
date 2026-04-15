# Vision Triage Skill - Medical Agent

This skill defines the logic for on-device medical vision analysis using Gemma-3n.

## Analysis Modes
### 1. Dermatology (Skin)
- **Goal:** Identify rashes, moles, or lesions.
- **Protocol:**
  - Request clear lighting and centered focus.
  - Analyze border, color, and size (ABCDE criteria for moles).
  - Output: "Observations", "Potential Hypotheses", "Suggested Next Steps (Consult professional)".

### 2. Ophthalmology (Eyes)
- **Goal:** Detect redness, clouded lenses (cataracts), or iris issues.
- **Protocol:**
  - Focus on iris clarity and sclera color.
  - Watch for conjunctivitis or trauma signals.
  - Output: "Visual Findings", "Alert Level (High/Low)", "Professional Recommendation".

### 3. Lab Report Deciphering
- **Goal:** Simplify complex blood panels or radiology text images.
- **Protocol:**
  - OCR extraction (via local model).
  - Explain metrics (e.g., HbA1c, LDL) in plain language.
  - Contrast with "Normal Range" provided in the report.
  - Output: "Metric Explanation", "Summary of Results", "Key Questions for your Doctor".

## Privacy Constraint
- **CRITICAL:** ALL processing must be local. No OCR data or images must be sent to cloud APIs.
