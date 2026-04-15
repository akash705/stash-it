package com.stashed.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NLParserTest {

    // ── Pattern 1: verb + item + preposition + location ───────────────────────

    @Test
    fun `parses verb-item-prep-location with 'put'`() {
        val result = NLParser.parse("put passport in wardrobe")
        assertEquals("Passport", result.item)
        assertEquals("Wardrobe", result.location)
    }

    @Test
    fun `parses verb with possessive article`() {
        val result = NLParser.parse("put my passport inside the wardrobe top shelf")
        assertEquals("Passport", result.item)
        assertEquals("Wardrobe top shelf", result.location)
    }

    @Test
    fun `parses 'left' verb`() {
        val result = NLParser.parse("left my keys on the kitchen counter")
        assertEquals("Keys", result.item)
        assertEquals("Kitchen counter", result.location)
    }

    @Test
    fun `parses 'stored' verb`() {
        val result = NLParser.parse("stored the charger behind the tv unit")
        assertEquals("Charger", result.item)
        assertEquals("Tv unit", result.location)
    }

    // ── Pattern 2: item + is + preposition + location ─────────────────────────

    @Test
    fun `parses 'item is preposition location'`() {
        val result = NLParser.parse("glasses are on the bedside table")
        assertEquals("Glasses", result.item)
        assertEquals("Bedside table", result.location)
    }

    @Test
    fun `parses 'is' singular form`() {
        val result = NLParser.parse("passport is in the wardrobe")
        assertEquals("Passport", result.item)
        assertEquals("Wardrobe", result.location)
    }

    // ── Pattern 3: simplest form ──────────────────────────────────────────────

    @Test
    fun `parses simplest item-prep-location`() {
        val result = NLParser.parse("toilet key in office desk drawer")
        assertEquals("Toilet key", result.item)
        assertEquals("Office desk drawer", result.location)
    }

    @Test
    fun `parses multi-word location`() {
        val result = NLParser.parse("gym bag in the car boot")
        assertEquals("Gym bag", result.item)
        assertEquals("Car boot", result.location)
    }

    @Test
    fun `parses 'under' preposition`() {
        val result = NLParser.parse("spare key under the front door mat")
        assertEquals("Spare key", result.item)
        assertEquals("Front door mat", result.location)
    }

    @Test
    fun `parses 'behind' preposition`() {
        val result = NLParser.parse("laptop charger behind the TV unit")
        assertEquals("Laptop charger", result.item)
        assertEquals("TV unit", result.location)
    }

    @Test
    fun `parses 'on top of' multi-word preposition`() {
        val result = NLParser.parse("remote on top of the fridge")
        assertEquals("Remote", result.item)
        assertEquals("Fridge", result.location)
    }

    @Test
    fun `parses 'next to' multi-word preposition`() {
        val result = NLParser.parse("medicine next to the bathroom sink")
        assertEquals("Medicine", result.item)
        assertEquals("Bathroom sink", result.location)
    }

    // ── Filler word stripping ─────────────────────────────────────────────────

    @Test
    fun `strips leading 'my' from item`() {
        val result = NLParser.parse("my keys in the drawer")
        assertEquals("Keys", result.item)
    }

    @Test
    fun `strips trailing punctuation`() {
        val result = NLParser.parse("keys in the drawer.")
        assertEquals("Keys", result.item)
        assertEquals("Drawer", result.location)
    }

    // ── Case handling ─────────────────────────────────────────────────────────

    @Test
    fun `capitalises item and location`() {
        val result = NLParser.parse("passport in wardrobe")
        assertTrue(result.item[0].isUpperCase())
        assertTrue(result.location[0].isUpperCase())
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Test
    fun `fallback: no preposition stores entire input as item`() {
        val result = NLParser.parse("passport")
        assertEquals("Passport", result.item)
        assertEquals("", result.location)
    }

    @Test
    fun `fallback: preserves raw text`() {
        val input = "passport in wardrobe"
        val result = NLParser.parse(input)
        assertEquals(input, result.rawText)
    }

    @Test
    fun `handles mixed case input`() {
        val result = NLParser.parse("PASSPORT IN WARDROBE")
        assertTrue(result.item.isNotBlank())
        assertTrue(result.location.isNotBlank())
    }
}
