fun main() {
    val a = FractionMutable(1, 2, -1)
    a.add(FractionMutable(1, 3))
    println(a)  // Output: -1/6

    a.mult(FractionMutable(5, 2, -1))
    println(a)  // Output: 5/12

    a.div(FractionMutable(2, 1))
    println(a)  // Output: 5/24
}
