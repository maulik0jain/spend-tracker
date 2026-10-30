package com.smsreader.app.parse

import com.smsreader.app.data.Category
import com.smsreader.app.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizerTest {
    private fun classify(payee: String, channel: Channel = Channel.UPI) = Categorizer.classify(payee, channel)

    @Test fun merchantUpiIdBecomesBrand() {
        assertEquals(Classification("Netflix", Category.SUBSCRIPTIONS), classify("netflixupi.payu@hdfcbank"))
        assertEquals(Classification("Netflix", Category.SUBSCRIPTIONS), classify("NETFLIX.COM", Channel.CARD))
    }

    @Test fun specificBrandWinsOverContainedOne() {
        assertEquals("Swiggy Instamart", classify("SWIGGY INSTAMART").brand)
        assertEquals("Amazon Prime", classify("AMAZON PRIME").brand)
    }

    @Test fun peopleByNameAndUpiId() {
        assertEquals(Category.PEOPLE, classify("RAHUL SHARMA").category)
        assertEquals(Category.PEOPLE, classify("son.sharma@okaxis").category)
        assertEquals(Category.PEOPLE, classify("9876543210@ybl").category)
    }

    @Test fun shopsAreNotPeople() {
        assertEquals(Category.OTHER, classify("SHREE GANESH ENTERPRISES").category)
        assertEquals(Category.OTHER, classify("q123456789@ybl").category)
        assertEquals(Category.GROCERIES, classify("SHARMA GENERAL STORE").category)
    }

    @Test fun cardBillAndCash() {
        assertEquals(Category.CARD_BILL, classify("cred.club@axisb").category)
        assertEquals(Category.CASH, classify("ATM SBIN0001234", Channel.ATM).category)
        assertEquals(Category.OTHER, classify(TransactionParser.UNKNOWN_PAYEE).category)
    }
}
