package com.example.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.components.EmptyStateView
import com.example.core.components.FastProductImage
import com.example.core.components.ProductCardSkeleton
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.i18n.LocalAppStrings

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    products: List<Product>,
    selectedCategoryId: String?,
    isLoading: Boolean = false,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val countsByCat = remember(products) {
        products.groupingBy { it.categoryId }.eachCount()
    }

    if (isLoading && categories.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.gridGap),
            horizontalArrangement = Arrangement.spacedBy(spacing.gridGap),
            modifier = modifier.fillMaxSize()
        ) {
            items(4) { ProductCardSkeleton() }
        }
        return
    }

    if (categories.isEmpty()) {
        EmptyStateView(
            icon = CleanSupplyIcons.NavCategoriesInactive,
            title = s.categoriesTitle,
            subtitle = s.noMatchingProducts,
            modifier = modifier
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.gridGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.gridGap),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "categories_header") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(s.categoriesTitle, style = type.headlineSmall, color = colors.textPrimary)
                Text(s.categoriesSubtitle, style = type.bodySmall, color = colors.textSecondary)
            }
        }

        // All Products Banner Card
        item(span = { GridItemSpan(maxLineSpan) }, key = "category_all_card") {
            val selected = selectedCategoryId == null
            Surface(
                onClick = { onSelectCategory(null) },
                shape = shapes.card,
                color = if (selected) colors.primaryContainer else colors.surface,
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_grid_card_all")
            ) {
                Row(
                    modifier = Modifier.padding(spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selected) colors.primary else colors.surfaceVariant,
                                    shapes.button
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CleanSupplyIcons.AllCategories,
                                contentDescription = null,
                                tint = if (selected) colors.onPrimary else colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = s.allCategories,
                                style = type.titleMedium,
                                color = if (selected) colors.onPrimaryContainer else colors.textPrimary
                            )
                            Text(
                                text = "${products.size} ${s.resultsCountLabel}",
                                style = type.caption,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Icon(CleanSupplyIcons.Forward, contentDescription = null, tint = colors.primary)
                }
            }
        }

        items(categories, key = { it.id }) { cat ->
            val selected = selectedCategoryId == cat.id
            val count = countsByCat[cat.id] ?: 0
            Card(
                onClick = { onSelectCategory(cat.id) },
                shape = shapes.card,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) colors.primaryContainer else colors.surface
                ),
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.primary else colors.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_grid_card_${cat.id}")
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .padding(spacing.xs)
                            .clip(shapes.smallComponent)
                    ) {
                        FastProductImage(
                            drawableRes = Formatters.productDrawable(cat.iconKey),
                            contentDescription = cat.localizedName(s.isArabic),
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = if (selected) colors.primary else colors.surface.copy(alpha = 0.92f),
                            shape = shapes.smallComponent,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(spacing.xs)
                                .size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CleanSupplyIcons.categoryIcon(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (selected) colors.onPrimary else colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.padding(
                            start = spacing.sm,
                            end = spacing.sm,
                            top = spacing.xxs,
                            bottom = spacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = cat.localizedName(s.isArabic),
                            style = type.titleSmall,
                            color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$count ${s.resultsCountLabel}",
                            style = type.numericSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
