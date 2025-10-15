package com.example.aifitnesscoach

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aifitnesscoach.databinding.ActivityDayExercisesBinding

class DayExercisesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDayExercisesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayExercisesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dayTitle = intent.getStringExtra("DAY_TITLE")
        val exercises = intent.getStringArrayListExtra("EXERCISES_LIST") ?: arrayListOf()

        binding.dayTitleTextView.text = dayTitle

        setupRecyclerView(exercises)
    }

    private fun setupRecyclerView(exercises: List<String>) {
        val adapter = ExerciseListAdapter(exercises) { exerciseName ->
            // When an exercise is clicked, start the WorkoutActivity
            val intent = Intent(this, WorkoutActivity::class.java).apply {
                // You can pass data to WorkoutActivity if needed, e.g., the exercise name
                putExtra("EXERCISE_NAME", exerciseName)
            }
            startActivity(intent)
        }
        binding.exercisesRecyclerView.adapter = adapter
        binding.exercisesRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}