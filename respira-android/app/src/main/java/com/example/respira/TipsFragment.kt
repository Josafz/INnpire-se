package com.example.respira

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.respira.data.AppDatabase
import com.example.respira.data.EmergencyContact
import com.example.respira.databinding.FragmentTipsBinding
import com.example.respira.reminder.Notifier
import com.example.respira.reminder.ReminderScheduler
import com.example.respira.util.Dialer
import com.example.respira.util.Prefs
import com.example.respira.util.Sms
import kotlinx.coroutines.launch

class TipsFragment : Fragment() {

    private var _binding: FragmentTipsBinding? = null
    private val binding get() = _binding!!
    private var updatingSwitch = false  // evita loop quando a chave é mudada por código

    // Pede a permissão de notificações (Android 13+) e avisa o resultado
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (_binding == null) return@registerForActivityResult
            if (granted) {
                enableReminder()
            } else {
                setSwitch(false)
                Toast.makeText(
                    requireContext(),
                    "Sem permissão de notificações, o lembrete não pode ser ativado.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCvv.setOnClickListener { Dialer.dial(requireContext(), "188") }
        binding.btnSamu.setOnClickListener { Dialer.dial(requireContext(), "192") }

        val ctx = requireContext()
        setSwitch(Prefs.reminderEnabled(ctx) && Notifier.hasPermission(ctx))
        updateTimeLabel()

        binding.swReminder.setOnCheckedChangeListener { _, checked ->
            if (updatingSwitch) return@setOnCheckedChangeListener
            when {
                !checked -> disableReminder()
                Notifier.hasPermission(requireContext()) -> enableReminder()
                else -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        binding.btnReminderTime.setOnClickListener { pickTime() }

        binding.btnEditContacts.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyContactsActivity::class.java))
        }
        loadContacts()
    }

    override fun onResume() {
        super.onResume()
        loadContacts()   // o usuário pode ter acabado de editar os contatos e voltado para cá
    }

    // Linhas fixas (não é RecyclerView: são sempre exatamente 3 slots).
    private val contactRows by lazy {
        listOf(
            Triple(1, binding.tvContactName1, Pair(binding.btnCall1, binding.btnSms1)),
            Triple(2, binding.tvContactName2, Pair(binding.btnCall2, binding.btnSms2)),
            Triple(3, binding.tvContactName3, Pair(binding.btnCall3, binding.btnSms3))
        )
    }

    private fun loadContacts() {
        val dao = AppDatabase.getInstance(requireContext()).emergencyContactDao()
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = dao.getAll().associateBy { it.position }
            contactRows.forEach { (position, nameView, buttons) ->
                bindContactRow(position, saved[position], nameView, buttons.first, buttons.second)
            }
        }
    }

    private fun bindContactRow(
        position: Int, contact: EmergencyContact?, nameView: TextView, callButton: Button, smsButton: Button
    ) {
        if (contact == null) {
            nameView.text = "Contato $position: não configurado"
            callButton.isEnabled = false
            smsButton.isEnabled = false
            callButton.setOnClickListener(null)
            smsButton.setOnClickListener(null)
            return
        }
        nameView.text = contact.name
        callButton.isEnabled = true
        smsButton.isEnabled = true
        callButton.setOnClickListener { Dialer.dial(requireContext(), contact.phone) }
        smsButton.setOnClickListener {
            Sms.compose(requireContext(), contact.phone, "Oi, pode me ligar? Preciso conversar.")
        }
    }

    private fun enableReminder() {
        val ctx = requireContext()
        Prefs.setReminderEnabled(ctx, true)
        ReminderScheduler.schedule(ctx, Prefs.reminderHour(ctx), Prefs.reminderMinute(ctx))
        setSwitch(true)
    }

    private fun disableReminder() {
        val ctx = requireContext()
        Prefs.setReminderEnabled(ctx, false)
        ReminderScheduler.cancel(ctx)
    }

    private fun pickTime() {
        val ctx = requireContext()
        TimePickerDialog(
            ctx,
            { _, hour, minute ->
                Prefs.setReminderTime(ctx, hour, minute)
                updateTimeLabel()
                if (Prefs.reminderEnabled(ctx)) ReminderScheduler.schedule(ctx, hour, minute)
            },
            Prefs.reminderHour(ctx), Prefs.reminderMinute(ctx), true
        ).show()
    }

    private fun updateTimeLabel() {
        val ctx = requireContext()
        binding.btnReminderTime.text =
            "Horário: %02d:%02d".format(Prefs.reminderHour(ctx), Prefs.reminderMinute(ctx))
    }

    private fun setSwitch(checked: Boolean) {
        updatingSwitch = true
        binding.swReminder.isChecked = checked
        updatingSwitch = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
