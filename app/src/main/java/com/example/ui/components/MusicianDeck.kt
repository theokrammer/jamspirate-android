package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicianCategory
import com.example.model.MusicianProfile
import com.example.model.MusicianRegistry

@Composable
fun MusicianInfluenceDeck(
    selectedMusician: MusicianProfile,
    onSelectMusician: (MusicianProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<MusicianCategory?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("musician_influence_deck")
    ) {
        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All (${MusicianRegistry.ALL_MUSICIANS.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF334155),
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.JAZZ,
                    onClick = { selectedCategoryFilter = MusicianCategory.JAZZ },
                    label = { Text("Jazz") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.ROCK_BLUES,
                    onClick = { selectedCategoryFilter = MusicianCategory.ROCK_BLUES },
                    label = { Text("Rock & Blues") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.VIRTUOSO_SHRED,
                    onClick = { selectedCategoryFilter = MusicianCategory.VIRTUOSO_SHRED },
                    label = { Text("Virtuoso Shred") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.FLAMENCO,
                    onClick = { selectedCategoryFilter = MusicianCategory.FLAMENCO },
                    label = { Text("Flamenco") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.CLASSICAL,
                    onClick = { selectedCategoryFilter = MusicianCategory.CLASSICAL },
                    label = { Text("Classical") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategoryFilter == MusicianCategory.DIAGNOSTIC,
                    onClick = { selectedCategoryFilter = MusicianCategory.DIAGNOSTIC },
                    label = { Text("⚡ Diagnostics") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFF10B981)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Musician Cards Carousel
        val filteredMusicians = remember(selectedCategoryFilter) {
            if (selectedCategoryFilter == null) {
                MusicianRegistry.ALL_MUSICIANS
            } else {
                MusicianRegistry.ALL_MUSICIANS.filter { it.category == selectedCategoryFilter }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("musicians_carousel"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredMusicians, key = { it.id }) { musician ->
                val isSelected = musician.id == selectedMusician.id

                MusicianCard(
                    musician = musician,
                    isSelected = isSelected,
                    onClick = { onSelectMusician(musician) }
                )
            }
        }
    }
}

@Composable
fun MusicianCard(
    musician: MusicianProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(230.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("musician_card_${musician.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(musician.accentColor)
            )
        } else {
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Name & Selected Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = musician.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = musician.eraOrGenre,
                        fontSize = 11.sp,
                        color = musician.accentColor,
                        maxLines = 1
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Selected",
                        tint = musician.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (musician.famousPiece.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(musician.accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = musician.accentColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Piece: ${musician.famousPiece}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = musician.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Signature style summary
            Text(
                text = musician.signatureStyle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Phrasing Stat Meters
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PhrasingBar(
                    label = "Density",
                    value = musician.noteDensity,
                    color = musician.accentColor
                )
                PhrasingBar(
                    label = "Swing",
                    value = musician.swingFactor,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
fun PhrasingBar(
    label: String,
    value: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.width(42.dp),
            fontFamily = FontFamily.Monospace
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF334155))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0.05f, 1f))
                    .background(color)
            )
        }
    }
}
