import kotlin.math.absoluteValue

class FractionMutable(numerator: Int, denominator: Int, sign: Int = 1) {


    private var numerator: Int = numerator.absoluteValue
    private var denominator: Int = denominator.absoluteValue


    private var sign: Int = if (numerator * denominator < 0) -sign else sign

    init {
        if (denominator == 0) {
            throw IllegalArgumentException("Denominator cannot be zero")
        }
        simplify()  // Simplify the fraction after initialization
    }

    // GCD calculation to simplify fractions
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    // Simplify the fraction by dividing by the GCD
    private fun simplify() {
        val gcdValue = gcd(numerator, denominator)
        numerator /= gcdValue
        denominator /= gcdValue

        if (numerator < 0) {
            sign *= -1
            numerator = numerator.absoluteValue
        }

        if (denominator < 0) {
            sign *= -1
            denominator = denominator.absoluteValue
        }
    }

    override fun toString(): String {
        return "${if (sign == -1) "-" else ""}$numerator/$denominator"
    }

    fun toDecimal(): Double {
        return sign * numerator.toDouble() / denominator.toDouble()
    }

    fun negate() {
        sign *= -1
    }

    fun add(other: FractionMutable) {
        val commonDenominator = denominator * other.denominator
        val newNumerator = sign * numerator * other.denominator + other.sign * other.numerator * denominator

        numerator = newNumerator.absoluteValue
        denominator = commonDenominator
        sign = if (newNumerator >= 0) 1 else -1

        simplify()
    }

    fun mult(other: FractionMutable) {
        numerator *= other.numerator
        denominator *= other.denominator
        sign *= other.sign
        simplify()
    }

    fun div(other: FractionMutable) {
        if (other.numerator == 0) {
            throw IllegalArgumentException("Cannot divide by zero")
        }
        numerator *= other.denominator
        denominator *= other.numerator
        sign *= other.sign
        simplify()
    }

    fun intPart(): Int {
        return (numerator * sign) / denominator
    }
}
