package fr.univcotedazur.crosswordsai.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class to represent a single crossword puzzle
data class CrosswordPuzzle(
    val id: String,
    val title: String,
    val creationDate: Date,
    val progress: Int // Percentage completed (0-100)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    modifier: Modifier = Modifier,
    userName: String?
) {
    val samplePuzzles = remember {
        listOf(
            CrosswordPuzzle("1", "Défi Quotidien #123", Date(), 75),
            CrosswordPuzzle("2", "Puzzle Débutant", Date(System.currentTimeMillis() - 86400000 * 5), 20),
            CrosswordPuzzle("3", "Grille Expert Amusante", Date(System.currentTimeMillis() - 86400000 * 10), 95),
            CrosswordPuzzle("4", "Puzzle Thème : Animaux", Date(System.currentTimeMillis() - 86400000 * 2), 0),
            CrosswordPuzzle("5", "Résolution Rapide", Date(System.currentTimeMillis() - 86400000 * 1), 50),
            CrosswordPuzzle("6", "Figures Historiques", Date(System.currentTimeMillis() - 86400000 * 7), 60),
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Crosswords AI",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors( // Utiliser centerAlignedTopAppBarColors pour TopAppBar
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Navigate to Add New Crossword screen */ },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = CircleShape,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Filled.Add, "Ajouter un nouveau mot croisé", Modifier.size(28.dp))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            userName?.let { name ->
                Text(
                    text = "Bonjour $name 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
            }

            Text(
                text = "Vos Mots Croisés",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp, top = 12.dp)
            )

            if (samplePuzzles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun mot croisé trouvé. Appuyez sur '+' pour en ajouter un nouveau !",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1), // Un mot croisé par ligne
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(samplePuzzles) { puzzle ->
                        CrosswordCard(puzzle = puzzle,
                            onContinueClick = { /* TODO: Navigate to puzzle screen */ },
                            onDeleteClick = { /* TODO: Show confirmation, then delete */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CrosswordCard(
    puzzle: CrosswordPuzzle,
    onContinueClick: (CrosswordPuzzle) -> Unit,
    onDeleteClick: (CrosswordPuzzle) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) } // État pour gérer l'affichage des boutons

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded }, // Rendre la carte cliquable pour expand/shrink
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = puzzle.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Créé le: ${dateFormatter.format(puzzle.creationDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = puzzle.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap, // Ajoutez strokeCap ici
            )
            Text(
                text = "${puzzle.progress}% terminé",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Boutons d'action visibles si la carte est "expanded"
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onContinueClick(puzzle) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Continuer", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Continuer")
                        }
                        Button(
                            onClick = { onDeleteClick(puzzle) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, "Supprimer", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 360)
@Composable
fun PreviewHomeViewModern() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeView(userName = "test")
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun PreviewCrosswordCardModern() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CrosswordCard(
                puzzle = CrosswordPuzzle(
                    "1", "Défi Quotidien Super Long", Date(), 65
                ),
                onContinueClick = {},
                onDeleteClick = {}
            )
        }
    }
}