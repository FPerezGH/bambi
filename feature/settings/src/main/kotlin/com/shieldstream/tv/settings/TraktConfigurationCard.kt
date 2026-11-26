package com.shieldstream.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shieldstream.tv.domain.TraktRepository
import com.shieldstream.tv.domain.ProfileRepository
import com.shieldstream.tv.ui.component.TvButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TraktConfigurationViewModel @Inject constructor(
    private val trakt: TraktRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    val profile = profiles.active
    val configured = MutableStateFlow(trakt.isConfigured)
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    fun clientId() = trakt.configuredClientId()
    fun save(id: String, secret: String, onSaved: () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try {
                trakt.configureApplication(id, secret).fold(
                    onSuccess = { configured.value = trakt.isConfigured; onSaved() },
                    onFailure = { error.value = it.message ?: "No se pudo guardar la configuración." },
                )
            } finally { busy.value = false }
        }
    }
}

@Composable
internal fun TraktConfigurationCard(viewModel: TraktConfigurationViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    if (profile.kids) return
    val configured by viewModel.configured.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButton(text = if (configured) "Trakt · Configurado" else "Trakt · Configurar", onClick = { open = true })
        Text("Vincula tu cuenta desde Editar perfil → Trakt.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
    }
    if (open) {
        var id by remember { mutableStateOf(viewModel.clientId()) }
        var secret by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { if (!busy) open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.width(650.dp).background(Color(0xFF202024), RoundedCornerShape(16.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configurar Trakt", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("Registra CINEFRAN en trakt.tv/oauth/applications. Introduce el Client ID y, solo si Trakt lo exige, el Client Secret.", color = Color.LightGray)
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Client ID") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("Client Secret (opcional)") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Text("Guardar credenciales distintas desvincula las cuentas Trakt de todos los perfiles. No se borra tu historial local. Si usas Client Secret, introdúcelo también al editar.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = Color(0xFFFF7777)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvButton("Guardar", { viewModel.save(id, secret) { open = false } }, enabled = !busy && id.isNotBlank())
                    TvButton("Desactivar", { viewModel.save("", "") { open = false } }, enabled = !busy && configured)
                    TvButton("Cancelar", { open = false }, enabled = !busy)
                }
            }
        }
    }
}
