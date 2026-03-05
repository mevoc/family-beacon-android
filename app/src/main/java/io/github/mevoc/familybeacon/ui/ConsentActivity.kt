package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R

class ConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Om samtycke redan finns → gå direkt till Main
        if (Prefs.isConsentGiven(this)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_consent)

        val checkbox = findViewById<CheckBox>(R.id.checkboxConsent)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        btnAccept.isEnabled = false

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
        }

        btnAccept.setOnClickListener {
            Prefs.setConsentGiven(this, true)
            goToMain()
        }

        btnDecline.setOnClickListener {
            finishAffinity() // stänger appen helt
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}