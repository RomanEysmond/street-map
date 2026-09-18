package com.example.example.apikey

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.example.MainActivity
import com.example.example.databinding.ActivityApiKeyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ApiKeyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiKeyBinding
    private val viewModel: ApiKeyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apiKeySaveButton.setOnClickListener {
            viewModel.onSaveClicked(binding.apiKeyInput.text.toString())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.apiKeySaveButton.isEnabled = !state.isSaving
                    binding.apiKeyErrorText.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                    binding.apiKeyErrorText.text = state.errorMessage
                    if (state.saved) {
                        startActivity(Intent(this@ApiKeyActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
