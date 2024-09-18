import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

const val N = 100

class MainActivity : ComponentActivity() {
    class Account {
        private var amount: Double = 0.0
        private val mutex = Mutex() // Add a mutex for synchronization

        // Deposit function now uses mutex to synchronize access to the `amount` variable
        suspend fun deposit(amount: Double) {
            mutex.withLock {
                val x = this.amount
                delay(1) // simulates processing time
                this.amount = x + amount
            }
        }

        fun saldo(): Double = amount
    }

    /* Approximate measurement of the given block's execution time */
    private fun withTimeMeasurement(title: String, isActive: Boolean = true, code: () -> Unit) {
        if (!isActive) return
        val time = measureTimeMillis { code() }
        Log.i("MSU", "operation in '$title' took ${time} ms")
    }

    data class Saldos(val saldo1: Double, val saldo2: Double)

    fun bankProcess(account: Account): Saldos {
        var saldo1: Double = 0.0
        var saldo2: Double = 0.0

        /* We measure the execution time of one deposit task */
        withTimeMeasurement("Single coroutine deposit $N times") {
            runBlocking {
                launch {
                    for (i in 1..N)
                        account.deposit(0.0)
                }
                saldo1 = account.saldo()
            }
        }

        /* Then we measure the execution time of two simultaneous deposit tasks using coroutines */
        withTimeMeasurement("Two $N times deposit coroutines together", isActive = true) {
            runBlocking {
                launch {
                    for (i in 1..N) account.deposit(1.0)
                }
                launch {
                    for (i in 1..N) account.deposit(1.0)
                }
            }
            saldo2 = account.saldo()
        }
        return Saldos(saldo1, saldo2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val results = bankProcess(Account())
        setContent {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ShowResults(saldo1 = results.saldo1, saldo2 = results.saldo2)
            }

        }
    }
}

@Composable
fun ShowResults(saldo1: Double, saldo2: Double) {
    Column {
        Text(text = "Saldo1: $saldo1")
        Text(text = "Saldo2: $saldo2")
    }
}
