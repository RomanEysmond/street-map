package com.example.example.offline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.example.databinding.ItemOfflineAreaBinding
import com.example.example.domain.model.OfflineArea
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class OfflineAreasAdapter(
    private val onDeleteClicked: (OfflineArea) -> Unit
) : ListAdapter<OfflineArea, OfflineAreasAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfflineAreaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClicked)
    }

    class ViewHolder(private val binding: ItemOfflineAreaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(area: OfflineArea, onDeleteClicked: (OfflineArea) -> Unit) {
            binding.areaName.text = area.name
            binding.areaBoundingBox.text = String.format(
                Locale.getDefault(),
                "N %.4f  S %.4f  E %.4f  W %.4f",
                area.boundingBox.north, area.boundingBox.south, area.boundingBox.east, area.boundingBox.west
            )
            val sizeMb = area.estimatedSizeBytes / (1024.0 * 1024.0)
            val createdAt = DateFormat.getDateTimeInstance().format(Date(area.createdAtMillis))
            binding.areaDetails.text = String.format(
                Locale.getDefault(),
                "%d tiles · ~%.1f MB · %s",
                area.tileCount, sizeMb, createdAt
            )
            binding.deleteButton.setOnClickListener { onDeleteClicked(area) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OfflineArea>() {
            override fun areItemsTheSame(oldItem: OfflineArea, newItem: OfflineArea) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: OfflineArea, newItem: OfflineArea) = oldItem == newItem
        }
    }
}
