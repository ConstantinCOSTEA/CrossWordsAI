package fr.univcotedazur.crosswordsai.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.univcotedazur.crosswordsai.data.model.GridCell
import fr.univcotedazur.crosswordsai.viewmodel.CrosswordViewModel

@Composable
fun CrossedWordsView(
    modifier: Modifier = Modifier,
    viewModel: CrosswordViewModel = viewModel()
) {
    val cells by viewModel.gridState.collectAsState()
    CrossedWordsContent(cells = cells, modifier = modifier)
}

@Composable
fun CrossedWordsContent(
    cells: List<GridCell>,
    modifier: Modifier = Modifier
) {
    if (cells.isNotEmpty()) {
        val backgroundColor = Color(0xFFF0F4F8)
        val textMeasurer = rememberTextMeasurer()

        val maxX = cells.maxOf { it.x }
        val maxY = cells.maxOf { it.y }
        val columns = maxX + 1
        val rows = maxY + 1

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .aspectRatio(columns.toFloat() / rows.toFloat())
                    .fillMaxSize(0.9f)
            ) {
                val cellSize = minOf(size.width / columns, size.height / rows)

                cells.forEach { cell ->
                    val left = cell.x * cellSize
                    val top = cell.y * cellSize


                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    drawRoundRect(
                        color = Color.DarkGray,
                        topLeft = Offset(left, top),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    cell.number?.let { num ->
                        val textLayoutResult = textMeasurer.measure(
                            text = num.toString(),
                            style = TextStyle(
                                fontSize = (cellSize * 0.25f).toSp(),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                left + 4.dp.toPx(),
                                top + 2.dp.toPx()
                            )
                        )
                    }

                    cell.char?.let { char ->
                        val textLayoutResult = textMeasurer.measure(
                            text = char.toString(),
                            style = TextStyle(
                                fontSize = (cellSize * 0.6f).toSp(),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                left + (cellSize - textLayoutResult.size.width) / 2,
                                top + (cellSize - textLayoutResult.size.height) / 2
                            )
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chargement...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}