package com.stivestabletop.scorer

import android.util.Log

private const val TAG = "SpecialCalc"

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
                    // Push any number we have been collecting
                    pushNumStr(numstr.toString())
                    numstr.clear()
                    // Try to push the specified variable
                    val ok = pushVariable(variables, notation.toInt() - 'A'.toInt())
                    if (!ok) {
                        Log.e(TAG, "Missing variable '$notation' - setting calculation to zero")
                        return 0
                    }
                }
                in '0'..'9' -> numstr.append(notation)
                else -> {
                    // Push any number we have been collecting
                    pushNumStr(numstr.toString())
                    numstr.clear()
                    // Perform the chosen operation
                    val ok = performOp(notation)
                    if (!ok) {
                        val stack = sizeStack()
                        Log.e(
                            TAG,
                            "Unknown op '$notation' or Not enough stack '$stack' - setting calculation to zero"
                        )
                        return 0
                    }
                }
            }
        }
        val stack = sizeStack()
        if (stack != 1) {
            Log.e(TAG, "Incorrect final stack size of '$stack' - setting calculation to zero")
            return 0
        }
        return popStack()
    }

    private fun pushVariable(variables: List<Int>, idx: Int): Boolean {
        assert(variables.size >= idx)
        if (variables.size >= idx) {
            pushStack(variables[idx])
            return true
        }
        return false
    }

    private fun pushNumStr(numstr: String) {
        if (numstr.isNotBlank())
            pushStack(numstr.toInt())
    }

    private fun sizeStack(): Int {
        return datastack.size
    }

    private fun popStack(): Int {
        assert(datastack.size > 0)
        if (datastack.size > 0)
            return datastack.removeAt(datastack.lastIndex)
        return 0
    }

    private fun pushStack(num: Int) {
        datastack.add(num)
    }

    private fun performOp(operation: Char): Boolean {
        // Assume operations require two operands
        if (sizeStack() < 2)
            return false
        val y = popStack()
        val x = popStack()
        var answer = 0
        when (operation) {
            '+' -> answer = x + y
            '-' -> answer = x - y
            '*' -> answer = x * y
            '/' -> answer = x / y
            'n' -> answer = if (x < y) x else y // Minimum
            'x' -> answer = if (x > y) x else y // Maximum
            else -> {
                assert(false)
                return false
            }
        }
        pushStack(answer)
        return true
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