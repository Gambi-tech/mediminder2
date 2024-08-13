package com.example.medisync

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineListActivity : AppCompatActivity(), FeedbackReceiver{

    private lateinit var tableMed: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_list)

        tableMed = findViewById(R.id.table_med)

        addDummyMed()

        // Check for new entry
        val details = intent.getStringExtra("details")
        details?.let {
            addMedToTable(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_list -> {
                true
            }
            R.id.action_rate_us -> {
                val rateUsFragment = RateUsFragment()
                rateUsFragment.show(supportFragmentManager, "RateUsFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addMedToTable(details: String) {
        // Split the details string by lines
        val detailParts = details.lines()
        if (detailParts.size < 3) return

        // Extract the values from the split lines
        val medicineName = detailParts[0].substringAfter("medicine_name: ").trim()
        val dosage = detailParts[1].substringAfter("dosage: ").trim()
        val intakeTimeString = detailParts[2].substringAfter("intake_time: ").trim()

        // Convert intake_time from String to Long
        val intakeTimeMillis = intakeTimeString.toLongOrNull() ?: 0L
        // Convert the intake time from milliseconds to a human-readable time format
        val intakeTime = Date(intakeTimeMillis)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(intakeTime)

        // Create a new table row
        val tableRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        // Formatting
        val nameTextView = TextView(this).apply {
            text = medicineName
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            textSize = 18f
            // Text wrapping for thr Medicine name column
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END

            // Set the layout parameters for double space in the medicine name column
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f).apply {
                marginEnd = 22.dp
            }
        }

        val dosageTextView = TextView(this).apply {
            text = dosage
            setPadding(10.dp, 8.dp, 8.dp, 8.dp)
            textSize = 18f

            // Set layout parameters for the dosage column
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 22.dp
            }
        }

        val timeTextView = TextView(this).apply {
            text = formattedTime
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            textSize = 18f

            // Set layout parameters for the time column
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 22.dp
            }
        }

        tableRow.addView(nameTextView)
        tableRow.addView(dosageTextView)
        tableRow.addView(timeTextView)

        tableMed.addView(tableRow)
    }


    private fun addDummyMed() {
        val dummyMed = listOf(
            "Medicine 1\n1 tablet\n12:05 m",
            "Medicine 2\n15 ml\n7.15 pm",
            "Medicine 3\n2 tablets\n7.30 pm"
        )

        dummyMed.forEach { item ->
            addMedToTable(item)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun receiveFeedback(feedback: String) {
        Toast.makeText(this, "You have rated us: $feedback", Toast.LENGTH_SHORT).show()
    }

}