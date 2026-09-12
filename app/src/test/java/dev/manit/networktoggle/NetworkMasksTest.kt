package dev.manit.networktoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMasksTest {
    @Test fun androidMasksAreCorrect() {
        assertEquals(4096L, NetworkMasks.LTE)
        assertEquals(524288L, NetworkMasks.NR)
    }

    @Test fun disabling5gPreservesEveryOtherRat() {
        val original = 0b101101L or NetworkMasks.LTE or NetworkMasks.NR
        val changed = NetworkMasks.with5g(original, false)
        assertFalse(changed and NetworkMasks.NR != 0L)
        assertEquals(original and NetworkMasks.NR.inv(), changed)
    }

    @Test fun enabling5gPreservesEveryOtherRat() {
        val original = 0b101101L or NetworkMasks.LTE
        val changed = NetworkMasks.with5g(original, true)
        assertTrue(changed and NetworkMasks.NR != 0L)
        assertEquals(original, changed and NetworkMasks.NR.inv())
    }

    @Test fun parserHandlesExactPocoOutput() {
        val live = "GPRS|EDGE|UMTS|HSDPA|HSUPA|HSPA|LTE|HSPA+|GSM|TD_SCDMA|LTE_CA|NR\n"
        assertEquals(906119L, AllowedTypesCodec.parse(live))
        assertEquals(381831L, AllowedTypesCodec.parse(live.removeSuffix("|NR\n")))
    }

    @Test fun parserRejectsErrorsNumbersUnknownAndEmptyResults() {
        assertEquals(null, AllowedTypesCodec.parse("Allowed network types: 906119"))
        assertEquals(null, AllowedTypesCodec.parse("SecurityException for slot 0"))
        assertEquals(null, AllowedTypesCodec.parse("LTE|FUTURE_RADIO"))
        assertEquals(null, AllowedTypesCodec.parse("LTE|LTE"))
        assertEquals(null, AllowedTypesCodec.parse(""))
    }

    @Test fun shellWritesUseBinaryEncoding() {
        assertEquals("11011101001110000111", AllowedTypesCodec.toBinary(906119L))
        assertEquals("1011101001110000111", AllowedTypesCodec.toBinary(381831L))
        assertEquals(null, AllowedTypesCodec.toBinary(0L))
    }

    @Test fun unsafePoliciesAreRejected() {
        assertFalse(NetworkMasks.isSafeBase(0L))
        assertFalse(NetworkMasks.isSafeBase(NetworkMasks.NR))
        assertTrue(NetworkMasks.isSafeBase(NetworkMasks.LTE or NetworkMasks.NR))
    }
}
