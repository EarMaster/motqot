package rocks.wiedemann.motqot

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import rocks.wiedemann.motqot.databinding.ActivityMainBinding
import rocks.wiedemann.motqot.viewmodel.MainViewModel
import java.util.Date

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    // Request notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(this, R.string.notification_permission_granted, Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
        
        // Check for notification permission
        checkNotificationPermission()
    }
    
    // Function to check notification permission and request if needed
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show educational UI explaining why we need this permission
                    Toast.makeText(
                        this,
                        R.string.notification_permission_rationale,
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly ask for the permission
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if API key is set
        if (!viewModel.isApiKeySet()) {
            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun setupObservers() {
        viewModel.quote.observe(this) { quote ->
            if (quote != null) {
                binding.tvQuote.text = quote.text
                binding.tvDate.text = viewModel.formatDateForDisplay(quote.date)
            } else {
                binding.tvQuote.text = getString(R.string.no_quote_available)
                binding.tvDate.text = viewModel.formatDateForDisplay(Date())
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.tvQuote.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
        
        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnGenerateQuote.setOnClickListener {
            if (viewModel.isApiKeySet()) {
                viewModel.generateQuote()
            } else {
                Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        
        binding.btnShareQuote.setOnClickListener {
            val quote = viewModel.quote.value
            if (quote != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, quote.text)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_quote)))
            }
        }
    }
}
