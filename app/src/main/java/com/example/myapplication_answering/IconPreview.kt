package com.example.myapplication_answering

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun AppIconPreview() {
    Box(
        modifier = Modifier
            .size(108.dp)
            .background(Color.Gray)
    ) {
        // Since we can't easily preview the adaptive-icon XML directly in Compose Image
        // we can preview the foreground and background separately or just trust the vector drawables.
        // But we can try to load the adaptive icon if the system supports it.
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
    }
}
