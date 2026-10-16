package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * High-fidelity animated shimmer gradient brush for skeleton loading screens.
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1200f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        )

        val transition = rememberInfiniteTransition(label = "SkeletonShimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SkeletonShimmerAnimation"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Basic reusable skeleton placeholder box with animated shimmer background.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Skeleton Loader matching the structure and dimensions of [BlueprintCardItem].
 */
@Composable
fun SkeletonBlueprintCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("skeleton_blueprint_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Title & Category Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(180.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonBox(
                        modifier = Modifier
                            .width(70.dp)
                            .height(18.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    SkeletonBox(
                        modifier = Modifier
                            .width(45.dp)
                            .height(18.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Row: Thumbnail + Details + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blueprint Thumbnail Placeholder
                SkeletonBox(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Detail text placeholders
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        modifier = Modifier
                            .width(130.dp)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .width(90.dp)
                            .height(11.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .width(160.dp)
                            .height(18.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                // Action buttons placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonBox(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape
                    )
                    SkeletonBox(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Timestamp & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )

                SkeletonBox(
                    modifier = Modifier
                        .width(130.dp)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

/**
 * Vertical list of skeleton blueprint cards.
 */
@Composable
fun SkeletonBlueprintsList(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("skeleton_blueprints_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            SkeletonBlueprintCard()
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Skeleton Loader matching the structure and dimensions of [BuildingTemplateCard].
 */
@Composable
fun SkeletonTemplateCard(
    isFullWidth: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = if (isFullWidth) {
            modifier
                .fillMaxWidth()
                .testTag("skeleton_template_card_full")
        } else {
            modifier
                .width(280.dp)
                .testTag("skeleton_template_card_compact")
        },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero Thumbnail Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                )
                // Floating category chip
                SkeletonBox(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                        .width(80.dp)
                        .height(22.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Title and subtitle
                SkeletonBox(
                    modifier = Modifier
                        .width(200.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(11.dp),
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Specs bar (Bedrooms, Bathrooms, SQM, Floors)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) {
                        SkeletonBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // Footer Row: Price estimate and CTA Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SkeletonBox(
                            modifier = Modifier
                                .width(70.dp)
                                .height(9.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .width(110.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }

                    SkeletonBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(34.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Vertical list of skeleton template cards for catalog view.
 */
@Composable
fun SkeletonTemplatesList(
    count: Int = 3,
    isFullWidth: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isFullWidth) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .testTag("skeleton_templates_vertical_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count) {
                SkeletonTemplateCard(isFullWidth = true)
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .testTag("skeleton_templates_horizontal_list"),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count) {
                SkeletonTemplateCard(isFullWidth = false)
            }
        }
    }
}

/**
 * Skeleton loader for the top 3 KPI Ribbon on Dashboard.
 */
@Composable
fun SkeletonKpiRibbon(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { idx ->
            SkeletonBox(
                modifier = Modifier
                    .weight(if (idx == 2) 1.3f else 1f)
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Skeleton loader for filter chip bars.
 */
@Composable
fun SkeletonFilterChips(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count) { idx ->
            SkeletonBox(
                modifier = Modifier
                    .width((60 + (idx % 3) * 20).dp)
                    .height(32.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
