package com.example.parliamentapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.parliament.R
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // Define some sample MP data
    data class MP(val name: String, val constituency: String)

    // Sample list of MPs
    private val mpList = listOf(
        MP("John Doe", "Springfield"),
        MP("Jane Smith", "Shelbyville"),
        MP("Mark Johnson", "Capital City"),
        MP("Anna Brown", "Ogdenville"),
        MP("James Wilson", "North Haverbrook")
    )

    private lateinit var mpImageView: ImageView
    private lateinit var mpNameTextView: TextView
    private lateinit var mpConstituencyTextView: TextView
    private lateinit var nextMPButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mpImageView = findViewById(R.id.mp_image)
        mpNameTextView = findViewById(R.id.mp_name)
        mpConstituencyTextView = findViewById(R.id.mp_constituency)
        nextMPButton = findViewById(R.id.next_mp_button)

        // Set initial random MP
        displayRandomMP()

        // Set onClick listener for the button
        val nextMpButton = null
        nextMpButton.setOnClickListener {
            displayRandomMP()
        }
    }

    private fun displayRandomMP() {

        val randomMP = mpList[Random.nextInt(mpList.size)]
        mpNameTextView.text = randomMP.name
        mpConstituencyTextView.text = randomMP.constituency
        mpImageView.setImageResource(R.drawable.placeholder)
    }

}

private fun Nothing?.setOnClickListener(function: () -> Unit) {
    TODO("Not yet implemented")
}


