package dev.aauth.app.ui

import dev.aauth.app.ui.home.ServiceBrands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceBrandsTest {

    @Test
    fun matchesCommonNames() {
        assertEquals("github", ServiceBrands.find("GitHub")?.slug)
        assertEquals("github", ServiceBrands.find("GitHub Enterprise")?.slug)
        assertEquals("google", ServiceBrands.find("Google")?.slug)
        assertEquals("amazon", ServiceBrands.find("Amazon Web Services")?.slug)
        assertEquals("discord", ServiceBrands.find("  discord ")?.slug)
    }

    @Test
    fun shortNamesMustStartTheName() {
        assertEquals("aws", ServiceBrands.find("AWS Console")?.slug)
        assertNull(ServiceBrands.find("Jaws"))
        assertEquals("x", ServiceBrands.find("X")?.slug)
        assertNull(ServiceBrands.find("Xylophone Shop"))
    }

    @Test
    fun unknownOrEmptyNamesHaveNoBrand() {
        assertNull(ServiceBrands.find("My Small Blog"))
        assertNull(ServiceBrands.find(""))
        assertNull(ServiceBrands.find("   "))
    }
}
