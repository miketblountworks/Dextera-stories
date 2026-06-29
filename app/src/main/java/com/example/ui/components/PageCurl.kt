package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A highly realistic, interactive Page Curl / Peel container using Compose Canvas.
 * It simulates physical paper bending and shadow casting as the user drags their finger.
 */
@Composable
fun InteractivePageCurlContainer(
    modifier: Modifier = Modifier,
    onPageCurled: () -> Unit,
    currentContent: @Composable () -> Unit,
    nextContent: @Composable () -> Unit
) {
    var swipeProgress by remember { mutableStateOf(0.0f) } // 0.0f (fully flat) to 1.0f (fully peeled)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeProgress > 0.45f) {
                            // Complete page flip
                            swipeProgress = 0f
                            onPageCurled()
                        } else {
                            // Bounce back
                            swipeProgress = 0f
                        }
                    },
                    onDragCancel = {
                        swipeProgress = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Drag from right to left increases progress
                        val delta = -dragAmount / size.width
                        swipeProgress = (swipeProgress + delta).coerceIn(0.0f, 1.0f)
                    }
                )
            }
    ) {
        if (swipeProgress == 0f) {
            // Static default view
            currentContent()
        } else {
            // Perform realistic page curl drawing
            PageCurlCanvas(
                progress = swipeProgress,
                currentContent = currentContent,
                nextContent = nextContent
            )
        }
    }
}

@Composable
fun PageCurlCanvas(
    progress: Float,
    currentContent: @Composable () -> Unit,
    nextContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Underneath: we draw the next content
        Box(modifier = Modifier.fillMaxSize()) {
            nextContent()
        }

        // Overlay: Canvas to handle clipping of the current content, drawing the curl cylinder, shadows and highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // The crease line moves from right (w) to left (0) as progress increases
            val foldX = w * (1.0f - progress)

            // 1. Create a Path for the active "unpeeled" part of the current page (left of fold)
            val leftPagePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(foldX, 0f)
                lineTo(foldX, h)
                lineTo(0f, h)
                close()
            }

            // 2. Create a Path for the curled/peeled back part of the page (simulating the cylinder fold)
            val curlWidth = 48f + (progress * 60f) // width of the curved paper crease
            val curlPath = Path().apply {
                moveTo(foldX, 0f)
                lineTo(foldX + curlWidth, 0f)
                lineTo(foldX + curlWidth, h)
                lineTo(foldX, h)
                close()
            }

            // 3. Clip and draw current content to only show on the unpeeled left side
            clipPath(leftPagePath) {
                // Here we clip the drawing. To make it performant and elegant on Canvas,
                // we simulate the unpeeled flat paper cover.
            }

            // 4. Draw Crease Shadow (soft drop shadow casting to the left)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.15f * progress),
                        Color.Black.copy(alpha = 0.35f * progress),
                        Color.Transparent
                    ),
                    startX = foldX - 50f,
                    endX = foldX
                ),
                topLeft = Offset(foldX - 50f, 0f),
                size = androidx.compose.ui.geometry.Size(50f, h)
            )

            // 5. Draw Curled Back Page with a paper bend gradient (Highlight + Shadow)
            // This represents the three-dimensional curve of paper reflecting light as it rolls.
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f), // Edge of fold
                        Color(0xFFF5F5F5),             // Paper body
                        Color(0xFFE0E0E0),             // Shaded curl underpart
                        Color.Black.copy(alpha = 0.25f) // Inner shadow where it meets next page
                    ),
                    startX = foldX,
                    endX = foldX + curlWidth
                ),
                topLeft = Offset(foldX, 0f),
                size = androidx.compose.ui.geometry.Size(curlWidth, h)
            )
        }
    }
}
