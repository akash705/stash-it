package com.stashed.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiMapperTest {

    @Test
    fun `maps keys to key emoji`() = assertEquals("🔑", EmojiMapper.getEmoji("toilet key"))

    @Test
    fun `maps passport`() = assertEquals("🛂", EmojiMapper.getEmoji("passport"))

    @Test
    fun `maps glasses`() = assertEquals("👓", EmojiMapper.getEmoji("reading glasses"))

    @Test
    fun `maps sunglasses`() = assertEquals("👓", EmojiMapper.getEmoji("sunglasses"))

    @Test
    fun `maps charger`() = assertEquals("🔌", EmojiMapper.getEmoji("phone charger"))

    @Test
    fun `maps laptop`() = assertEquals("💻", EmojiMapper.getEmoji("laptop"))

    @Test
    fun `maps medicine`() = assertEquals("💊", EmojiMapper.getEmoji("medicine"))

    @Test
    fun `maps headphones`() = assertEquals("🎧", EmojiMapper.getEmoji("headphones"))

    @Test
    fun `maps wallet`() = assertEquals("👛", EmojiMapper.getEmoji("wallet"))

    @Test
    fun `maps unknown item to fallback`() = assertEquals("📦", EmojiMapper.getEmoji("random thing"))

    @Test
    fun `is case insensitive`() = assertEquals("🔑", EmojiMapper.getEmoji("KEYS"))

    @Test
    fun `matches substring within item name`() = assertEquals("🔌", EmojiMapper.getEmoji("old usb cable"))
}
