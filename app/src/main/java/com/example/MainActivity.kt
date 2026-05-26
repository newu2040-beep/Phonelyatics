package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.LaunchedEffect
import com.example.data.PhonelyticsDatabase
import com.example.data.PhonelyticsRepository
import com.example.ui.PhonelyticsDashboard
import com.example.viewmodel.PhonelyticsViewModel
import com.example.viewmodel.PhonelyticsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local offline-first Database & Repository
        val database = PhonelyticsDatabase.getDatabase(this)
        val repository = PhonelyticsRepository(database.diagnosticDao())

        // 2. Instantiate master View-Model via Safe factory
        val factory = PhonelyticsViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[PhonelyticsViewModel::class.java]

        setContent {
            // 3. Listen to diagnostic notifications in separate channel
            LaunchedEffect(key1 = true) {
                viewModel.toastEvent.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            // 4. Render futuristic Material 3 Expressive layout
            PhonelyticsDashboard(viewModel = viewModel)
        }
    }
}
