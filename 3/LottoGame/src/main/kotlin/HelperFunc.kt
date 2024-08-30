class HelperFunc {

    fun readNDistinct(low: Int, high: Int, n: Int): List<Int> {
        while (true) {
            println("Give $n numbers from $low to $high, separated by commas:")
            val input = readLine()

            if (input != null) {
                val numbers = input.split(",")
                    .map { it.trim().toIntOrNull() }
                    .filterNotNull()

                if (numbers.size == n && numbers.toSet().size == n && numbers.all { it in low..high }) {
                    return numbers
                } else {
                    println("Invalid input. Please make sure to provide $n distinct numbers in the range from $low to $high.")
                }
            } else {
                println("Input cannot be null. Please try again.")
            }
        }
    }

    fun playLotto() {
        val lotto = Lotto()
        val secretNumbers = lotto.pickNDistinct(1..40, 7) ?: return

        while (true) {
            val guess = readNDistinct(1, 40, 7)
            val correctGuesses = lotto.checkGuess(guess, secretNumbers)
            println("Lotto numbers: $secretNumbers")
            println("you got $correctGuesses correct")

            println("More? (Y/N):")
            val response = readLine()?.trim()?.uppercase()
            if (response != "Y") {
                break
            }
        }
    }

}

