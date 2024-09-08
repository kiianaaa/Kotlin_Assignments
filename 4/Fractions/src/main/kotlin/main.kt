fun main() {
    val f1 = Fraction(2, 2)
    val f2 = Fraction(3, 4)

    // Test arithmetic operations
    println("f1 + f2 = ${f1 + f2}")
    println("f1 - f2 = ${f1 - f2}")
    println("f1 * f2 = ${f1 * f2}")
    println("f1 / f2 = ${f1 / f2}")

    // Test comparison
    println("f1 > f2: ${f1 > f2}")
    println("f1 < f2: ${f1 < f2}")
    println("f1 == Fraction(2, 4): ${f1 == Fraction(2, 4)}")

    // Test negation
    println("-f1 = ${-f1}")
}
