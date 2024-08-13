package com.example.medisync

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.os.Build
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

class MainActivity : AppCompatActivity(), FeedbackReceiver {

    private lateinit var tvSelectedTime: TextView
    private lateinit var etMedicineName: EditText
    private lateinit var etDosage: EditText
    private lateinit var btnTimePicker: Button
    private lateinit var btnScheduleNotification: Button
    private var selectedTimeInMillis: Long = 0

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                true
            }
            R.id.action_list -> {
                val intent = Intent(this, MedicineListActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSelectedTime = findViewById(R.id.tv_selected_time)
        etMedicineName = findViewById(R.id.et_medicine_name)
        etDosage = findViewById(R.id.et_dosage)
        btnTimePicker = findViewById(R.id.btn_time_picker)
        btnScheduleNotification = findViewById(R.id.btn_schedule_notification)

        createNotificationChannel()

        btnTimePicker.setOnClickListener {
            showTimePickerDialog()
        }

        btnScheduleNotification.setOnClickListener {
            if (selectedTimeInMillis > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(this)) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } else {
                    try {
                        val medicineName = etMedicineName.text.toString()
                        val dosage = etDosage.text.toString()

                        val details = """
                            medicine_name: $medicineName
                            dosage: $dosage
                            intake_time: $selectedTimeInMillis
                        """.trimIndent()

                        scheduleNotification(this, selectedTimeInMillis, details)
                        Toast.makeText(this, "$medicineName added to your medication schedule", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, MedicineListActivity::class.java).apply {
                            putExtra("details", details )
                        }
                        startActivity(intent)

                    } catch (e: SecurityException) {
                        Toast.makeText(this, "Permission denied to schedule notification", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                calendar.set(Calendar.SECOND, 0)
                selectedTimeInMillis = calendar.timeInMillis

                if (selectedTimeInMillis <= System.currentTimeMillis()) {
                    calendar.timeInMillis = System.currentTimeMillis() + 60000
                    selectedTimeInMillis = calendar.timeInMillis
                }

                val adjustedHour = calendar.get(Calendar.HOUR_OF_DAY)
                val adjustedMinute = calendar.get(Calendar.MINUTE)
                tvSelectedTime.text = "Selected Time: $adjustedHour:$adjustedMinute"
            }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "medication_reminder_channel"
            val channelName = "MediSync Reminder"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotification(context: Context, timeInMillis: Long, details: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
            throw SecurityException("Permission denied to schedule notification")
        }


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("details", details )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun receiveFeedback(feedback: String) {
        Toast.makeText(this, "You have rated us: $feedback", Toast.LENGTH_SHORT).show()
    }
}
