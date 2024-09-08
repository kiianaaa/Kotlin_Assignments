class Fraction(numerator: Int, denominator: Int, sign: Int = 1) : Comparable<Fraction> {

    private val numerator: Int
    private val denominator: Int

    init {
        require(denominator != 0) { "Denominator cannot be zero" }

        // Calculate the GCD and reduce the fraction
        val gcd = gcd(Math.abs(numerator), Math.abs(denominator))

        // Ensure the fraction is stored with the correct sign
        this.numerator = (sign * numerator) / gcd
        this.denominator = Math.abs(denominator) / gcd
    }

    // Function to calculate GCD (Greatest Common Divisor)
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    // Override toString for easy printing
    override fun toString(): String {
        return if (numerator == 0) "0" else "${if (numerator < 0) "-" else ""}${Math.abs(numerator)}/${denominator}"
    }

    // Arithmetic operations that return new Fraction instances
    fun add(other: Fraction): Fraction {
        val newNumerator = this.numerator * other.denominator + other.numerator * this.denominator
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator)
    }

    fun subtract(other: Fraction): Fraction {
        val newNumerator = this.numerator * other.denominator - other.numerator * this.denominator
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator)
    }

    fun multiply(other: Fraction): Fraction {
        return Fraction(this.numerator * other.numerator, this.denominator * other.denominator)
    }

    fun divide(other: Fraction): Fraction {
        return Fraction(this.numerator * other.denominator, this.denominator * other.numerator)
    }

    // Unary minus for negation
    operator fun unaryMinus(): Fraction {
        return Fraction(-numerator, denominator)
    }

    // Operator overloads for +, -, *, /
    operator fun plus(other: Fraction): Fraction = add(other)
    operator fun minus(other: Fraction): Fraction = subtract(other)
    operator fun times(other: Fraction): Fraction = multiply(other)
    operator fun div(other: Fraction): Fraction = divide(other)

    // Compare two fractions by cross-multiplying to avoid floating point division
    override fun compareTo(other: Fraction): Int {
        return (this.numerator * other.denominator).compareTo(other.numerator * this.denominator)
    }

    // Override equals() and hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Fraction) return false
        return this.compareTo(other) == 0
    }

    override fun hashCode(): Int {
        return numerator.hashCode() * 31 + denominator.hashCode()
    }
}
