package com.smsreader.app.parse

import com.smsreader.app.data.Category
import com.smsreader.app.data.Channel

/** What a payee is, as far as its name or UPI ID tells. [brand] is set for well-known merchants. */
data class Classification(val brand: String?, val category: Category)

/**
 * Guesses a category from the payee alone. SMS never say what a payment was *for*, only
 * whom it went to, so anything not recognised here is fixed by the user tagging the payee.
 */
object Categorizer {
    private class Rule(val name: String?, val category: Category, pattern: String) {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    }

    // Order matters: specific names come before names they contain (instamart before swiggy).
    private val brands = listOf(
        Rule("Credit card bill", Category.CARD_BILL, """credit ?card|\bcred\b|card ?bill|\bcc ?bill"""),
        Rule("Netflix", Category.SUBSCRIPTIONS, "netflix"),
        Rule("Spotify", Category.SUBSCRIPTIONS, "spotify"),
        Rule("Amazon Prime", Category.SUBSCRIPTIONS, "prime ?video|amazon ?prime"),
        Rule("JioHotstar", Category.SUBSCRIPTIONS, "hotstar"),
        Rule("YouTube", Category.SUBSCRIPTIONS, "youtube"),
        Rule("Google Play", Category.SUBSCRIPTIONS, "google ?play|google ?one"),
        Rule("Apple", Category.SUBSCRIPTIONS, """apple\.com|apple services|itunes"""),
        Rule("SonyLIV", Category.SUBSCRIPTIONS, "sonyliv"),
        Rule("ZEE5", Category.SUBSCRIPTIONS, "zee5"),
        Rule("Swiggy Instamart", Category.GROCERIES, "instamart"),
        Rule("Swiggy", Category.FOOD, "swiggy"),
        Rule("Zomato", Category.FOOD, "zomato"),
        Rule("Domino's", Category.FOOD, "domino"),
        Rule("McDonald's", Category.FOOD, "mcdonald"),
        Rule("Starbucks", Category.FOOD, "starbucks"),
        Rule("Blinkit", Category.GROCERIES, "blinkit|grofers"),
        Rule("Zepto", Category.GROCERIES, "zepto"),
        Rule("BigBasket", Category.GROCERIES, "big ?basket"),
        Rule("DMart", Category.GROCERIES, "d-?mart|avenue supermarts"),
        Rule("JioMart", Category.GROCERIES, "jiomart"),
        Rule("Amazon", Category.SHOPPING, "amazon"),
        Rule("Flipkart", Category.SHOPPING, "flipkart"),
        Rule("Myntra", Category.SHOPPING, "myntra"),
        Rule("Ajio", Category.SHOPPING, "ajio"),
        Rule("Meesho", Category.SHOPPING, "meesho"),
        Rule("Nykaa", Category.SHOPPING, "nykaa"),
        Rule("Uber", Category.TRAVEL, """\buber"""),
        Rule("Ola", Category.TRAVEL, """\bola\b|olacabs"""),
        Rule("Rapido", Category.TRAVEL, "rapido"),
        Rule("IRCTC", Category.TRAVEL, "irctc"),
        Rule("MakeMyTrip", Category.TRAVEL, "makemytrip"),
        Rule("Airtel", Category.BILLS, "airtel"),
        Rule("Jio", Category.BILLS, """\bjio\b|reliance ?jio"""),
        Rule("Vi", Category.BILLS, """vodafone|\bvi\b"""),
        Rule("BookMyShow", Category.ENTERTAINMENT, "bookmyshow"),
        Rule("PVR INOX", Category.ENTERTAINMENT, """\bpvr\b|\binox\b"""),
        Rule("Zerodha", Category.INVESTMENT, "zerodha"),
        Rule("Groww", Category.INVESTMENT, "groww"),
        Rule("LIC", Category.INVESTMENT, """\blic\b|life insurance"""),
    )

    // Generic words: set the category but keep the payee's own name.
    private val keywords = listOf(
        Rule(null, Category.FUEL, """petrol|fuel|filling station|petroleum|indian ?oil|\b(?:iocl?|bpcl|hpcl)\b"""),
        Rule(null, Category.BILLS, """electricity|\bpower\b|\bgas\b|broadband|recharge|bill ?desk|\bbbps\b|postpaid|prepaid|\bdth\b|fastag"""),
        Rule(null, Category.HEALTH, """hospital|clinic|pharma|medical|chemist|diagnostic|apollo|1mg|netmeds|pharmeasy"""),
        Rule(null, Category.EDUCATION, """school|college|university|academy|tuition|\bfees?\b"""),
        Rule(null, Category.GROCERIES, """kirana|supermarket|provision|general store"""),
        Rule(null, Category.FOOD, """restaurant|\bcafe\b|dhaba|bakery|sweets|\bfoods?\b"""),
        Rule(null, Category.INVESTMENT, """insurance|mutual fund|\bsip\b|indian clearing"""),
    )

    private val phoneNumberVpa = Regex("""^(?:\+?91)?\d{10}@""")
    private val personalUpiHandle = Regex(
        """@(?:ok(?:axis|hdfcbank|icici|sbi)|ybl|ibl|axl|apl|yapl|paytm|pt(?:yes|axis|hdfc|sbi)|upi|naviaxis|superyes|kotak|kmbl|icici|sbi|hdfcbank|axisbank|yesbank|federal|indus|aubank|idfcbank|pnb|barodampay|freecharge|jupiteraxis|fam|wa(?:axis|hdfcbank|sbi|icici))$""",
        RegexOption.IGNORE_CASE,
    )
    private val personLikeUpiId = Regex("""^[a-z]+(?:[._][a-z]+)*\d{0,4}$""", RegexOption.IGNORE_CASE)
    private val personLikeName = Regex("""^[a-z]{2,}(?: [a-z]+){0,3}$""", RegexOption.IGNORE_CASE)
    private val businessWord = Regex(
        """\b(?:stores?|mart|enterprises?|traders?|trading|services|solutions|technologies|tech|industries|retail|agency|agencies|center|centre|shop|bazaar|general|motors|auto|electricals|hardware|company|co|bank|payments?|pay|ltd|limited|pvt|private|llp|india|merchant|online|digital|club|fitness|gym|salon|studio|hotel)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun classify(payee: String, channel: Channel): Classification {
        if (channel == Channel.ATM) return Classification(null, Category.CASH)
        if (payee == TransactionParser.UNKNOWN_PAYEE) return Classification(null, Category.OTHER)
        brands.firstOrNull { it.regex.containsMatchIn(payee) }?.let { return Classification(it.name, it.category) }
        keywords.firstOrNull { it.regex.containsMatchIn(payee) }?.let { return Classification(null, it.category) }
        return Classification(null, if (looksLikePerson(payee)) Category.PEOPLE else Category.OTHER)
    }

    private fun looksLikePerson(payee: String): Boolean {
        if ('@' in payee) {
            if (phoneNumberVpa.containsMatchIn(payee)) return true
            val id = payee.substringBefore('@')
            return personalUpiHandle.containsMatchIn(payee) &&
                personLikeUpiId.matches(id) &&
                !businessWord.containsMatchIn(id.replace('.', ' ').replace('_', ' '))
        }
        return payee.length <= 32 && personLikeName.matches(payee) && !businessWord.containsMatchIn(payee)
    }
}
