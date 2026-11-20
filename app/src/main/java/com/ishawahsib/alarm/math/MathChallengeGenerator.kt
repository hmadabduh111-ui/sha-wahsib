package com.ishawahsib.alarm.math

import com.ishawahsib.alarm.data.MathDifficulty
import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val answer: Int
)

object MathChallengeGenerator {

    fun generate(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> generateEasy()
            MathDifficulty.MEDIUM -> generateMedium()
            MathDifficulty.HARD -> generateHard()
        }
    }

    private fun generateEasy(): MathProblem {
        val isAddition = Random.nextBoolean()
        return if (isAddition) {
            val a = Random.nextInt(12, 65)
            val b = Random.nextInt(11, 55)
            MathProblem("$a + $b = ؟", a + b)
        } else {
            val b = Random.nextInt(11, 45)
            val result = Random.nextInt(10, 50)
            val a = b + result
            MathProblem("$a - $b = ؟", result)
        }
    }

    private fun generateMedium(): MathProblem {
        return when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(6, 16)
                val b = Random.nextInt(4, 13)
                val c = Random.nextInt(8, 35)
                MathProblem("($a × $b) + $c = ؟", (a * b) + c)
            }
            1 -> {
                val divisor = Random.nextInt(3, 12)
                val quotient = Random.nextInt(6, 19)
                val dividend = divisor * quotient
                val add = Random.nextInt(5, 25)
                MathProblem("($dividend ÷ $divisor) + $add = ؟", quotient + add)
            }
            else -> {
                val a = Random.nextInt(7, 15)
                val b = Random.nextInt(5, 12)
                val sub = Random.nextInt(4, (a * b) - 5)
                MathProblem("($a × $b) - $sub = ؟", (a * b) - sub)
            }
        }
    }

    private fun generateHard(): MathProblem {
        return when (Random.nextInt(3)) {
            0 -> {
                val base = Random.nextInt(11, 19)
                val offset = Random.nextInt(15, 65)
                MathProblem("($base × $base) - $offset = ؟", (base * base) - offset)
            }
            1 -> {
                val a = Random.nextInt(12, 24)
                val b = Random.nextInt(4, 9)
                val c = Random.nextInt(15, 45)
                MathProblem("($a × $b) + $c = ؟", (a * b) + c)
            }
            else -> {
                val divisor = Random.nextInt(4, 13)
                val quotient = Random.nextInt(12, 25)
                val dividend = divisor * quotient
                val multiplier = Random.nextInt(3, 7)
                MathProblem("($dividend ÷ $divisor) × $multiplier = ؟", quotient * multiplier)
            }
        }
    }
}
