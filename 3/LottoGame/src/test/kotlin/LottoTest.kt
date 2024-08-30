import org.junit.jupiter.api.Assertions.*

fun main() {
    val lotto = Lotto()

    // Test pickNDistinct
    val numbers = lotto.pickNDistinct(1..40, 7)
    println("Picked numbers: $numbers")

    // Test numDistinct
    val list = listOf(1, 2, 3, 3, 4, 5, 5)
    println("Number of distinct elements: ${lotto.numDistinct(list)}")

    // Test numCommon
    val list1 = listOf(1, 2, 3, 4)
    val list2 = listOf(3, 4, 5, 6)
    println("Number of common elements: ${lotto.numCommon(list1, list2)}")

    // Test isLegalLottoGuess
    val guess = listOf(1, 2, 3, 4, 5, 6, 7)
    println("Is legal lotto guess: ${lotto.isLegal(guess)}")

    // Test checkGuess
    val secret = listOf(3, 4, 7, 8, 9, 10, 11)
    println("Number of correct guesses: ${lotto.checkGuess(guess, secret)}")
}
