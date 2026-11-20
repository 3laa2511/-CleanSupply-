package com.example.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplySearchField
import com.example.core.components.EmptyStateView
import com.example.core.components.ProductGridCard
import com.example.core.components.ProductListRowCard
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CatalogFilters
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCatalogScreen(
    categories: List<Category>,
    products: List<Product>,
    filters: CatalogFilters,
    recentSearches: List<String>,
    profile: UserProfile?,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleBulkOnly: () -> Unit,
    onToggleInStockOnly: () -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onResetFilters: () -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(),
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var sortMenuOpen by remember { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    val suggestions = remember(categories, s.isArabic) {
        categories.take(4).map { it.localizedName(s.isArabic).split(" ").first() }.distinct()
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = colors.surface,
        labelColor = colors.textPrimary,
        iconColor = colors.textSecondary,
        selectedContainerColor = colors.primaryContainer,
        selectedLabelColor = colors.onPrimaryContainer,
        selectedLeadingIconColor = colors.primary
    )

    val hasActiveFilters = filters.searchQuery.isNotBlank() ||
        filters.selectedCategoryId != null ||
        filters.bulkOnly ||
        filters.inStockOnly

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontal)
    ) {
        Spacer(Modifier.height(spacing.sm))

        CleanSupplySearchField(
            query = filters.searchQuery,
            onQueryChange = onSearchChange,
            placeholder = s.searchHint,
            onSubmit = { onCommitSearch(filters.searchQuery) },
            testTag = "catalog_search_input"
        )

        // Compact Recent Searches / Suggestions Row (keeps products prominent in top viewport)
        if (filters.searchQuery.isBlank() && (recentSearches.isNotEmpty() || suggestions.isNotEmpty())) {
            val activeTerms = if (recentSearches.isNotEmpty()) recentSearches else suggestions
            val isRecent = recentSearches.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    items(activeTerms, key = { (if (isRecent) "recent_" else "sug_") + it }) { term ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onSearchChange(term)
                                if (!isRecent) onCommitSearch(term)
                            },
                            shape = shapes.pill,
                            colors = chipColors,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isRecent) CleanSupplyIcons.History else CleanSupplyIcons.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(term, style = type.labelSmall) }
                        )
                    }
                }
                if (isRecent) {
                    TextButton(
                        onClick = onClearRecentSearches,
                        contentPadding = PaddingValues(horizontal = spacing.xs)
                    ) {
                        Text(s.clearAll, style = type.labelSmall, color = colors.primary)
                    }
                }
            }
        }

        // Categories Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = PaddingValues(vertical = spacing.xs)
        ) {
            item(key = "cat_all") {
                FilterChip(
                    selected = filters.selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    shape = shapes.pill,
                    colors = chipColors,
                    leadingIcon = {
                        Icon(CleanSupplyIcons.AllCategories, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    label = { Text(s.allCategories, style = type.labelMedium) },
                    modifier = Modifier.testTag("cat_chip_all")
                )
            }
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = filters.selectedCategoryId == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    shape = shapes.pill,
                    colors = chipColors,
                    leadingIcon = {
                        Icon(CleanSupplyIcons.categoryIcon(cat.iconKey), contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    label = { Text(cat.localizedName(s.isArabic), style = type.labelMedium) },
                    modifier = Modifier.testTag("cat_chip_${cat.id}")
                )
            }
        }

        // Bulk, Availability, Count, Sort & Instant Grid/List Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = filters.bulkOnly,
                        onClick = onToggleBulkOnly,
                        shape = shapes.pill,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.surface,
                            selectedContainerColor = colors.discountContainer,
                            selectedLabelColor = colors.discount,
                            selectedLeadingIconColor = colors.discount
                        ),
                        leadingIcon = {
                            Icon(CleanSupplyIcons.BulkDeal, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text(s.bulkOnlyFilter, style = type.labelSmall) },
                        modifier = Modifier.testTag("bulk_filter_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = filters.inStockOnly,
                        onClick = onToggleInStockOnly,
                        shape = shapes.pill,
                        colors = chipColors,
                        leadingIcon = {
                            Icon(CleanSupplyIcons.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text(s.inStockOnlyFilter, style = type.labelSmall) },
                        modifier = Modifier.testTag("in_stock_filter_chip")
                    )
                }
                item {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = shapes.pill,
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Text(
                            text = "${products.size} ${s.resultsCountLabel}",
                            style = type.numericSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(spacing.xs))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Box {
                    OutlinedButton(
                        onClick = { sortMenuOpen = true },
                        shape = shapes.button,
                        border = BorderStroke(1.dp, colors.outlineStrong),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = 0.dp),
                        modifier = Modifier
                            .height(spacing.minTouchTarget)
                            .testTag("sort_button")
                    ) {
                        Icon(CleanSupplyIcons.Sort, contentDescription = null, tint = colors.primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(spacing.xxs))
                        Text(
                            text = when (filters.sortOption) {
                                SortOption.FEATURED -> s.sortFeatured
                                SortOption.PRICE_LOW -> s.sortPriceLow
                                SortOption.PRICE_HIGH -> s.sortPriceHigh
                            },
                            style = type.labelSmall
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.sortFeatured) },
                            onClick = {
                                onSelectSort(SortOption.FEATURED)
                                sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.sortPriceLow) },
                            onClick = {
                                onSelectSort(SortOption.PRICE_LOW)
                                sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.sortPriceHigh) },
                            onClick = {
                                onSelectSort(SortOption.PRICE_HIGH)
                                sortMenuOpen = false
                            }
                        )
                    }
                }

                Surface(
                    color = colors.surface,
                    shape = shapes.button,
                    border = BorderStroke(1.dp, colors.outlineStrong)
                ) {
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(spacing.minTouchTarget)
                            .testTag("layout_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) CleanSupplyIcons.ListView else CleanSupplyIcons.GridView,
                            contentDescription = if (isGridView) "List View" else "Grid View",
                            tint = colors.primary
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (products.isEmpty()) {
                EmptyStateView(
                    icon = CleanSupplyIcons.SearchEmpty,
                    title = s.noMatchingProducts,
                    subtitle = s.searchHint,
                    actionLabel = if (hasActiveFilters) s.resetFilters else null,
                    onAction = if (hasActiveFilters) {
                        {
                            onSearchChange("")
                            onSelectCategory(null)
                            if (filters.bulkOnly) onToggleBulkOnly()
                            if (filters.inStockOnly) onToggleInStockOnly()
                            onResetFilters()
                        }
                    } else null,
                    actionTestTag = "search_reset_filters_btn"
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 166.dp),
                    contentPadding = PaddingValues(bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.gridGap),
                    horizontalArrangement = Arrangement.spacedBy(spacing.gridGap),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = products,
                        key = { it.id },
                        contentType = { "product_grid" }
                    ) { prod ->
                        ProductGridCard(
                            product = prod,
                            profile = profile,
                            onCardClick = { onOpenProductDetail(prod) },
                            onQuickAdd = { onQuickAdd(prod) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.listGap),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = products,
                        key = { it.id },
                        contentType = { "product_row" }
                    ) { prod ->
                        ProductListRowCard(
                            product = prod,
                            profile = profile,
                            onCardClick = { onOpenProductDetail(prod) },
                            onQuickAdd = { onQuickAdd(prod) }
                        )
                    }
                }
            }
        }
    }
}
