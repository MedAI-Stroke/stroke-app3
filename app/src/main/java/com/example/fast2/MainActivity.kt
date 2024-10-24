package com.example.fast2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BODY_SENSORS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 권한 체크 및 요청
        checkAndRequestPermissions()

        // ANA 버튼 설정
        val anaButtonImage: ImageView = findViewById(R.id.ANA_button_image)
        anaButtonImage.setOnClickListener {
            val intent = Intent(this, FActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
}