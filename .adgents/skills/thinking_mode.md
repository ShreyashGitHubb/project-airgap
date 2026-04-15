# Thinking Mode - Medical Trust

Thinking mode is core to building trust in an offline medical agent.

## Logic Flow (Clinical Reasoning)
1. **Extraction:** "Identify key medical identifiers (e.g., age, symptom duration)."
2. **Observation:** "Objectively describe findings (e.g., 'Erythema with irregular edges')."
3. **Hypothesis:** "Provide potential scenarios based on on-device data."
4. **Validation:** "Identify missing data points (e.g., 'Does it itch?')."
5. **Conclusion:** "Synthesize a structured summary for the user to take to a doctor."

## Display Rules
- Streaming: Thinking results should stream live to the UI.
- Label: Thinking blocks labeled "Clinical Logic Process".
- Disclaimer: Always include a note that this is an AI tool, not a doctor.
