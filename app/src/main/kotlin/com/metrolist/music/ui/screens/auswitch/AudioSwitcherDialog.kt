package com.metrolist.music.ui.screens.auswitch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.isSyncEnabled
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AudioSourcesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.logging.Logger

@Composable
fun CreateAudioSource(
    onDismiss: () -> Unit,
    Title: String? = null,
    SongId: String,
    fieldVals: List<Pair<String, TextFieldValue>>? = null,
    viewModel: AudioSourcesViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    // Use delegated state so the text is a plain String for OutlinedTextField
    var sourceTitle by rememberSaveable { mutableStateOf("") }

    // File selection state
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }

    // Launcher for picking a single document (e.g., audio or any file)
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            // user cancelled
            return@rememberLauncherForActivityResult
        }

        selectedFileUri = uri

        // Persist read permission so we can access the file later if needed
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // ignore if not available
        }

        // Get display name for the Uri (fallback to a generated name)
        var displayName = "selected_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                displayName = cursor.getString(nameIndex)
            }
        }

        // Copy contents to a file in the app cache and expose its absolute path
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val outFile = File(context.cacheDir, displayName)
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                selectedFilePath = outFile.absolutePath
                Toast.makeText(context, "File copied to: $selectedFilePath", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Unable to open selected file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.graphic_eq),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                if (Title != null) {
                    Text(
                        text = Title
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection status indicator and controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = sourceTitle,
                            onValueChange = { sourceTitle = it },
                            label = { Text("Title") },
                            placeholder = { Text("Enter Title") },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.edit), contentDescription = null)
                            },
                            trailingIcon = {
                                if (sourceTitle.isNotBlank()) {
                                    IconButton(onClick = { sourceTitle = "" }) {
                                        Icon(painterResource(R.drawable.close), contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                    }
                    Column(modifier = Modifier.padding(4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                        // Button that launches the system file picker
                        Button(onClick = {
                            // Launch file picker for audio files; change to "*/*" to allow any type
                            pickFileLauncher.launch(arrayOf("audio/*"))
                        }) {
                            Text(text = "Select file")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show selected file path (cached copy) or the Uri for debugging
                        Text(
                            text = selectedFilePath ?: selectedFileUri?.toString()
                            ?: "No file selected",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = sourceTitle.ifBlank { "Audio Source" }

                if (selectedFilePath?.isNotBlank() ?: false) {
                    viewModel.addAudioSource(
                    externalAudioPath = selectedFilePath.toString(),
                    isSelected = false,
                    type = 0,
                    title = name,
                        songId = SongId
                    )
                } else {
                    Toast.makeText(context, "Select a file please", Toast.LENGTH_LONG).show()
                }
            }
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}