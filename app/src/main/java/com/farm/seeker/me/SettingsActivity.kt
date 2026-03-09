package com.farm.seeker.me

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.farm.seeker.R
import com.farm.seeker.solana.SolanaManager
import com.farm.seeker.utils.SwipeBackGestureListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var solanaManager: SolanaManager
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        gestureDetector = GestureDetector(this, SwipeBackGestureListener(this))

        solanaManager = SolanaManager(this)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.btn_logout).setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        solanaManager.logout()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && gestureDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}