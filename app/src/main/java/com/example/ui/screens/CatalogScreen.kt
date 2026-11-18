package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.components.ProductCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CatalogFilters
import com.example.ui.viewmodel.SortOption

@Composable
fun CatalogScreen(
    categories: List<Category>,
    products: List<Product>,
    filters: CatalogFilters,
    profile: UserProfile?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleBulkOnly: () -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = filters.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(s.searchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (filters.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("catalog_search_input")
        )

        // Categories Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = filters.selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(s.allCategories) },
                    modifier = Modifier.testTag("cat_chip_all")
                )
            }
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = filters.selectedCategoryId == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    label = { Text(cat.localizedName(s.isArabic)) },
                    modifier = Modifier.testTag("cat_chip_${cat.id}")
                )
            }
        }

        // Bulk Filter & Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filters.bulkOnly,
                onClick = onToggleBulkOnly,
                label = { Text(s.bulkOnlyFilter) },
                modifier = Modifier.testTag("bulk_filter_chip")
            )

            Box {
                OutlinedButton(
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier.testTag("sort_button")
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = when (filters.sortOption) {
                            SortOption.FEATURED -> " ${s.sortFeatured}"
                            SortOption.PRICE_LOW -> " ${s.sortPriceLow}"
                            SortOption.PRICE_HIGH -> " ${s.sortPriceHigh}"
                        }
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
        }

        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (s.isArabic) "لا توجد منتجات مطابقة لبحثك" else "No matching products found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 166.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id }) { prod ->
                    ProductCard(
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
