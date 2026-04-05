package io.github.mevoc.familybeacon.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.util.Contact
import io.github.mevoc.familybeacon.util.ContactStore

class WhitelistActivity : AppCompatActivity() {

    private lateinit var store: ContactStore
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)

        store = ContactStore(this)
        container = findViewById(R.id.contactListContainer)

        val editName = findViewById<EditText>(R.id.editName)
        val editNumber = findViewById<EditText>(R.id.editNumber)
        val cbAddPosition = findViewById<CheckBox>(R.id.cbAddPosition)
        val cbAddBattery = findViewById<CheckBox>(R.id.cbAddBattery)
        val cbAddPanic = findViewById<CheckBox>(R.id.cbAddPanic)
        val cbAddGeofence = findViewById<CheckBox>(R.id.cbAddGeofence)

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = editName.text?.toString()?.trim().orEmpty()
            val number = store.normalize(editNumber.text?.toString().orEmpty())
            if (number.isEmpty()) {
                Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val contact = Contact(
                name = name,
                number = number,
                canRequestPosition = cbAddPosition.isChecked,
                receiveBattery = cbAddBattery.isChecked,
                canRequestPanic = cbAddPanic.isChecked,
                receiveGeofence = cbAddGeofence.isChecked
            )
            store.add(contact)
            EventLogger.info(this, "WHITELIST", "Added ${name.ifEmpty { number }} ($number)")
            editName.setText("")
            editNumber.setText("")
            cbAddPosition.isChecked = true
            cbAddBattery.isChecked = true
            cbAddPanic.isChecked = true
            cbAddGeofence.isChecked = true
            refreshList()
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            store.save(emptyList())
            EventLogger.warn(this, "WHITELIST", "Cleared all contacts")
            refreshList()
        }

        refreshList()
    }

    private fun refreshList() {
        container.removeAllViews()
        val contacts = store.getAll()
        if (contacts.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.whitelist_empty)
                textSize = 14f
            }
            container.addView(empty)
        } else {
            contacts.forEach { renderContact(it) }
        }
    }

    private fun renderContact(contact: Contact) {
        val row = layoutInflater.inflate(R.layout.item_contact, container, false)

        row.findViewById<TextView>(R.id.contactName).text =
            contact.name.ifEmpty { "—" }
        row.findViewById<TextView>(R.id.contactNumber).text = contact.number

        val cbPos = row.findViewById<CheckBox>(R.id.cbPosition)
        val cbBat = row.findViewById<CheckBox>(R.id.cbBattery)
        val cbPanic = row.findViewById<CheckBox>(R.id.cbPanic)
        val cbGeo = row.findViewById<CheckBox>(R.id.cbGeofence)

        cbPos.isChecked = contact.canRequestPosition
        cbBat.isChecked = contact.receiveBattery
        cbPanic.isChecked = contact.canRequestPanic
        cbGeo.isChecked = contact.receiveGeofence

        fun saveUpdate() {
            store.update(contact.copy(
                canRequestPosition = cbPos.isChecked,
                receiveBattery = cbBat.isChecked,
                canRequestPanic = cbPanic.isChecked,
                receiveGeofence = cbGeo.isChecked
            ))
        }

        cbPos.setOnCheckedChangeListener { _, _ -> saveUpdate() }
        cbBat.setOnCheckedChangeListener { _, _ -> saveUpdate() }
        cbPanic.setOnCheckedChangeListener { _, _ -> saveUpdate() }
        cbGeo.setOnCheckedChangeListener { _, _ -> saveUpdate() }

        row.findViewById<Button>(R.id.btnRemoveContact).setOnClickListener {
            store.remove(contact.number)
            EventLogger.info(this, "WHITELIST", "Removed ${contact.name.ifEmpty { contact.number }}")
            refreshList()
        }

        container.addView(row)
    }
}
