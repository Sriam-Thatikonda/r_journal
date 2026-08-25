package com.baverika.r_journal.utils

import java.security.SecureRandom
import java.util.Locale

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

    private val secureRandom = SecureRandom()

    /** Securely pick a random element from a list. */
    private fun <T> List<T>.secureRandom(): T = this[secureRandom.nextInt(this.size)]

    /**
     * Generates a passphrase using format: CapitalizedAdjective + CapitalizedNounOrAbstract + Number
     * @param numberLength The number of digits for the suffix (2 to 6).
     */
    fun generate(numberLength: Int = 4): String {
        val adj = ADJECTIVES.secureRandom().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // 50/50 chance for Noun or Abstract
        val useNoun = secureRandom.nextBoolean()
        val secondWordRaw = if (useNoun) NOUNS.secureRandom() else ABSTRACTS.secureRandom()
        val secondWord = secondWordRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Generate number based on length (2..6)
        // 2: 10..99
        // 6: 100000..999999
        val safeLength = numberLength.coerceIn(2, 6)
        val min = Math.pow(10.0, (safeLength - 1).toDouble()).toInt()
        val max = Math.pow(10.0, safeLength.toDouble()).toInt() - 1
        
        val number = min + secureRandom.nextInt(max - min + 1)
        
        val specialChar = SPECIAL_CHARS.secureRandom()

        return "$adj$secondWord$specialChar$number"
    }

    /**
     * Generates a numeric PIN of specified length.
     * @param length The number of digits (4 to 16).
     */
    fun generatePin(length: Int): String {
        return (1..length)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
    }
}
