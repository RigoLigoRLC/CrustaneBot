package org.tech4c57.bot

class BotCommand {
    var commandName: String = ""
    var params: MutableList<String> = mutableListOf()
}

object CmdParser {
    fun parse(cmd: String): BotCommand {
        var ret = BotCommand()
        var i = 0;
        val b = cmd.length;
        var rCmd = ""

        i = advanceBlank(cmd, i, b); if(i < 0) return ret
        if(cmd[i] != '%') return ret // Commands begin with % always
        i++

        i = advanceBlank(cmd, i, b); if(i < 0) return ret
        val pCmd = getString(cmd, i, b); i = pCmd.first; ret.commandName = pCmd.second

        var pParam: Pair<Int, String>
        while(true) {
            i = advanceBlank(cmd, i, b)
            if(i < 0) return ret
            pParam = getString(cmd, i, b); i = pParam.first; ret.params.add(pParam.second)
        }

        return ret
    }

    fun parseZeroArg(cmd: String): BotCommand {
        val retChk = parse(cmd)
        if(retChk.params.size != 0)
            return BotCommand()
        else
            return retChk
    }

    fun advanceBlank(cmd: String, i_: Int, b: Int): Int {
        var i = i_
        while(i < b) {
            when(cmd[i++]) {
                ' ', '\t' -> continue
                else -> return i - 1
            }
        }
        return -1
    }

    fun getString(cmd: String, i_: Int, b: Int): Pair<Int, String> {
        var ret = ""
        var i = i_
        val doEscapeQuote = cmd[i_] == '"'
        if(doEscapeQuote) i++

        while(i < b) {
            val ic = cmd[i++]
            when(ic) {
                '\\' -> {
                    val iec = cmd[i++]
                    if(doEscapeQuote)
                        when(iec) {
                            '"' -> ret += '"'
                            '\\' -> ret += '\\'
                            'n' -> ret += '\n'
                            else -> {
                                ret += ic
                                ret += iec
                            }
                        }
                }
                ' ' -> {
                    if(doEscapeQuote)
                        ret += ' '
                    else
                        break
                }
                '"' -> break
                else -> ret += ic
            }
        }

        return Pair(i, ret)
    }
}