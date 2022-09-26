package com.rrat.distancetrackerapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.rrat.distancetrackerapp.R
import com.rrat.distancetrackerapp.utils.Permissions


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.nav_host_fragment)

        if(Permissions.hasLocationPermission(this)){
            navController.navigate(R.id.action_permissionFragment_to_mapsFragment)
        }
    }
}