package ps.anira.aid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ps.anira.aid.backup.TelegramSettingsStore
import ps.anira.aid.data.Repository

class SettingsViewModelFactory(
    private val repo: Repository, private val store: TelegramSettingsStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(repo, store) as T
}

@Composable
fun SettingsScreen(repo: Repository, store: TelegramSettingsStore, modifier: Modifier = Modifier) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repo, store))
    val state by vm.state.collectAsState()
    var showToken by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("النسخ الاحتياطي عبر تيليجرام", style = MaterialTheme.typography.titleLarge)
        Text(
            "التوكن ومعرّف المحادثة يُحفظان على جهازك فقط — لا يُصدَّران أبداً ضمن أي نسخة JSON.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.botToken,
                    onValueChange = vm::onTokenChange,
                    label = { Text("Bot Token") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.chatId,
                    onValueChange = vm::onChatIdChange,
                    label = { Text("Chat ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { vm.saveConfig() }, modifier = Modifier.fillMaxWidth()) {
                    Text("حفظ الإعدادات")
                }
            }
        }

        Button(
            onClick = { vm.sendBackupNow() },
            enabled = state.sendStatus != SendStatus.SENDING,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (state.sendStatus) {
                SendStatus.SENDING -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                else -> {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  رفع نسخة احتياطية الآن")
                }
            }
        }

        when (state.sendStatus) {
            SendStatus.SUCCESS -> StatusRow(Icons.Filled.CheckCircle, "تم رفع النسخة إلى تيليجرام بنجاح", MaterialTheme.colorScheme.primary)
            SendStatus.ERROR -> StatusRow(Icons.Filled.Error, state.errorMessage ?: "حدث خطأ", MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}

@Composable
private fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
