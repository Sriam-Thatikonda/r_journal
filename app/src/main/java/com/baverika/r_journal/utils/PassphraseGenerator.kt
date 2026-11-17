package com.baverika.r_journal.utils

import java.security.SecureRandom
import java.util.Locale

object PassphraseGenerator {

    private val ADJECTIVES = listOf(
        "silent", "midnight", "hidden", "calm", "gentle", "wandering", "frozen", "amber", "quiet", "velvet",
        "soft", "steady", "lonely", "subtle", "hollow", "distant", "fading", "lucid", "mellow", "ancient",
        "still", "shadowed", "dim", "brave", "neutral", "faint", "echoing", "weightless", "deep", "muted",
        "bright", "cool", "clear", "smooth", "sharp", "slow", "swift", "warm", "fresh", "pure", "bold",
        "neat", "glow", "dry", "tender", "solid", "open", "softened", "quieted", "lined",
        "silver", "golden", "cobalt", "crimson", "emerald", "indigo", "ivory", "scarlet", "slate", "ochre",
        "ashen", "pearly", "bronze", "copper", "jade", "onyx", "pearl", "quartz", "ruby", "sapphire",
        "topaz", "turquoise", "violet", "coral", "teal", "cyan", "charcoal", "ebony", "alabaster", "russet",
        "misty", "lunar", "solar", "crisp", "breezy", "hazy", "frosty", "autumn", "winter", "summer",
        "spring", "coastal", "alpine", "mossy", "earthy", "rustic", "dusky", "dawn", "twilight", "starry",
        "cloudy", "rainy", "snowy", "stormy", "windy", "foggy", "dewy", "humid", "arid", "frigid",
        "radiant", "vivid", "stark", "shimmering", "gleaming", "glowing", "sparkling", "glossy", "matte", "satin",
        "silky", "coarse", "rough", "sleek", "polished", "weathered", "worn", "brittle", "fragile", "dense",
        "heavy", "light", "airy", "wispy", "fleeting", "transient", "eternal", "timeless", "endless", "boundless",
        "serene", "humble", "drifting", "nimble", "agile", "patient", "honest", "loyal", "fierce", "noble",
        "tranquil", "placid", "stoic", "earnest", "candid", "kind", "wise", "clever", "keen", "alert",
        "awake", "aware", "mindful", "thoughtful", "pensive", "wistful", "nostalgic", "dreamy", "poetic", "lyrical",
        "vast", "immense", "tiny", "narrow", "wide", "tall", "steep", "flat", "curved", "spiral",
        "angled", "level", "balanced", "aligned", "centered", "grounded", "rooted", "rising", "falling", "soaring",
        "gliding", "floating", "flowing", "rushing", "surging", "pulsing", "humming", "vibrant", "lively", "dynamic",
        "cryptic", "mystic", "arcane", "sacred", "secret", "obscure", "veiled", "cloaked", "masked", "bare",
        "raw", "primal", "wild", "untamed", "feral", "tame", "domestic", "urban", "rural", "remote",
        "solitary", "paired", "twin", "dual", "triple", "single", "whole", "broken", "mended", "forged",
        "gilded", "rusted", "tarnished", "etched", "carved", "woven", "braided", "knitted", "spun", "coiled",
        "twisted", "folded", "layered", "stacked", "nested", "linked", "bound", "free", "loose", "tight",
        "taut", "slack", "firm", "rigid", "flexible", "supple", "elastic", "delicate", "sturdy", "resilient"
    )

