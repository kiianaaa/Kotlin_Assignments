class Lotto {

    fun pickNDistinct(range: IntRange, n: Int) : List<Int>? {
        if (n > range.count()) return null
        return range.shuffled().take(n)
    }

    fun numDistinct(list: List<Int>) : Int? {
        return list.toSet().size
    }

    fun numCommon (list1: List<Int>, list2: List<Int>) : Int {
        return list1.intersect(list2).size
    }

    fun isLegal (guess: List<Int>, range: IntRange = 1..40, count: Int = 7) : Boolean {
        return guess.size == count && guess.toSet().size == count && guess.all {it in range}
    }

    fun checkGuess(guess: List<Int>, secret: List<Int>): Int {
        return if (isLegal(guess)) {
            numCommon(guess, secret)
        } else {
            0
        }
    }


}