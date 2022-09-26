package com.rrat.distancetrackerapp.ui.map

import android.annotation.SuppressLint
import android.content.Intent
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.rrat.distancetrackerapp.R
import com.rrat.distancetrackerapp.databinding.FragmentMapsBinding
import com.rrat.distancetrackerapp.service.TrackerService
import com.rrat.distancetrackerapp.utils.Constants.ACTION_SERVICE_START
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.disable
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.hide
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.show
import com.rrat.distancetrackerapp.utils.Permissions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



class MapsFragment : Fragment(),
    OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks
{

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!


    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {

        }
        binding.resetButton.setOnClickListener {

        }

        return binding.root
    }

    private fun onStartButtonClicked() {
        if(Permissions.hasBackgroundLocationPermission(requireContext())){
            startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.stopButton.show()
        }else{
            Permissions.requestBackgroundLocationPermission(this)
        }
    }

    private fun startCountDown() {
        binding.timerTv.show()
        binding.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if(currentSecond.toString() == "0"){
                    binding.timerTv.text = getString(R.string.go)
                    binding.timerTv.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                }else{
                    binding.timerTv.text = currentSecond.toString()
                    binding.timerTv.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red
                    ))
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTv.hide()
            }

        }
        timer.start()
    }

    private fun sendActionCommandToService(action: String){
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            Permissions.requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTv.animate().alpha(0f).duration = 1500
        lifecycleScope.launch{
            delay(2500)
            binding.hintTv.hide()
            binding.startButton.show()
        }

        return false
    }


}