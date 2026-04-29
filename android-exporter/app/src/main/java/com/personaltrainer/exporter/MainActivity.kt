package com.personaltrainer.exporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configStore = ConfigStore(this)
        val syncUseCase = SyncUseCase(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF167982),
                    secondary = Color(0xFF7C5CE6),
                    background = Color(0xFFF5F8FB),
                    surface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ExporterApp(
                        initialConfig = configStore.read(),
                        onSaveConfig = configStore::save,
                        onSyncNow = { syncUseCase.syncYesterday() },
                        onScheduleDaily = { SyncScheduler.scheduleDaily(this) },
                        onCancelSchedule = { SyncScheduler.cancel(this) },
                        getHealthState = ::readHealthState
                    )
                }
            }
        }
    }

    private suspend fun readHealthState(): HealthState {
        val status = HealthConnectClient.getSdkStatus(this)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            return HealthState(
                available = false,
                basePermissionsGranted = false,
                backgroundPermissionGranted = false,
                backgroundAvailable = false,
                message = when (status) {
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                        "Health Connect doit etre installe ou mis a jour depuis Google Play."
                    else -> "Health Connect n'est pas disponible sur cet appareil."
                }
            )
        }

        val client = HealthConnectClient.getOrCreate(this)
        val granted = client.permissionController.getGrantedPermissions()
        val backgroundAvailable = ExporterPermissions.backgroundReadAvailable(client)

        return HealthState(
            available = true,
            basePermissionsGranted = granted.containsAll(ExporterPermissions.basePermissions),
            backgroundPermissionGranted = PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted,
            backgroundAvailable = backgroundAvailable,
            message = "Health Connect disponible."
        )
    }
}

data class HealthState(
    val available: Boolean,
    val basePermissionsGranted: Boolean,
    val backgroundPermissionGranted: Boolean,
    val backgroundAvailable: Boolean,
    val message: String
)

@Composable
private fun ExporterApp(
    initialConfig: AppConfig,
    onSaveConfig: (AppConfig) -> Unit,
    onSyncNow: suspend () -> String,
    onScheduleDaily: () -> Unit,
    onCancelSchedule: () -> Unit,
    getHealthState: suspend () -> HealthState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backendUrl by remember { mutableStateOf(initialConfig.backendUrl) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var status by remember { mutableStateOf("Initialisation...") }
    var healthState by remember {
        mutableStateOf(
            HealthState(
                available = false,
                basePermissionsGranted = false,
                backgroundPermissionGranted = false,
                backgroundAvailable = false,
                message = "Verification Health Connect..."
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        coroutineScope.launch {
            healthState = getHealthState()
            status = if (healthState.basePermissionsGranted) {
                "Permissions Health Connect accordees."
            } else {
                "Certaines permissions Health Connect manquent encore."
            }
        }
    }

    LaunchedEffect(Unit) {
        healthState = getHealthState()
        status = healthState.message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Personal Trainer Exporter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(
            "Lit Health Connect, agrege la veille et envoie les donnees vers ton backend.",
            color = Color(0xFF617089)
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configuration backend", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Backend URL") },
                    placeholder = { Text("https://ton-backend.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                Text(
                    "Sur un telephone physique, n'utilise pas localhost. Utilise l'URL publique du backend ou l'adresse LAN/tunnel.",
                    color = Color(0xFF617089),
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        onSaveConfig(AppConfig(backendUrl, apiKey))
                        status = "Configuration sauvegardee."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sauvegarder")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Health Connect", fontWeight = FontWeight.Bold)
                StatusLine("Disponible", healthState.available)
                StatusLine("Permissions donnees", healthState.basePermissionsGranted)
                StatusLine("Lecture arriere-plan disponible", healthState.backgroundAvailable)
                StatusLine("Permission arriere-plan", healthState.backgroundPermissionGranted)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                healthState = getHealthState()
                                status = healthState.message
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Verifier")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val statusNow = getHealthState()
                                if (!statusNow.available) {
                                    status = statusNow.message
                                    return@launch
                                }
                                val client = HealthConnectClient.getOrCreate(context)
                                permissionLauncher.launch(ExporterPermissions.requestedPermissions(client))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Autoriser")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Synchronisation", fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        onSaveConfig(AppConfig(backendUrl, apiKey))
                        coroutineScope.launch {
                            status = "Synchronisation en cours..."
                            status = try {
                                onSyncNow()
                                "Synchronisation envoyee avec succes."
                            } catch (error: Throwable) {
                                "Erreur sync: ${error.message}"
                            }
                        }
                    },
                    enabled = backendUrl.isNotBlank() && apiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Synchroniser hier maintenant")
                }

                Button(
                    onClick = {
                        onSaveConfig(AppConfig(backendUrl, apiKey))
                        coroutineScope.launch {
                            healthState = getHealthState()
                            if (!healthState.backgroundAvailable) {
                                status = "Lecture en arriere-plan indisponible sur cette version de Health Connect. Utilise la sync manuelle."
                            } else if (!healthState.backgroundPermissionGranted) {
                                status = "Accorde d'abord la permission de lecture en arriere-plan."
                            } else {
                                onScheduleDaily()
                                status = "Sync quotidienne planifiee autour de 08:20."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Planifier chaque matin")
                }

                OutlinedButton(
                    onClick = {
                        onCancelSchedule()
                        status = "Planification annulee."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler la planification")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Statut", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(status)
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    val color = if (enabled) Color(0xFF2E7D4F) else Color(0xFF9A3412)
    Text(
        text = "${if (enabled) "OK" else "--"}  $label",
        color = color,
        fontWeight = FontWeight.SemiBold
    )
}
