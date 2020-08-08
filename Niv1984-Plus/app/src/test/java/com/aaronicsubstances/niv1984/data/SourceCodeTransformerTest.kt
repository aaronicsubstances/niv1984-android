package com.aaronicsubstances.niv1984.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceCodeTransformerTest {

    @Test
    fun test1() {
        runTestTransformer("", "", listOf())
    }

    @Test
    fun test2() {
        runTestTransformer("pie", "pie",
            listOf(TransformRequest(0, "", 0)))
    }

    @Test
    fun test3() {
        runTestTransformer("pie", "",
            listOf(TransformRequest(-3, "", 0, 3)))
    }

    @Test
    fun test4() {
        runTestTransformer("", "pie",
            listOf(TransformRequest(3, "pie", 0)))
    }

    @Test
    fun test5() {
        runTestTransformer("I am going to school.","She's going to school?!",
            listOf(TransformRequest(2, "She", 0, 1),
                TransformRequest(1, "'s", 1, 4),
                TransformRequest(1, "?", 20, 21),
                TransformRequest(2, "!", 21)
            ))
    }

    private fun runTestTransformer(
        originalText: String, expected: String,
        transforms: List<TransformRequest>
    ) {
        val instance = SourceCodeTransformer(originalText)
        assertEquals(instance.positionAdjustment, 0)
        for ((totalDiff, replacement, startPos, endPos) in transforms) {
            if (endPos != -1) {
                instance.addTransform(replacement, startPos, endPos)
            } else {
                instance.addTransform(replacement, startPos)
            }
            assertEquals(instance.positionAdjustment, totalDiff)
        }
        val actual = instance.transformedText.toString()
        assertEquals(actual, expected)
    }

    data class TransformRequest(
        val totalDiff: Int,
        val replacement: String,
        val startPos: Int,
        val endPos: Int = -1
    )
}