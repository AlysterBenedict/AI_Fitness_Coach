package com.example.aifitnesscoach

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.aifitnesscoach.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the PlanFragment by default when the app starts
        if (savedInstanceState == null) {
            loadFragment(PlanFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_plan
        }

        // Set up the listener for the navigation bar
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_plan -> selectedFragment = PlanFragment()
                R.id.nav_reports -> selectedFragment = ReportsFragment()
                R.id.nav_me -> selectedFragment = ProfileFragment()
            }
            // If a fragment was selected, load it
            if (selectedFragment != null) {
                loadFragment(selectedFragment)
            }
            true
        }
    }

    // A helper function to swap fragments in the container
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}