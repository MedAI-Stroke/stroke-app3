package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val anaButtonImage: ImageView = findViewById(R.id.ANA_button_image)
        anaButtonImage.setOnClickListener {
            // FActivity로 전환
            val intent = Intent(this, FActivity::class.java)
            startActivity(intent)
        }
    }
}
