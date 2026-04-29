package com.personaltrainer.exporter

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = getString(R.string.health_permissions_rationale_title)
            textSize = 24f
        }

        val body = TextView(this).apply {
            text = getString(R.string.health_permissions_rationale_body)
            textSize = 16f
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(body)
        setContentView(layout)
    }
}
