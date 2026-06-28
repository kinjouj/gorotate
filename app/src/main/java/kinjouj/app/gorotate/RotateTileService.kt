package kinjouj.app.gorotate

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.Surface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

class RotateTileService : TileService(), SensorEventListener {

    companion object {
        private const val GRAVITY_FILTER_ALPHA = 0.8f
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var gravityX = 0f
    private var gravityY = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartListening() {
        super.onStartListening()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
            return
        }

        toggleRotation()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravityX = GRAVITY_FILTER_ALPHA * gravityX + (1 - GRAVITY_FILTER_ALPHA) * event.values[0]
            gravityY = GRAVITY_FILTER_ALPHA * gravityY + (1 - GRAVITY_FILTER_ALPHA) * event.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun toggleRotation() {
        try {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)

            val currentRotation = Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0)
            val nextRotation = if (currentRotation == Surface.ROTATION_90 || currentRotation == Surface.ROTATION_270) {
                Surface.ROTATION_0
            } else {
                if (gravityX >= 0) Surface.ROTATION_90 else Surface.ROTATION_270
            }

            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, nextRotation)
            vibrate()
            updateTileState()

            val sensorIntent = Intent(this, RotateSensorService::class.java)
            if (nextRotation == Surface.ROTATION_90 || nextRotation == Surface.ROTATION_270) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(sensorIntent)
                } else {
                    startService(sensorIntent)
                }
            } else {
                stopService(sensorIntent)
            }

            val messageRes = when (nextRotation) {
                Surface.ROTATION_90 -> R.string.toast_rotation_landscape_left
                Surface.ROTATION_270 -> R.string.toast_rotation_landscape_right
                else -> R.string.toast_rotation_portrait
            }

            Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.toast_no_write_settings_permission), Toast.LENGTH_LONG).show()
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.updateTile()
    }
}
