package com.stashed.app.intelligence

/**
 * Maps item names to relevant emojis via keyword matching.
 * Checks item text for each keyword using substring/word matching.
 * Falls back to 📦 when nothing matches.
 */
object EmojiMapper {

    private val EMOJI_MAP: List<Pair<List<String>, String>> = listOf(
        listOf("key", "keys", "keychain") to "🔑",
        listOf("passport", "travel document") to "🛂",
        listOf("glasses", "specs", "spectacles", "sunglasses", "shades") to "👓",
        listOf("charger", "charging cable", "usb cable", "power cable", "power adapter", "adapter") to "🔌",
        listOf("phone", "mobile", "smartphone") to "📱",
        listOf("laptop", "computer", "macbook", "notebook") to "💻",
        listOf("bag", "backpack", "rucksack", "tote", "handbag", "purse") to "🎒",
        listOf("gym bag", "gym kit", "gym stuff") to "🏋️",
        listOf("medicine", "medication", "tablet", "pill", "tablets", "pills", "inhaler") to "💊",
        listOf("wallet", "purse", "cards") to "👛",
        listOf("watch", "wristwatch") to "⌚",
        listOf("headphones", "earphones", "earbuds", "airpods", "headset") to "🎧",
        listOf("book", "notebook", "journal", "diary") to "📚",
        listOf("document", "file", "papers", "forms", "certificate") to "📄",
        listOf("camera", "lens") to "📷",
        listOf("umbrella") to "☂️",
        listOf("tool", "hammer", "screwdriver", "drill", "spanner", "wrench") to "🔧",
        listOf("scissors") to "✂️",
        listOf("remote", "remote control", "tv remote") to "📺",
        listOf("pen", "pencil", "marker") to "✏️",
        listOf("card", "id card", "driving licence", "license", "visa") to "🪪",
        listOf("cash", "money", "notes", "coins") to "💵",
        listOf("charger", "powerbank", "power bank") to "🔋",
        listOf("food", "snack", "lunch") to "🍱",
        listOf("bottle", "water bottle", "flask") to "🍶",
        listOf("toy", "toys") to "🧸",
        listOf("shoe", "shoes", "boots", "trainers", "sneakers") to "👟",
        listOf("jacket", "coat", "hoodie", "jumper", "sweater") to "🧥",
        listOf("helmet") to "⛑️",
        listOf("gym bag", "sports bag") to "🏅",
        listOf("ticket", "tickets", "boarding pass") to "🎫",
        listOf("airpod", "airpods") to "🎧",
        listOf("usb", "drive", "hard drive", "ssd") to "💾",
        listOf("sim card", "sim") to "📡",
    )

    fun getEmoji(itemName: String): String {
        val lower = itemName.lowercase()
        for ((keywords, emoji) in EMOJI_MAP) {
            if (keywords.any { keyword -> lower.contains(keyword) }) {
                return emoji
            }
        }
        return "📦"
    }
}
