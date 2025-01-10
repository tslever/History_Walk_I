package com.history_walk.history_walk_i.ui.screens

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.history_walk.history_walk_i.ui.components.MapWithPathAndCircle
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val stepCounts by viewModel.stepCounts.observeAsState(emptyMap())
    val currentStepCount = stepCounts[episodeId] ?: 0

    MapWithPathAndCircle(episodeId)
    if (hasPermission) {
        Text(text = "Steps: $currentStepCount")
    } else {
        Text("Activity recognition permission is required to track your steps.")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                // TODO: Inform the user that permission is required using a Dialog.
            }
        }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val stepSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }
    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    val currentViewModel by rememberUpdatedState(newValue = viewModel)
    val stepListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
                    val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        powerManager.isInteractive
                    } else {
                        powerManager.isScreenOn
                    }
                    if (isScreenOn) {
                        viewModel.incrementStepCount(episodeId)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }
        }
    }
    DisposableEffect(key1 = hasPermission, key2 = stepSensor) {
        if (hasPermission && stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // TODO: Notify user that permission is denied or step sensor is unavailable.
        }
        onDispose {
            sensorManager.unregisterListener(stepListener)
        }
    }
}