package com.personaltrainer.exporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
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

private data class OperationNotice(
    val title: String,
    val detail: String,
    val tone: NoticeTone = NoticeTone.Info,
    val inProgress: Boolean = false
)

private enum class NoticeTone {
    Info,
    Success,
    Warning,
    Error
}

@Composable
private fun ExporterApp(
    initialConfig: AppConfig,
    onSaveConfig: (AppConfig) -> Unit,
    onSyncNow: suspend () -> SyncResult,
    onScheduleDaily: () -> Unit,
    onCancelSchedule: () -> Unit,
    getHealthState: suspend () -> HealthState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backendUrl by remember { mutableStateOf(initialConfig.backendUrl) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var notice by remember {
        mutableStateOf(
            OperationNotice(
                title = "Initialisation",
                detail = "Verification de Health Connect...",
                inProgress = true
            )
        )
    }
    var syncInProgress by remember { mutableStateOf(false) }
    var permissionInProgress by remember { mutableStateOf(false) }
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

    fun showNotice(title: String, detail: String, tone: NoticeTone = NoticeTone.Info, inProgress: Boolean = false) {
        notice = OperationNotice(
            title = title,
            detail = detail,
            tone = tone,
            inProgress = inProgress
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        coroutineScope.launch {
            permissionInProgress = false
            healthState = getHealthState()
            if (healthState.basePermissionsGranted) {
                if (healthState.backgroundAvailable && !healthState.backgroundPermissionGranted) {
                    showNotice(
                        "Permissions principales accordees",
                        "Relance Autoriser pour activer l'arriere-plan si tu veux la sync automatique.",
                        NoticeTone.Warning
                    )
                } else {
                    showNotice(
                        "Autorisation accordee",
                        "Toutes les permissions necessaires a l'envoi manuel sont accordees.",
                        NoticeTone.Success
                    )
                }
            } else {
                val missingCount = ExporterPermissions.basePermissions.count { it !in grantedPermissions }
                showNotice(
                    "Autorisations incompletes",
                    "$missingCount permission(s) Health Connect restent manquantes.",
                    NoticeTone.Warning
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        healthState = getHealthState()
        showNotice("Health Connect", healthState.message, if (healthState.available) NoticeTone.Success else NoticeTone.Warning)
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
                        val config = AppConfig(backendUrl, apiKey).normalized()
                        onSaveConfig(config)
                        backendUrl = config.backendUrl
                        apiKey = config.apiKey
                        showNotice(
                            "URL mise a jour",
                            "Backend sauvegarde: ${config.backendUrl.ifBlank { "URL vide" }}",
                            if (config.isComplete) NoticeTone.Success else NoticeTone.Warning
                        )
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
                                showNotice("Verification", "Lecture des autorisations Health Connect...", inProgress = true)
                                healthState = getHealthState()
                                showNotice(
                                    "Etat Health Connect",
                                    healthState.message,
                                    if (healthState.available) NoticeTone.Success else NoticeTone.Warning
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Verifier")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                permissionInProgress = true
                                showNotice(
                                    "Autorisation",
                                    "Preparation de la demande Health Connect...",
                                    inProgress = true
                                )
                                healthState = getHealthState()
                                if (!healthState.available) {
                                    permissionInProgress = false
                                    showNotice("Health Connect indisponible", healthState.message, NoticeTone.Warning)
                                    return@launch
                                }

                                val client = HealthConnectClient.getOrCreate(context)
                                val granted = client.permissionController.getGrantedPermissions()
                                val missingBase = ExporterPermissions.basePermissions - granted
                                val missingBackground =
                                    healthState.backgroundAvailable &&
                                        PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND !in granted
                                val requestedPermissions = when {
                                    missingBase.isNotEmpty() -> missingBase
                                    missingBackground -> setOf(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
                                    else -> emptySet()
                                }

                                if (requestedPermissions.isEmpty()) {
                                    permissionInProgress = false
                                    showNotice(
                                        "Autorisations deja accordees",
                                        "Les permissions Health Connect necessaires sont deja actives.",
                                        NoticeTone.Success
                                    )
                                    return@launch
                                }

                                showNotice(
                                    "Ouverture Health Connect",
                                    "Valide les permissions demandees, puis reviens dans l'app.",
                                    inProgress = true
                                )

                                try {
                                    permissionLauncher.launch(requestedPermissions)
                                } catch (error: Throwable) {
                                    permissionInProgress = false
                                    showNotice(
                                        "Autorisation impossible",
                                        error.message ?: "Health Connect n'a pas pu ouvrir l'ecran de permissions.",
                                        NoticeTone.Error
                                    )
                                }
                            }
                        },
                        enabled = !permissionInProgress,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (permissionInProgress) "Ouverture..." else "Autoriser")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Synchronisation", fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        val config = AppConfig(backendUrl, apiKey).normalized()
                        onSaveConfig(config)
                        backendUrl = config.backendUrl
                        apiKey = config.apiKey
                        coroutineScope.launch {
                            if (!config.isComplete) {
                                showNotice(
                                    "Configuration incomplete",
                                    "Verifie l'URL du backend et la cle API avant l'envoi.",
                                    NoticeTone.Warning
                                )
                                return@launch
                            }

                            syncInProgress = true
                            showNotice(
                                "Controle avant envoi",
                                "Verification des permissions et preparation des donnees Health Connect...",
                                inProgress = true
                            )
                            healthState = getHealthState()
                            if (!healthState.available || !healthState.basePermissionsGranted) {
                                syncInProgress = false
                                showNotice(
                                    "Envoi bloque",
                                    if (!healthState.available) healthState.message else "Accorde les permissions Health Connect avant de synchroniser.",
                                    NoticeTone.Warning
                                )
                                return@launch
                            }

                            try {
                                val result = onSyncNow()
                                showNotice(
                                    "Donnees envoyees",
                                    "${result.metricCount} metrique(s) envoyee(s) pour ${result.date}.",
                                    NoticeTone.Success
                                )
                            } catch (error: Throwable) {
                                showNotice(
                                    "Erreur d'envoi",
                                    error.message ?: "La synchronisation a echoue.",
                                    NoticeTone.Error
                                )
                            } finally {
                                syncInProgress = false
                            }
                        }
                    },
                    enabled = backendUrl.isNotBlank() && apiKey.isNotBlank() && !syncInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (syncInProgress) "Envoi en cours..." else "Synchroniser hier maintenant")
                }

                Button(
                    onClick = {
                        val config = AppConfig(backendUrl, apiKey).normalized()
                        onSaveConfig(config)
                        backendUrl = config.backendUrl
                        apiKey = config.apiKey
                        coroutineScope.launch {
                            if (!config.isComplete) {
                                showNotice(
                                    "Configuration incomplete",
                                    "Verifie l'URL du backend et la cle API avant de planifier l'envoi.",
                                    NoticeTone.Warning
                                )
                                return@launch
                            }

                            showNotice("Planification", "Verification de la permission arriere-plan...", inProgress = true)
                            healthState = getHealthState()
                            if (!healthState.backgroundAvailable) {
                                showNotice(
                                    "Arriere-plan indisponible",
                                    "Cette version de Health Connect ne permet pas la lecture en arriere-plan. Utilise la sync manuelle.",
                                    NoticeTone.Warning
                                )
                            } else if (!healthState.backgroundPermissionGranted) {
                                showNotice(
                                    "Permission arriere-plan manquante",
                                    "Appuie sur Autoriser pour demander la lecture en arriere-plan.",
                                    NoticeTone.Warning
                                )
                            } else {
                                onScheduleDaily()
                                showNotice(
                                    "Sync planifiee",
                                    "Synchronisation quotidienne active autour de 08:20.",
                                    NoticeTone.Success
                                )
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
                        showNotice("Planification annulee", "La synchronisation automatique est desactivee.", NoticeTone.Info)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler la planification")
                }
            }
        }

        OperationNoticeCard(notice)
    }
}

@Composable
private fun OperationNoticeCard(notice: OperationNotice) {
    val accent = when (notice.tone) {
        NoticeTone.Info -> Color(0xFF167982)
        NoticeTone.Success -> Color(0xFF2E7D4F)
        NoticeTone.Warning -> Color(0xFFB45309)
        NoticeTone.Error -> Color(0xFFB42318)
    }
    val background = when (notice.tone) {
        NoticeTone.Info -> Color(0xFFE7F7F8)
        NoticeTone.Success -> Color(0xFFEAF7EF)
        NoticeTone.Warning -> Color(0xFFFFF7E8)
        NoticeTone.Error -> Color(0xFFFFECEA)
    }
    val label = when (notice.tone) {
        NoticeTone.Info -> "Info"
        NoticeTone.Success -> "OK"
        NoticeTone.Warning -> "A verifier"
        NoticeTone.Error -> "Erreur"
    }

    Card(colors = CardDefaults.cardColors(containerColor = background)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(112.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accent, CircleShape)
                    )
                    Text(label, color = accent, fontWeight = FontWeight.Bold)
                }
                Text(notice.title, fontWeight = FontWeight.Bold)
                Text(notice.detail, color = Color(0xFF39465A))
                if (notice.inProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accent)
                }
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
