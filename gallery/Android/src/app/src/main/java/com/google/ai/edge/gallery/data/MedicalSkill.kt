/*
 * Copyright 2026 Google LLC
 */

package com.google.ai.edge.gallery.data

/**
 * Represents a specialized medical agent skill.
 */
data class MedicalSkill(
  val id: String,
  val name: String,
  val description: String,
  val systemPrompt: String,
  val iconRes: Int? = null,
  val promptPrefix: String = ""
)

object MedicalSkillIds {
  const val GENERAL_ASSISTANT = "medical_general"
  const val SYMPTOM_CHECKER = "medical_symptom_checker"
  const val LAB_ANALYST = "medical_lab_analyst"
  const val JARGON_TRANSLATOR = "medical_jargon_translator"
}
