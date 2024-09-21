package com.example.lab10

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.numbergame.ui.theme.NumberGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NumberGameTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val numRange = 1..10
    var game by rememberSaveable { mutableStateOf(NumberGame(numRange)) }
    var userInput by rememberSaveable { mutableStateOf("") }
    var validInput by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        GreetingMessage(numRange)

        Spacer(modifier = Modifier.height(16.dp))

        NumberInputField(
            value = userInput,
            onValueChange = { newValue ->
                userInput = newValue
                validInput = validateInput(newValue)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))


    }
}

@Composable
fun GreetingMessage(numRange: IntRange) {
    Text(text = "Welcome! Try to guess the number between ${numRange.first} and ${numRange.last}")
}

@Composable
fun NumberInputField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        label = { Text("Enter your guess") }
    )
}

@Composable
fun GuessButton(validInput: Boolean, onGuessMade: () -> Unit) {
    Button(
        onClick = onGuessMade,
        enabled = validInput,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Make Guess")
    }
}

@Composable
fun GuessResultMessage(result: GuessResult, guesses: List<Int>) {
    Column {
        Text("Guess Result: $result")
        Text("Your guesses: ${guesses.sorted()}")
    }
}

fun validateInput(input: String): Boolean {
    return input.isNotEmpty() && input.all { it.isDigit() }
}

enum class GuessResult { HIGH, LOW, HIT }

class NumberGame(val range: IntRange) {
    private val secret = range.random()
    var guesses = mutableListOf<Int>()
        private set

    fun makeGuess(guess: Int): GuessResult {
        guesses.add(guess)
        return when {
            guess < secret -> GuessResult.LOW
            guess > secret -> GuessResult.HIGH
            else -> GuessResult.HIT
        }
    }
}
