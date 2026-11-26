package com.shieldstream.tv.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shieldstream.tv.domain.PortableBackup
import com.shieldstream.tv.ui.component.TvButton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupEntryPoint {
    fun portableBackup(): PortableBackup
}

@Composable
internal fun PortableBackupCard(compact: Boolean = false) {
    val context = LocalContext.current
    val backup = remember(context) { runCatching { EntryPointAccessors.fromApplication(context.applicationContext, BackupEntryPoint::class.java).portableBackup() }.getOrNull() }
    if (backup == null || !backup.allowed) return
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val pass = password.toCharArray()
            password = ""
            busy = true
            scope.launch {
                message = try {
                    withContext(Dispatchers.IO) {
                        val bytes = backup.export(pass)
                        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                    }
                    "Copia cifrada guardada. Conserva el archivo y la contraseña."
                } catch (_: Exception) { "No se pudo guardar la copia." }
                finally { pass.fill('\u0000'); busy = false }
            }
        }
    }
    val select = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val pass = password.toCharArray()
            password = ""
            busy = true
            scope.launch {
                message = try {
                    withContext(Dispatchers.IO) {
                        val bytes = checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                            val out = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                require(out.size() + n <= 16 * 1024 * 1024 + 64)
                                out.write(buffer, 0, n)
                            }
                            out.toByteArray()
                        }
                        backup.restore(bytes, pass)
                    }
                    restored = true
                    "Copia restaurada. Reinicia CINEFRAN antes de continuar. Reconecta Trakt."
                } catch (_: Exception) { "No se pudo restaurar: comprueba la contraseña y el archivo." }
                finally { pass.fill('\u0000'); busy = false }
            }
        }
    }
    if (compact) TvButton(text = "Restaurar copia", onClick = { open = true; message = "" })
    else FocusCard(onClick = { open = true; message = "" }) { _ ->
        Column {
            Text("Copia de seguridad", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("Exportar o restaurar perfiles, listas, historial, preferencias y claves API con contraseña.", color = SettingsMuted)
        }
    }
    if (open) Dialog(onDismissRequest = { if (!busy && !restored) { open = false; password = "" } }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(560.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Copia de seguridad CINEFRAN", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("Usa una contraseña de al menos 12 caracteres. Restaurar sustituye los datos actuales; guarda antes otra copia. Las sesiones de Trakt deben reconectarse.", color = Color.White)
            if (!restored) {
                OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
                TvButton(text = "Exportar copia cifrada", enabled = !busy && password.length >= 12, onClick = { runCatching { create.launch("CINEFRAN.cfb") }.onFailure { message = "No hay selector de archivos compatible." } })
                TvButton(text = "Restaurar y sustituir datos", enabled = !busy && password.length >= 12, onClick = { runCatching { select.launch(arrayOf("*/*")) }.onFailure { message = "No hay selector de archivos compatible." } })
                TvButton(text = "Cerrar", enabled = !busy, onClick = { open = false; password = "" })
            } else TvButton(text = "Reiniciar CINEFRAN", onClick = { com.shieldstream.tv.ui.component.restartApp(context) })
            if (busy) Text("Procesando…", color = Color.White)
            if (message.isNotBlank()) Text(message, color = Color.White)
        }
    }
}
