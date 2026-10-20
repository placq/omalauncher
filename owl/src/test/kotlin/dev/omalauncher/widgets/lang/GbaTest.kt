package dev.omalauncher.widgets.lang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GbaTest {
    @Test fun gbaNeedsPatchedLanguage() {
        val message = error("""face "gba" { meta { owl: 12 }; Gba { rom: "/root/game.gba" } }""")
        assertTrue(message.contains("13"))
    }

    @Test fun gbaNeedsRom() {
        assertTrue(error("""face "gba" { meta { owl: 13 }; Gba {} }""").contains("rom"))
    }

    @Test fun gbaCompilesAndRequiresLinux() {
        val p = Compiler.compile("""face "gba" {
            meta { owl: 13 }
            page: -1
            requires { pacman: ["mgba-sdl"] }
            state game = "/root/ROMs/a game.gba"
            Gba { rom: game }
        }""")
        assertTrue(p.needsLinux)
        assertEquals("Gba", (p.root.single() as WNode.Element).kind)
    }

    private fun error(source: String): String = try {
        Compiler.compile(source)
        fail("Expected a compile error")
        ""
    } catch (e: CompileError) {
        e.message.orEmpty()
    }
}
