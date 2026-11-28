package com.luma.camera.analysis

data class SceneLabelInput(val text: String, val confidence: Double)

data class SceneClassification(
    val intent: ShootingIntent,
    val confidence: Double,
    val matchedLabels: List<String>
)

/**
 * Conservative scene mapping over on-device image labels.
 *
 * ML Kit can describe the same subject with several nearby labels (for example,
 * "plate", "cuisine" and "food"). We combine distinct matches per scene instead
 * of letting one noisy top label decide the mode. Ambiguous scenes are left alone.
 */
object SceneClassifier {
    private val keywords = mapOf(
        ShootingIntent.PORTRAIT to setOf(
            "person", "people", "human", "face", "portrait", "man", "woman",
            "child", "boy", "girl", "selfie", "crowd", "fashion model"
        ),
        ShootingIntent.LANDSCAPE to setOf(
            "landscape", "nature", "mountain", "sky", "forest", "woodland",
            "beach", "sea", "ocean", "lake", "waterfall", "sunset", "sunrise",
            "river", "valley", "coast", "desert", "field", "garden", "tree",
            "flower", "plant"
        ),
        ShootingIntent.ARCHITECTURE to setOf(
            "building", "architecture", "house", "skyscraper", "bridge",
            "tower", "structure", "church", "cityscape", "urban", "facade",
            "monument", "temple", "castle", "interior", "room"
        ),
        ShootingIntent.FOOD to setOf(
            "food", "dish", "meal", "cuisine", "fruit", "vegetable", "dessert",
            "pizza", "bread", "drink", "beverage", "plate", "restaurant",
            "cooking", "baking", "sushi", "coffee", "cake", "pastry"
        )
    )

    fun classify(labels: List<SceneLabelInput>, minimumConfidence: Double = 0.55): SceneClassification? {
        val normalized = labels.mapNotNull { label ->
            val text = label.text.trim().lowercase().replace('_', ' ').replace('-', ' ')
                .replace(Regex("\\s+"), " ")
            val confidence = label.confidence
            if (text.isBlank() || !confidence.isFinite() || confidence !in 0.0..1.0) null
            else text to confidence
        }
        val minimum = minimumConfidence.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.55
        val ranked = keywords.mapNotNull { (intent, words) ->
            val matches = normalized
                .filter { (text, confidence) ->
                    confidence >= 0.25 && words.any { word ->
                        text == word || text.split(' ').any { token -> token == word } ||
                            (word.contains(' ') && text.contains(word))
                    }
                }
                .distinctBy { it.first }
                .sortedByDescending { it.second }
            val strongest = matches.firstOrNull()?.second ?: return@mapNotNull null
            // A second independent label modestly reinforces a scene without overpowering confidence.
            val support = matches.drop(1).take(2).sumOf { it.second } * 0.08
            Triple(intent, (strongest + support).coerceAtMost(1.0), matches.map { it.first })
        }.sortedByDescending { it.second }

        val best = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)?.second ?: 0.0
        if (best.second < minimum || best.second - runnerUp < 0.12) return null
        return SceneClassification(best.first, best.second, best.third)
    }
}
