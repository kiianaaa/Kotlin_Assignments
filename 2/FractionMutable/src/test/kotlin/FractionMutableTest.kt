import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class FractionMutableTest {

    @Test
    fun testCons() {
        val a = FractionMutable(2, 4, -1)
        assertEquals("-1/2", a.toString())
    }

    @Test
    fun testToString() {
        val a = FractionMutable(1, 2, -1)
        assertEquals("-1/2", a.toString())
    }

    @Test
    fun negate() {
        val a = FractionMutable(1, 2, -1)
        a.negate()
        assertEquals("1/2", a.toString())
    }

    @Test
    fun addPos1() {
        val a = FractionMutable(1, 2)
        a.add(FractionMutable(1, 3))
        assertEquals("5/6", a.toString())
    }

    @Test
    fun addPosNeg1() {
        val a = FractionMutable(1, 2)
        a.add(FractionMutable(1, 3, -1))
        assertEquals("1/6", a.toString())
    }

    @Test
    fun multPos() {
        val a = FractionMutable(1, 2)
        a.mult(FractionMutable(1, 3))
        assertEquals("1/6", a.toString())
    }

    @Test
    fun multPosNeg1() {
        val a = FractionMutable(1, 2)
        a.mult(FractionMutable(1, 3, -1))
        assertEquals("-1/6", a.toString())
    }

    @Test
    fun div() {
        val a = FractionMutable(8, 3)
        a.div(FractionMutable(4, 6))
        assertEquals("4/1", a.toString())
    }

    @Test
    fun intPart() {
        val a = FractionMutable(8, 3)
        assertEquals(2, a.intPart())
    }
}
