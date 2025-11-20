package com.aktarjabed.inbusiness.utils

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView

@Composable
fun PdfViewer(uri: Uri, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PDFView(ctx, null)
        },
        update = { pdfView ->
            pdfView.fromUri(uri)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .spacing(10)
                .load()
        },
        modifier = modifier
    )
}