/*
 * Copyright 2026 Google LLC
 */

package com.google.ai.edge.gallery.ui.common.chat

/**
 * Helper to parse medical structured markers from LLM output.
 */
object MedicalDataParser {
    private const val REPORT_BEGIN = "--- BEGIN MEDICAL REPORT ---"
    private const val REPORT_END = "--- END MEDICAL REPORT ---"
    private const val HANDOUT_BEGIN = "--- BEGIN PHYSICIAN HANDOUT ---"
    private const val HANDOUT_END = "--- END PHYSICIAN HANDOUT ---"

    fun parseMedicalReport(text: String): ChatMessageMedicalReport? {
        val start = text.indexOf(REPORT_BEGIN)
        val end = text.indexOf(REPORT_END)
        if (start == -1 || end == -1 || end <= start) return null

        val content = text.substring(start + REPORT_BEGIN.length, end).trim()
        val lines = content.split("\n")
            .map { it.trim() }
            .filter { it.startsWith("|") && it.count { c -> c == '|' } >= 4 }

        if (lines.size < 2) return null // Need at least header and one row

        // Find the index of the divider line (e.g., |---|---|...)
        val metrics = mutableListOf<MedicalMetric>()
        lines.forEach { line ->
            if (line.contains("---")) return@forEach
            if (line.contains("Metric") && line.contains("Value")) return@forEach
            
            val parts = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 4) {
                metrics.add(MedicalMetric(
                    name = parts[0],
                    value = parts[1],
                    referenceRange = parts[2],
                    status = parts[3]
                ))
            }
        }
        
        if (metrics.isEmpty()) return null
        
        return ChatMessageMedicalReport(reportTitle = "Laboratory Analysis Result", metrics = metrics)
    }

    fun parsePhysicianHandout(text: String): ChatMessagePhysicianHandout? {
        val start = text.indexOf(HANDOUT_BEGIN)
        val end = text.indexOf(HANDOUT_END)
        if (start == -1 || end == -1 || end <= start) return null

        val content = text.substring(start + HANDOUT_BEGIN.length, end).trim()
        val points = content.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.removePrefix("-").trim() }

        if (points.isEmpty()) return null

        return ChatMessagePhysicianHandout(title = "Physician Discussion Points", points = points)
    }
}
