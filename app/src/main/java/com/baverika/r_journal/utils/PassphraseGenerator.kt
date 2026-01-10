package com.baverika.r_journal.utils

import java.util.Locale
import kotlin.random.Random

object PassphraseGenerator {

    private val ADJECTIVES = listOf(
        "silent", "midnight", "hidden", "calm", "gentle", "wandering", "frozen", "amber", "quiet", "velvet",
        "soft", "steady", "lonely", "subtle", "hollow", "distant", "fading", "lucid", "mellow", "ancient",
        "still", "shadowed", "dim", "brave", "neutral", "faint", "echoing", "weightless", "deep", "muted",
        "bright", "cool", "clear", "smooth", "sharp", "slow", "swift", "warm", "fresh", "pure", "bold",
        "neat", "glow", "dry", "tender", "solid", "open", "softened", "quieted", "lined"
    )

    private val NOUNS = listOf(
        "moon", "journal", "river", "forest", "stone", "cloud", "signal", "shadow", "ember", "path",
        "light", "voice", "dream", "mirror", "wind", "field", "lake", "star", "mountain", "circle",
        "leaf", "window", "thread", "bridge", "spark", "harbor", "dust", "road", "flame", "horizon",
        "wave", "shore", "trail", "grove", "peak", "cave", "plain", "meadow", "cliff", "bay", "reef",
        "valley", "spring", "branch", "root", "bloom", "grain", "shell", "fog", "rain"
    )

    private val ABSTRACTS = listOf(
        "memory", "echo", "orbit", "signal", "cipher", "fragment", "pattern", "node", "vector", "stream",
        "logic", "pulse", "static", "flux", "layer", "matrix", "module", "buffer", "kernel", "archive",
        "index", "protocol", "schema", "thread", "compile", "render", "cache", "packet", "token", "entropy",
        "engine", "system", "frame", "grid", "flow", "state", "input", "output", "stack", "queue", "route",
        "cycle", "trace", "signalize", "parse", "map", "drive", "link", "scale", "model"
    )
    
    private val SPECIAL_CHARS = listOf('#', '@', '&', '$')

    /**
     * Generates a passphrase using format: CapitalizedAdjective + CapitalizedNounOrAbstract + Number
     * @param numberLength The number of digits for the suffix (2 to 6).
     */
    fun generate(numberLength: Int = 4): String {
        val adj = ADJECTIVES.random().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // 50/50 chance for Noun or Abstract
        val useNoun = Random.nextBoolean()
        val secondWordRaw = if (useNoun) NOUNS.random() else ABSTRACTS.random()
        val secondWord = secondWordRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Generate number based on length (2..6)
        // 2: 10..99
        // 6: 100000..999999
        val safeLength = numberLength.coerceIn(2, 6)
        val min = Math.pow(10.0, (safeLength - 1).toDouble()).toInt()
        val max = Math.pow(10.0, safeLength.toDouble()).toInt() - 1
        
        // Random.nextInt(min, max) excludes max, so max + 1
        val number = Random.nextInt(min, max + 1)
        
        val specialChar = SPECIAL_CHARS.random()

        return "$adj$secondWord$specialChar$number"
    }
}
