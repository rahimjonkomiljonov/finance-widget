package com.rahimjon.financewidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StateStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "state.json")

    private val holding = Holding("id1", "AAPL", 10.0, 150.0, Currency.KRW, "2026-06-08", "note")

    @Test
    fun `starts empty when there is no file`() {
        assertEquals(AppState(), StateStore(file()).snapshot())
    }

    @Test
    fun `saved state survives a restart`() {
        StateStore(file()).update { it.copy(holdings = listOf(holding), settings = Settings(displayCurrency = Currency.KRW)) }

        val reloaded = StateStore(file()).snapshot()
        assertEquals(listOf(holding), reloaded.holdings)
        assertEquals(Currency.KRW, reloaded.settings.displayCurrency)
    }

    @Test
    fun `no temp file is left behind after a save`() {
        StateStore(file()).update { it.copy(holdings = listOf(holding)) }

        assertEquals(listOf("state.json"), tmp.root.list()!!.toList())
    }

    @Test
    fun `an unreadable file is set aside, not overwritten or lost`() {
        file().writeText("{ this is not json")

        val store = StateStore(file())
        assertEquals(AppState(), store.snapshot())

        val kept = tmp.root.listFiles()!!.filter { it.name.startsWith("state.json.corrupt-") }
        assertEquals(1, kept.size)
        assertEquals("{ this is not json", kept.single().readText())
    }

    @Test
    fun `newer files with extra fields still load`() {
        file().writeText("""{"holdings":[],"someFutureField":42}""")
        assertEquals(AppState(), StateStore(file()).snapshot())
    }

    @Test
    fun `an update that changes nothing does not rewrite the file`() {
        val store = StateStore(file())
        store.update { it } // no-op on a fresh store
        assertFalse(file().exists())
    }

    @Test
    fun `holdings saved before the buy currency existed default to USD`() {
        file().writeText("""{"holdings":[{"id":"1","ticker":"AAPL","shares":1.0,"buyPrice":100.0}]}""")

        assertEquals(Currency.USD, StateStore(file()).snapshot().holdings.single().buyPriceCurrency)
    }

    @Test
    fun `the flow reflects updates`() {
        val store = StateStore(file())
        store.update { it.copy(holdings = listOf(holding)) }
        assertTrue(store.state.value.holdings.isNotEmpty())
    }
}