    private val NOUNS = listOf(
        "moon", "journal", "river", "forest", "stone", "cloud", "signal", "shadow", "ember", "path",
        "light", "voice", "dream", "mirror", "wind", "field", "lake", "star", "mountain", "circle",
        "leaf", "window", "thread", "bridge", "spark", "harbor", "dust", "road", "flame", "horizon",
        "wave", "shore", "trail", "grove", "peak", "cave", "plain", "meadow", "cliff", "bay", "reef",
        "valley", "spring", "branch", "root", "bloom", "grain", "shell", "fog", "rain",
        "creek", "canyon", "glacier", "island", "lagoon", "oasis", "pebble", "boulder", "summit", "ridge",
        "dune", "desert", "steppe", "tundra", "prairie", "plateau", "mesa", "gorge", "chasm", "crater",
        "volcano", "cavern", "bluff", "cape", "peninsula", "delta", "estuary", "marsh", "swamp", "bog",
        "timber", "cedar", "pine", "willow", "fern", "moss", "petal", "thorn", "acorn", "seed",
        "oak", "birch", "maple", "ash", "elm", "bamboo", "reed", "ivy", "vine", "shrub",
        "thicket", "canopy", "trunk", "bark", "twig", "bud", "blossom", "pollen", "nectar", "spore",
        "breeze", "gale", "thunder", "lightning", "mist", "frost", "snow", "dew", "puddle", "tide",
        "current", "ripple", "waterfall", "pond", "brook", "stream", "torrent", "tempest", "cyclone", "comet",
        "meteor", "asteroid", "nebula", "galaxy", "cosmos", "planet", "eclipse", "aurora", "zenith", "solstice",
        "compass", "lantern", "anchor", "beacon", "voyage", "vessel", "cabin", "shelter", "hearth", "prism",
        "torch", "candle", "lamp", "bell", "chime", "drum", "flute", "harp", "lute", "quill",
        "parchment", "scroll", "tome", "atlas", "chart", "dial", "gauge", "pendulum", "hourglass", "sundial",
        "tower", "gate", "wall", "arch", "dome", "spire", "pier", "dock", "wharf", "lighthouse",
        "windmill", "mill", "forge", "kiln", "loom", "anvil", "well", "fountain", "courtyard", "garden",
        "orchard", "terrace", "balcony", "roof", "attic", "cellar", "threshold", "corridor", "hallway", "chamber",
        "cinder", "soot", "smoke", "steam", "vapor", "ice", "crystal", "glass", "clay", "sand",
        "silt", "gravel", "soil", "loam", "peat", "coal", "flint", "slate", "granite", "marble",
        "iron", "steel", "bronze", "copper", "silver", "gold", "brass", "tin", "lead", "zinc",
        "feather", "wing", "nest", "burrow", "den", "hive", "web", "cocoon", "chrysalis", "pearl",
        "coral", "kelp", "driftwood", "foam", "spray", "drizzle", "hail", "sleet", "rainbow", "mirage",
        "sanctuary", "refuge", "haven", "outpost", "landmark", "milestone", "signpost", "crossroads", "waypoint", "cairn"
    )

    private val ABSTRACTS = listOf(
        "memory", "echo", "orbit", "signal", "cipher", "fragment", "pattern", "node", "vector", "stream",
        "logic", "pulse", "static", "flux", "layer", "matrix", "module", "buffer", "kernel", "archive",
        "index", "protocol", "schema", "thread", "compile", "render", "cache", "packet", "token", "entropy",
        "engine", "system", "frame", "grid", "flow", "state", "input", "output", "stack", "queue", "route",
        "cycle", "trace", "signalize", "parse", "map", "drive", "link", "scale", "model",
        "axiom", "beacon", "binary", "bit", "byte", "channel", "circuit", "cluster", "codec", "core",
        "cursor", "daemon", "data", "delta", "domain", "element", "entity", "epoch", "event", "factor",
        "fiber", "filter", "format", "function", "gateway", "graph", "hash", "header", "helix", "host",
        "icon", "image", "instance", "intent", "key", "latch", "limit", "loop", "macro", "metric",
        "mode", "nexus", "object", "offset", "opcode", "origin", "page", "parity", "payload", "peer",
        "pipeline", "pixel", "plugin", "pointer", "port", "process", "proxy", "query", "quota", "radix",
        "range", "ratio", "record", "relay", "replica", "request", "response", "result", "runtime", "sandbox",
        "scalar", "scope", "script", "sector", "segment", "semaphore", "sequence", "server", "service", "session",
        "shard", "shell", "shift", "signature", "slice", "socket", "source", "span", "spec", "sprite",
        "status", "storage", "struct", "subnet", "symbol", "sync", "syntax", "target", "task", "template",
        "tensor", "terminal", "theorem", "threshold", "tier", "timeout", "timer", "topic", "topology", "traffic",
        "trigger", "tuple", "type", "unit", "uplink", "uptime", "value", "variable", "vault", "version",
        "vertex", "view", "virtual", "volume", "webhook", "widget", "window", "worker", "wrapper", "zone",
        "algebra", "algorithm", "analog", "anchor", "anomaly", "aperture", "array", "artifact", "asset", "async",
        "atom", "attribute", "audit", "auth", "backend", "backup", "bandwidth", "baseline", "batch", "benchmark",
        "concept", "context", "contrast", "control", "cortex", "cosmos", "craft", "creed", "criterion", "crux",
        "current", "custom", "datum", "debug", "decode", "default", "defense", "degree", "delay", "density",
        "depth", "design", "detail", "device", "diagram", "dialect", "dialog", "digest", "digit", "dimension",
        "essence", "ethic", "ethos", "evolution", "expanse", "facet", "feedback", "focus", "force", "form",
        "formula", "fraction", "frequency", "frontier", "fusion", "future", "genesis", "gesture", "gradient", "gravity",
        "harmony", "heritage", "heuristic", "hierarchy", "horizon", "hypothesis", "ideal", "identity", "illusion", "impact"
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
