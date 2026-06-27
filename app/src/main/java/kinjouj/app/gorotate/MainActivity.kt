package kinjouj.app.gorotate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import kinjouj.app.gorotate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantWriteSettingsPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }

        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun updateUiState() {
        val hasPermission = Settings.System.canWrite(this)
        binding.btnGrantWriteSettingsPermission.isEnabled = !hasPermission
        binding.tvWriteSettingsPermissionStatus.text = if (hasPermission) {
            getString(R.string.write_settings_permission_granted)
        } else {
            getString(R.string.write_settings_permission_not_granted)
        }
    }
}
