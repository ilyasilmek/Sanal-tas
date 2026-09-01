package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalStats
import com.example.data.repository.RockRepository
import com.example.data.security.TapRateLimiter
import com.example.data.sync.RegistrationResult
import com.example.data.sync.SyncServerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RockRepository(application, viewModelScope)
    private val rateLimiter = TapRateLimiter()

    val localStats: StateFlow<LocalStats?> = repository.localStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocalStats(1, 0, 0, System.currentTimeMillis())
        )

    val serverState: StateFlow<SyncServerState> = repository.serverState

    private val _currentCps = MutableStateFlow(0)
    val currentCps: StateFlow<Int> = _currentCps.asStateFlow()

    private val _isLeaderboardOpen = MutableStateFlow(false)
    val isLeaderboardOpen: StateFlow<Boolean> = _isLeaderboardOpen.asStateFlow()

    private val _isArchitectureOpen = MutableStateFlow(false)
    val isArchitectureOpen: StateFlow<Boolean> = _isArchitectureOpen.asStateFlow()

    private val _rateLimitWarning = MutableStateFlow(false)
    val rateLimitWarning: StateFlow<Boolean> = _rateLimitWarning.asStateFlow()

    // Registration and Onboarding States
    private val _isUserRegistered = MutableStateFlow(repository.isUserRegistered())
    val isUserRegistered: StateFlow<Boolean> = _isUserRegistered.asStateFlow()

    private val _isCheckingName = MutableStateFlow(false)
    val isCheckingName: StateFlow<Boolean> = _isCheckingName.asStateFlow()

    private val _nameErrorMessage = MutableStateFlow<String?>(null)
    val nameErrorMessage: StateFlow<String?> = _nameErrorMessage.asStateFlow()

    private val _username = MutableStateFlow(repository.getUsername() ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    val userId: String = repository.getUserId()
    val countryCode: String = repository.getCountryCode()

    private var cpsPollJob: Job? = null

    init {
        startCpsMonitor()
    }

    private fun startCpsMonitor() {
        cpsPollJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(200)
                val cps = rateLimiter.getCurrentCps()
                _currentCps.value = cps
            }
        }
    }

    fun registerUser(name: String, country: String) {
        _isCheckingName.value = true
        _nameErrorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.registerUser(name, country)
            _isCheckingName.value = false
            when (result) {
                is RegistrationResult.Success -> {
                    _username.value = name.trim()
                    _isUserRegistered.value = true
                    _nameErrorMessage.value = null
                }
                is RegistrationResult.Error -> {
                    _nameErrorMessage.value = result.message
                }
            }
        }
    }

    fun onTapRock() {
        val accepted = rateLimiter.tryTap()
        if (!accepted) {
            _rateLimitWarning.value = true
            viewModelScope.launch {
                delay(800)
                _rateLimitWarning.value = false
            }
            return
        }

        _rateLimitWarning.value = false
        triggerHapticFeedback()

        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementClick()
        }

        _currentCps.value = rateLimiter.getCurrentCps()
    }

    private fun triggerHapticFeedback() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic failure on test environments
        }
    }

    fun openLeaderboard() {
        _isLeaderboardOpen.value = true
    }

    fun closeLeaderboard() {
        _isLeaderboardOpen.value = false
    }

    fun openArchitecture() {
        _isArchitectureOpen.value = true
    }

    fun closeArchitecture() {
        _isArchitectureOpen.value = false
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.flushUnsyncedClicks()
        }
    }

    fun updateServerUrl(url: String) {
        repository.cloudSync.updateServerUrl(url)
    }
}
