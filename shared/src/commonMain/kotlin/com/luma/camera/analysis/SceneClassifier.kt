package com.luma.camera.analysis

data class SceneLabelInput(val text: String, val confidence: Double)

data class SceneClassification(
    val intent: ShootingIntent,
    val confidence: Double,
    val matchedLabels: List<String>
)

/** Conservative scene mapping over on-device image labels. Low-confidence guesses are omitted. */
object SceneClassifier {
    private val keywords = mapOf(
        ShootingIntent.PORTRAIT to setOf("person", "people", "human", "face", "portrait", "man", "woman", "child"),
        ShootingIntent.LANDSCAPE to setOf("landscape", "nature", "mountain", "sky", "forest", "beach", "sea", "lake", "waterfall", "sunset"),
        ShootingIntent.ARCHITECTURE to setOf("building", "architecture", "house", "skyscraper", "bridge", "tower", "structure", "church", "cityscape"),
        ShootingIntent.FOOD to setOf("food", "dish", "meal", "cuisine", "fruit", "vegetable", "dessert", "pizza", "bread", "drink")
    )

    fun classify(labels: List<SceneLabelInput>, minimumConfidence: Double = 0.55): SceneClassification? {
        val normalized = labels.mapNotNull { label ->
            val text = label.text.trim().lowercase()
            val confidence = label.confidence
            if (text.isBlank() || !confidence.isFinite() || confidence !in 0.0..1.0) null
            else text to confidence
        }
        val scores = keywords.mapValues { (_, words) ->
            normalized.filter { (text, confidence) -> confidence >= 0.25 && text in words }
                .sortedByDescending { it.second }
        }
        val ranked = scores.entries.mapNotNull { (intent, matches) ->
            matches.firstOrNull()?.let { (label, confidence) -> Triple(intent, confidence, matches.map { it.first }) }
        }.sortedByDescending { it.second }
        val best = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)?.second ?: 0.0
        if (best.second < minimumConfidence.coerceIn(0.0, 1.0) || best.second - runnerUp < 0.12) return null
        return SceneClassification(best.first, best.second, best.third)
    }
}
