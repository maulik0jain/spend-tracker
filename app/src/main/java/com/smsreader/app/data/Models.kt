package com.smsreader.app.data

enum class Channel(val label: String) {
    UPI("UPI"),
    CARD("Card"),
    BANK_TRANSFER("Bank transfer"),
    ATM("ATM"),
    WALLET("Wallet"),
    OTHER("Other"),
}

enum class Category(val label: String) {
    SUBSCRIPTIONS("Subscriptions"),
    FOOD("Food & dining"),
    GROCERIES("Groceries"),
    SHOPPING("Shopping"),
    TRAVEL("Travel & transport"),
    FUEL("Fuel"),
    BILLS("Bills & recharges"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    EDUCATION("Education"),
    INVESTMENT("Investments & insurance"),
    CASH("Cash withdrawals"),
    PEOPLE("People"),
    CARD_BILL("Credit card bills"),
    OTHER("Other"),
}

/** One money-out event parsed from an SMS. Amounts are in paise so sums stay exact. */
data class Transaction(
    val amountPaise: Long,
    val payee: String,
    val payeeKey: String,
    val channel: Channel,
    val reference: String?,
    val timestamp: Long,
    val sender: String,
    val body: String,
)

/**
 * A [Transaction] after automatic categorisation and the user's own tags are applied.
 * [tagKey] is what a user tag is stored under. [groupKey] is what totals and recurring
 * detection group by, so two payees given the same name by the user merge into one.
 */
data class Payment(
    val txn: Transaction,
    val tagKey: String,
    val groupKey: String,
    val name: String,
    val category: Category,
)
