package com.dermatrace.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dermatrace.app.databinding.ActivityRegistryBinding
import com.dermatrace.app.databinding.ItemPatientBinding
import com.dermatrace.app.network.PatientRecord

class RegistryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistryBinding
    private val patientList = mutableListOf<PatientRecord>()
    private lateinit var adapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadSamplePatients()
    }

    private fun setupRecyclerView() {
        adapter = PatientAdapter(patientList)
        binding.rvPatients.layoutManager = LinearLayoutManager(this)
        binding.rvPatients.adapter = adapter
    }

    private fun loadSamplePatients() {
        patientList.clear()
        patientList.add(PatientRecord(1, "alex.morgan@health.org", "client", 4, "2026-10-08", "Stable", false))
        patientList.add(PatientRecord(2, "sarah.chen@clinic.com", "client", 2, "2026-10-07", "Review Recommended", true))
        patientList.add(PatientRecord(3, "david.miller@telemed.io", "client", 6, "2026-10-05", "Stable", false))
        patientList.add(PatientRecord(4, "elena.rostova@derm.net", "client", 1, "2026-10-02", "Routine Follow-up", false))
        adapter.notifyDataSetChanged()
    }

    class PatientAdapter(private val items: List<PatientRecord>) :
        RecyclerView.Adapter<PatientAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemPatientBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val patient = items[position]
            holder.binding.tvPatientEmail.text = patient.email
            holder.binding.tvPatientDetails.text = "Lesion Count: ${patient.lesionCount} • Last Scan: ${patient.latestScanDate}"
            holder.binding.tvPatientTriageBadge.text = patient.status

            if (patient.urgent) {
                holder.binding.tvPatientTriageBadge.setBackgroundColor(holder.itemView.context.getColor(R.color.urgent_red_bg))
                holder.binding.tvPatientTriageBadge.setTextColor(holder.itemView.context.getColor(R.color.urgent_red))
            } else {
                holder.binding.tvPatientTriageBadge.setBackgroundColor(holder.itemView.context.getColor(R.color.safe_green_bg))
                holder.binding.tvPatientTriageBadge.setTextColor(holder.itemView.context.getColor(R.color.safe_green))
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
