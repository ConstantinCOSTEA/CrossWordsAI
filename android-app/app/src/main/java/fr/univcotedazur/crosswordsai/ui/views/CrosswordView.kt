package fr.univcotedazur.crosswordsai.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.univcotedazur.crosswordsai.data.model.GridCell
import fr.univcotedazur.crosswordsai.viewmodel.CrosswordViewModel
import kotlin.math.roundToInt

@Composable
fun CrossedWordsView(
    modifier: Modifier = Modifier,
    viewModel: CrosswordViewModel = viewModel()
) {
    val cells by viewModel.gridState.collectAsState()
    val gridWidth by viewModel.gridWidth.collectAsState()
    val gridHeight by viewModel.gridHeight.collectAsState()
    val xAxisType by viewModel.xAxisType.collectAsState()
    val yAxisType by viewModel.yAxisType.collectAsState()

    CrossedWordsContent(
        cells = cells,
        gridWidth = gridWidth,
        gridHeight = gridHeight,
        xAxisType = xAxisType,
        yAxisType = yAxisType,
        modifier = modifier
    )
}

@Composable
fun CrossedWordsContent(
    cells: List<GridCell>,
    gridWidth: Int,
    gridHeight: Int,
    xAxisType: String,
    yAxisType: String,
    modifier: Modifier = Modifier
) {
    if (cells.isNotEmpty() && gridWidth > 0 && gridHeight > 0) {
        CrossedWordsGrid(
            cells = cells,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            xAxisType = xAxisType,
            yAxisType = yAxisType,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chargement...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CrossedWordsGrid(
    cells: List<GridCell>,
    gridWidth: Int,
    gridHeight: Int,
    xAxisType: String,
    yAxisType: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFFF0F4F8)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        GridWithConstraints(
            cells = cells,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            xAxisType = xAxisType,
            yAxisType = yAxisType,
            maxWidth = maxWidth,
            maxHeight = maxHeight
        )
    }
}

@Composable
private fun GridWithConstraints(
    cells: List<GridCell>,
    gridWidth: Int,
    gridHeight: Int,
    xAxisType: String,
    yAxisType: String,
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp
) {
    val density = LocalDensity.current
    val paddingDp = 16.dp
    val labelSize = 32.dp

    // Calculer la taille de cellule optimale pour que toute la grille soit visible
    // On laisse de la marge (80%) pour être sûr que tout rentre
    val availableWidth = maxWidth - labelSize - paddingDp * 2
    val availableHeight = maxHeight - labelSize - paddingDp * 2

    val maxCellWidth = availableWidth / gridWidth * 0.8f
    val maxCellHeight = availableHeight / gridHeight * 0.8f

    // Prendre la plus petite des deux pour que tout rentre, avec un max de 60dp
    val baseCellSize = minOf(maxCellWidth, maxCellHeight, 60.dp)

    // Calculer la taille initiale de la grille avec labels (en dp)
    val initialGridWidthDp = labelSize + baseCellSize * gridWidth
    val initialGridHeightDp = labelSize + baseCellSize * gridHeight

    // Calculer l'offset pour centrer la grille (en pixels)
    // On soustrait le padding car il sera ajouté dans le Column
    val initialOffsetX = with(density) {
        ((maxWidth - initialGridWidthDp - paddingDp * 2) / 2).toPx()
    }
    val initialOffsetY = with(density) {
        ((maxHeight - initialGridHeightDp - paddingDp * 2) / 2).toPx()
    }

    // État pour le zoom et le déplacement
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(initialOffsetX) }
    var offsetY by remember { mutableFloatStateOf(initialOffsetY) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = state)
    ) {
        // Conteneur avec labels + grille
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(paddingDp)
        ) {
            // Labels de l'axe X (en haut)
            XAxisLabels(
                gridWidth = gridWidth,
                xAxisType = xAxisType,
                labelSize = labelSize,
                cellSize = baseCellSize,
                scale = scale
            )

            // Ligne de grille avec labels Y
            Row {
                // Labels de l'axe Y (à gauche)
                YAxisLabels(
                    gridHeight = gridHeight,
                    yAxisType = yAxisType,
                    labelSize = labelSize,
                    cellSize = baseCellSize,
                    scale = scale
                )

                // La grille
                GridCanvas(
                    cells = cells,
                    cellSize = baseCellSize * scale,
                    modifier = Modifier.size(
                        baseCellSize * gridWidth * scale,
                        baseCellSize * gridHeight * scale
                    )
                )
            }
        }
    }
}

@Composable
private fun XAxisLabels(
    gridWidth: Int,
    xAxisType: String,
    labelSize: androidx.compose.ui.unit.Dp,
    cellSize: androidx.compose.ui.unit.Dp,
    scale: Float
) {
    Row {
        // Espace pour aligner avec la grille
        Box(modifier = Modifier.size(labelSize))

        // Labels X
        for (i in 1..gridWidth) {
            val label = if (xAxisType == "numbers") i.toString() else ('A' + i - 1).toString()
            Box(
                modifier = Modifier.size(cellSize * scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    gridHeight: Int,
    yAxisType: String,
    labelSize: androidx.compose.ui.unit.Dp,
    cellSize: androidx.compose.ui.unit.Dp,
    scale: Float
) {
    Column {
        for (i in 1..gridHeight) {
            val label = if (yAxisType == "numbers") i.toString() else ('A' + i - 1).toString()
            Box(
                modifier = Modifier.size(labelSize, cellSize * scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun GridCanvas(
    cells: List<GridCell>,
    cellSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val cellSizePx = cellSize.toPx()

        cells.forEach { cell ->
            val left = cell.x * cellSizePx
            val top = cell.y * cellSizePx

            // Dessine la case (noire si isEmpty, blanche sinon)
            drawRoundRect(
                color = if (cell.isEmpty) Color.DarkGray else Color.White,
                topLeft = Offset(left, top),
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Bordure
            drawRoundRect(
                color = Color.DarkGray,
                topLeft = Offset(left, top),
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Numéro et lettre (seulement pour les cases blanches)
            if (!cell.isEmpty) {
                cell.number?.let { num ->
                    val textLayoutResult = textMeasurer.measure(
                        text = num.toString(),
                        style = TextStyle(
                            fontSize = (cellSizePx * 0.25f).toSp(),
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

                // Lettre de la case
                cell.char?.let { char ->
                    val textLayoutResult = textMeasurer.measure(
                        text = char.toString(),
                        style = TextStyle(
                            fontSize = (cellSizePx * 0.6f).toSp(),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            left + (cellSizePx - textLayoutResult.size.width) / 2,
                            top + (cellSizePx - textLayoutResult.size.height) / 2
                        )
                    )
                }
            }
        }
    }
}