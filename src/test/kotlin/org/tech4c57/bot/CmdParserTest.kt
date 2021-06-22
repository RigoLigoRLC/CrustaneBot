package org.tech4c57.bot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.text.CharacterIterator
import java.text.StringCharacterIterator

internal class CmdParserTest {

    @Test
    fun parse() {
        val c1 = CmdParser.parse("""  %  fm  mkdir "hello \"\\ "  fooo  """)
        assertTrue(c1.commandName == "fm")
        assertTrue(c1.params[0] == "mkdir")
        assertTrue(c1.params[1] == "hello \"\\ ")
        assertTrue(c1.params[2] == "fooo")
    }
}