# Zero-Trust Privacy - Local Protocols

This skill enforces the privacy constraints of the on-device medical agent.

## Core Directives
1. **Local-Only:** No network calls during inference.
2. **Zero-Persistence:** Photos/sensitive data should be deleted after processing (optional unless requested).
3. **Transparency:** Logcat or UI should confirm "Processing Locally...".
4. **Permissions:** Limit permissions to Camera and Files only.

## Data Isolation
- No usage of external image APIs (e.g., Cloud Vision).
- Use `LlmInference` Task from MediaPipe locally.
- OCR text stays in memory only.

## Phase 6 Security Enhancements
1. **Security Shield Dashboard**: Users can audit the local inference state at any time via the Top Bar shield icon.
2. **Session Orientation**: Mandatory on-device initialization headers in clinical sessions to reassure users.
3. **Editorial Calm UI**: Visual feedback via `MedicalPrivacyBadge` maintains persistent trust throughout diagnostic workflows.
