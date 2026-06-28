package kinjouj.app.gorotate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Surface

class RotateSensorService : Service(), SensorEventListener {

    companion object {
        private const val GRAVITY_FILTER_ALPHA = 0.8f
        private const val GRAVITY_THRESHOLD = 5.0f
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "rotate_sensor"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gravityX = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        gravityX = GRAVITY_FILTER_ALPHA * gravityX + (1 - GRAVITY_FILTER_ALPHA) * event.values[0]

        if (!Settings.System.canWrite(this)) return

        val currentRotation = Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0)
        if (currentRotation != Surface.ROTATION_90 && currentRotation != Surface.ROTATION_270) return
        if (Math.abs(gravityX) < GRAVITY_THRESHOLD) return

        val suggestedRotation = if (gravityX >= 0) Surface.ROTATION_90 else Surface.ROTATION_270
        if (suggestedRotation != currentRotation) {
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, suggestedRotation)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "画面回転センサー",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("横向き自動補正が有効です")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}
