package com.example.respira

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.respira.data.AppDatabase
import com.example.respira.data.EmergencyContact
import com.example.respira.databinding.ActivityEmergencyContactsBinding
import com.example.respira.util.PhoneValidation
import kotlinx.coroutines.launch

/** Tela de cadastro dos 3 contatos de confiança (nome + telefone por slot). */
class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private val dao get() = AppDatabase.getInstance(this).emergencyContactDao()

    // (posição, campo de nome, campo de telefone) — os 3 slots, sempre na mesma ordem
    private val slots by lazy {
        listOf(
            Triple(1, binding.etName1, binding.etPhone1),
            Triple(2, binding.etName2, binding.etPhone2),
            Triple(3, binding.etName3, binding.etPhone3)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }

        lifecycleScope.launch { fillFromDatabase() }
    }

    private suspend fun fillFromDatabase() {
        val saved = dao.getAll().associateBy { it.position }
        slots.forEach { (position, nameField, phoneField) ->
            saved[position]?.let {
                nameField.setText(it.name)
                phoneField.setText(it.phone)
            }
        }
    }

    private fun save() {
        var valid = true
        for ((_, nameField, phoneField) in slots) {
            nameField.error = null
            phoneField.error = null
            val name = nameField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            when {
                name.isEmpty() && phone.isEmpty() -> Unit   // slot em branco: tudo bem
                name.isEmpty() -> { nameField.error = "Digite um nome"; valid = false }
                !PhoneValidation.isValid(phone) -> { phoneField.error = "Telefone inválido"; valid = false }
                else -> Unit
            }
        }
        if (!valid) return

        lifecycleScope.launch {
            for ((position, nameField, phoneField) in slots) {
                val name = nameField.text.toString().trim()
                val phone = phoneField.text.toString().trim()
                if (name.isEmpty() || phone.isEmpty()) {
                    dao.deleteByPosition(position)
                } else {
                    dao.upsert(EmergencyContact(position = position, name = name, phone = phone))
                }
            }
            Toast.makeText(this@EmergencyContactsActivity, "Contatos salvos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
