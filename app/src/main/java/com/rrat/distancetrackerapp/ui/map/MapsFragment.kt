package com.rrat.distancetrackerapp.ui.map

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rrat.distancetrackerapp.R
import com.rrat.distancetrackerapp.databinding.FragmentMapsBinding
import com.rrat.distancetrackerapp.model.Result
import com.rrat.distancetrackerapp.service.TrackerService
import com.rrat.distancetrackerapp.ui.map.MapUtil.calculateElapsedTime
import com.rrat.distancetrackerapp.ui.map.MapUtil.calculateTheDistance
import com.rrat.distancetrackerapp.ui.map.MapUtil.setCameraPosition
import com.rrat.distancetrackerapp.utils.Constants.ACTION_SERVICE_START
import com.rrat.distancetrackerapp.utils.Constants.ACTION_SERVICE_STOP
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.disable
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.enabled
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.hide
import com.rrat.distancetrackerapp.utils.ExtensionFunctions.show
import com.rrat.distancetrackerapp.utils.Permissions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Compiler.enable


class MapsFragment : Fragment(),
    OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks
{

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!


    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetButton.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }



    private fun onStopButtonClicked() {
        stopForegroundService()
        binding.stopButton.hide()
        binding.startButton.show()
    }

    private fun onResetButtonClicked() {
        mapReset()
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

    private fun stopForegroundService() {
        binding.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
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

        observerTrackerService()
    }

    private fun observerTrackerService(){
        TrackerService.locationList.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it
                if(locationList.size > 1){
                    binding.stopButton.enabled()
                }
                drawPolyline()
                followPolyline()
            }
        }
        TrackerService.started.observe(viewLifecycleOwner){
            started.value = it
        }

        TrackerService.startTime.observe(viewLifecycleOwner){
            startTime = it
        }

        TrackerService.stopTime.observe(viewLifecycleOwner){
            stopTime = it
            if(stopTime != 0L){
                showBiggerPicture()
                displayResult()
            }
        }
    }

    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for(location in locationList){
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(), 100
            ), 2000, null
        )
    }

    private fun displayResult(){
        val result = Result(
            calculateTheDistance(locationList),
            calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.startButton.apply {
                hide()
                enable()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(it.result.latitude, it.result.longitude)
            for (polyline in polylineList){
                polyline.remove()
            }

            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            locationList.clear()
            binding.resetButton.hide()
            binding.startButton.show()
        }
    }


    private fun drawPolyline(){
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }

    private fun followPolyline(){
        if(locationList.isNotEmpty()){
            map.animateCamera((CameraUpdateFactory.newCameraPosition(
                MapUtil.setCameraPosition(locationList.last())
            )), 1000, null)
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