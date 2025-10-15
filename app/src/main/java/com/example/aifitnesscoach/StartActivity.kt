package com.example.aifitnesscoach

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User is signed in, check if they have a plan
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val hasGeneratedPlan = sharedPreferences.getBoolean("has_generated_plan", false)

                if (hasGeneratedPlan) {
                    // If they have a plan, go to Home
                    startActivity(Intent(this, HomeActivity::class.java))
                } else {
                    // If they don't have a plan, start the photo instructions
                    startActivity(Intent(this, PhotoInstructionsActivity::class.java))
                }
            } else {
                // No user is signed in, go to Login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2000) // 2 seconds delay
    }
}