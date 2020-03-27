package com.stivestabletop.scorer

class SpecialCalc(val RPN: String) {
    // RPN is the reverse polish notation calculation in a string
    // Example: 2A* = 2 * A
    // where A is a variable (variables should be incremental capital alpha chars)

    // Stack for data
    private var datastack = mutableListOf<Int>()

    // Accepts list of integers that match the variables in the RPN
    fun calculate(variables: List<Int>): Int {
        val numstr = StringBuilder()
        for (notation in RPN.asSequence()) {
            when (notation) {
                in 'A'..'Z' -> {
                    pushNumStr(numstr.toString())
                    numstr.clear()
                    pushVariable(variables, notation.toInt() - 'A'.toInt())
                }
                in '0'..'9' -> numstr.append(notation)
                else -> {
                    pushNumStr(numstr.toString())
                    numstr.clear()
                    performOp(notation)
                }
            }
        }
        return popStack()
    }

    private fun pushVariable(variables: List<Int>, idx: Int) {
        assert(variables.size <= idx)
        pushStack(variables[idx])
    }

    private fun pushNumStr(numstr: String) {
        if (numstr.isNotBlank())
            pushStack(numstr.toInt())
    }

    private fun popStack(): Int {
        assert(datastack.size > 0)
        return datastack.removeAt(datastack.lastIndex)
    }

    private fun pushStack(num: Int) {
        datastack.add(num)
    }

    private fun performOp(operation: Char) {
        // Assume operations require two operands
        val x = popStack()
        val y = popStack()
        var answer = 0
        when (operation) {
            '+' -> answer = x + y
            '-' -> answer = x - y
            '*' -> answer = x * y
            '/' -> answer = x / y
            'n' -> answer = if (x < y) x else y // Minimum
            'x' -> answer = if (x > y) x else y // Maximum
            else -> assert(false)
        }
        pushStack(answer)
    }
}

/* TODO: Turn this into test?
 Currently tested via Kotlin playground - https://play.kotlinlang.org
fun main() {
    val c = SpecialCalc("AB+ABn*25+")
    var answer: Int

    answer = c.calculate(listOf<Int>(10,5))
    println("$answer == 100")
}
*/