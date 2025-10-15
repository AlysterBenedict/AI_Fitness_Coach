package com.example.aifitnesscoach

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aifitnesscoach.databinding.ActivityOnboardingFormBinding
import com.example.aifitnesscoach.network.RetrofitClient
import com.example.aifitnesscoach.network.UserData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class OnboardingFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingFormBinding
    private var biometricsData: Map<String, Float>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        handleIntentData()

        binding.generatePlanButton.setOnClickListener {
            if (validateForm()) {
                generateWorkoutPlan()
            }
        }
    }

    private fun handleIntentData() {
        // Retrieve and parse the biometrics data
        val biometricsJson = intent.getStringExtra("BIOMETRICS_DATA")
        if (biometricsJson != null) {
            val type = object : TypeToken<Map<String, Float>>() {}.type
            biometricsData = Gson().fromJson(biometricsJson, type)
        }

        // Retrieve and display the captured images
        val frontalImageUriString = intent.getStringExtra("FRONTAL_IMAGE_URI")
        val sideImageUriString = intent.getStringExtra("SIDE_IMAGE_URI")

        frontalImageUriString?.let { binding.frontalImageView.setImageURI(Uri.parse(it)) }
        sideImageUriString?.let { binding.sideImageView.setImageURI(Uri.parse(it)) }
    }

    private fun setupSpinners() {
        // Setup Goal Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.fitness_goals,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.goalSpinner.adapter = adapter
        }

        // Setup Difficulty Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.fitness_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.difficultySpinner.adapter = adapter
        }
    }

    private fun validateForm(): Boolean {
        if (binding.ageEditText.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show()
            return false
        }
        if (biometricsData == null) {
            Toast.makeText(this, "Biometric data is missing.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun generateWorkoutPlan() {
        binding.progressBar.visibility = View.VISIBLE
        // --- 1. Gather all data ---
        val age = binding.ageEditText.text.toString().toInt()
        val gender = if (binding.maleRadioButton.isChecked) "Male" else "Female"
        val goal = binding.goalSpinner.selectedItem.toString()
        val difficulty = binding.difficultySpinner.selectedItem.toString()

        // --- 2. Use biometrics data directly and calculate BMI ---
        val height = biometricsData?.get("height_cm") ?: 0f
        val weight = biometricsData?.get("weight_kg") ?: 0f

        if (height == 0f || weight == 0f) {
            Toast.makeText(this, "Could not determine height and weight from image.", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        val chest = biometricsData?.get("chest") ?: 0f
        val waist = biometricsData?.get("waist") ?: 0f
        val hip = biometricsData?.get("hip") ?: 0f
        val thigh = biometricsData?.get("thigh") ?: 0f
        val bicep = biometricsData?.get("bicep") ?: 0f
        val bmi = (weight / ((height / 100) * (height / 100)))

        // --- 3. Create the UserData object ---
        val userData = UserData(
            age = age,
            gender = gender,
            heightCm = height,
            weightKg = weight,
            goal = goal,
            level = difficulty,
            bmi = bmi,
            chestCm = chest,
            waistCm = waist,
            hipCm = hip,
            thighCm = thigh,
            bicepCm = bicep
        )

        // --- 4. Make the API call ---
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.generateWorkout(userData)
                binding.progressBar.visibility = View.GONE

                val intent = Intent(this@OnboardingFormActivity, WorkoutPlanActivity::class.java).apply {
                    putExtra("WORKOUT_PLAN", Gson().toJson(response.workoutPlan))
                }
                startActivity(intent)
                finishAffinity()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e("OnboardingFormActivity", "Error generating workout plan", e)
                Toast.makeText(this@OnboardingFormActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}