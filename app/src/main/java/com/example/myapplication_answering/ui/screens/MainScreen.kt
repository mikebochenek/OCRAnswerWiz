package com.example.myapplication_answering.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication_answering.ocr.OcrResult
import com.example.myapplication_answering.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageUri by viewModel.imageUri.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onImageSelected(tempUri)
        }
    }

    // Effect to navigate to detail pane when Gemini answer is available
    LaunchedEffect(uiState) {
        if (uiState is MainViewModel.UiState.Success && (uiState as MainViewModel.UiState.Success).geminiAnswer != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }
    }

    val scope = rememberCoroutineScope()

    BackHandler(navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Question OCR") }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }

                            Button(onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    val uri = createTempImageUri(context)
                                    tempUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            }) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        when (val state = uiState) {
                            MainViewModel.UiState.Idle -> {
                                Text("Select an image to extract text")
                            }
                            MainViewModel.UiState.Loading -> {
                                CircularProgressIndicator()
                            }
                            is MainViewModel.UiState.Success -> {
                                ResultContent(state.result)
                            }
                            is MainViewModel.UiState.Error -> {
                                Column {
                                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                                    state.result?.let { ResultContent(it) }
                                }
                            }
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Gemini Answer") },
                            navigationIcon = {
                                if (navigator.canNavigateBack()) {
                                    IconButton(onClick = { 
                                        scope.launch {
                                            navigator.navigateBack() 
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        when (val state = uiState) {
                            is MainViewModel.UiState.Success -> {
                                if (state.geminiAnswer != null) {
                                    GeminiAnswerContent(state.geminiAnswer)
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator()
                                            Spacer(Modifier.height(8.dp))
                                            Text("Gemini is thinking...")
                                        }
                                    }
                                }
                            }
                            is MainViewModel.UiState.Error -> {
                                Text("Error fetching answer: ${state.message}")
                            }
                            else -> {
                                Text("Select a question to see the answer")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ResultContent(result: OcrResult) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Text("Question:", style = MaterialTheme.typography.titleMedium)
            Text(result.question, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        if (result.options.isNotEmpty()) {
            item {
                Text("Options:", style = MaterialTheme.typography.titleMedium)
            }
            items(result.options) { option ->
                Text("- $option", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        item {
            Text("Full Extracted Text:", style = MaterialTheme.typography.titleSmall)
            Text(result.fullText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun GeminiAnswerContent(answer: String) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("AI Answer", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            item {
                Text(answer, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun createTempImageUri(context: android.content.Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MainScreenAdaptivePreview() {
    com.example.myapplication_answering.ui.theme.OCRAnswerWizTheme {
        Surface {
            Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Adaptive Layout Preview (Tablet)")
            }
        }
    }
}
