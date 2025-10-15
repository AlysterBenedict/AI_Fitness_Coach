package com.example.aifitnesscoach

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aifitnesscoach.databinding.ActivityOnboardingFormBinding
import com.google.gson.Gson

class OnboardingFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingFormBinding
    private var frontalImageUri: Uri? = null
    private var sideImageUri: Uri? = null
    private lateinit var workoutGenerator: WorkoutGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workoutGenerator = WorkoutGenerator(applicationContext)

        // Retrieve the image URIs passed from the camera activity
        intent.getStringExtra(CameraCaptureActivity.EXTRA_FRONTAL_IMAGE_URI)?.let {
            frontalImageUri = Uri.parse(it)
            binding.frontalImageView.setImageURI(frontalImageUri)
        }
        intent.getStringExtra(CameraCaptureActivity.EXTRA_SIDE_IMAGE_URI)?.let {
            sideImageUri = Uri.parse(it)
            binding.sideImageView.setImageURI(sideImageUri)
        }

        setupSpinners()

        binding.generatePlanButton.setOnClickListener {
            if (validateInputs()) {
                runModelInference()
            }
        }
    }

    private fun setupSpinners() {
        val levelAdapter = ArrayAdapter.createFromResource(
            this, R.array.fitness_levels, android.R.layout.simple_spinner_item
        )
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.levelSpinner.adapter = levelAdapter

        val goalAdapter = ArrayAdapter.createFromResource(
            this, R.array.fitness_goals, android.R.layout.simple_spinner_item
        )
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.goalSpinner.adapter = goalAdapter
    }

    private fun validateInputs(): Boolean {
        if (frontalImageUri == null || sideImageUri == null) {
            Toast.makeText(this, "Images are missing. Please restart.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.ageEditText.text.toString().trim().isEmpty()) {
            binding.ageInputLayout.error = "Age is required"
            return false
        } else {
            binding.ageInputLayout.error = null
        }
        if (binding.genderRadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun runModelInference() {
        binding.progressBar.visibility = View.VISIBLE
        binding.generatePlanButton.isEnabled = false

        val age = binding.ageEditText.text.toString().toInt()
        val gender = if (binding.maleRadioButton.isChecked) "Male" else "Female"
        val level = binding.levelSpinner.selectedItem.toString()
        val goal = binding.goalSpinner.selectedItem.toString()

        workoutGenerator.generatePlan(frontalImageUri!!, sideImageUri!!, age, gender, level, goal) { workoutPlan ->
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.generatePlanButton.isEnabled = true

                if (workoutPlan != null && workoutPlan.isNotEmpty()) {
                    Toast.makeText(this, "Workout Plan Generated!", Toast.LENGTH_SHORT).show()
                    saveWorkoutPlanAndNavigate(workoutPlan)
                } else {
                    Toast.makeText(this, "Failed to generate workout plan. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveWorkoutPlanAndNavigate(workoutPlan: List<String>) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            // Convert list to a JSON string for easy storage
            putString("workout_plan", Gson().toJson(workoutPlan))
            putBoolean("has_generated_plan", true)
            apply()
        }

        // Navigate to the main app screen
        val intent = Intent(this, HomeActivity::class.java).apply {
            // Clear the activity stack so the user can't go back to the onboarding process
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}