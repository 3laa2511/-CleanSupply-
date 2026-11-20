package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.FastProductImage
import com.example.core.components.ProductGridCard
import com.example.core.components.SectionHeader
import com.example.core.components.SegmentVerificationBadge
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Category
import com.example.data.model.DeliveryZone
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.domain.PricingEngine
import com.example.ui.i18n.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: UserProfile?,
    activeZone: DeliveryZone?,
    categories: List<Category>,
    products: List<Product>,
    recentlyViewed: List<Product>,
    recentOrders: List<Order>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAllCategories: () -> Unit,
    onExploreBulkDeals: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onReorder: (String) -> Unit,
    onOpenAllCatalog: () -> Unit,
    onOpenOrders: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val featuredRows = remember(products) {
        val featured = products.filter { it.featured }.ifEmpty { products.take(4) }
        featured.chunked(2)
    }
    val countsByCat = remember(products) {
        products.groupingBy { it.categoryId }.eachCount()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_list"),
            contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Calm Editorial Greeting Strip directly on Warm Ivory canvas + Crisp White Search Bar
            item(key = "home_greeting", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val userName = profile?.displayName?.takeIf { it.isNotBlank() }
                            Text(
                                text = if (userName != null) "${s.welcomePrefix}، $userName" else s.guestBadge,
                                style = type.titleLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                SegmentVerificationBadge(profile)
                                if (activeZone != null) {
                                    Surface(
                                        color = colors.surface,
                                        shape = shapes.pill,
                                        border = BorderStroke(1.dp, colors.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = CleanSupplyIcons.Location,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${activeZone.localizedName(s.isArabic)} • ${activeZone.estimatedHours} ${s.hoursShort}",
                                                style = type.caption,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(spacing.xs))

                        Surface(
                            color = colors.secondaryContainer,
                            shape = shapes.pill,
                            border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = CleanSupplyIcons.Loyalty,
                                    contentDescription = null,
                                    tint = colors.warning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${profile?.loyaltyPoints ?: 0L} ${s.pointsUnit}",
                                    style = type.numericSmall,
                                    color = colors.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Crisp White Search Bar
                    Surface(
                        onClick = onOpenSearch,
                        shape = shapes.input,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineStrong),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.minTouchTarget)
                            .testTag("home_search_entry")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Icon(CleanSupplyIcons.Search, contentDescription = s.navSearch, tint = colors.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = s.searchHint,
                                style = type.bodyMedium,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(CleanSupplyIcons.Tune, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Editorial Deep Forest Green Wholesale Campaign Card
            item(key = "home_hero", contentType = "hero") {
                Card(
                    shape = shapes.largeSurface,
                    colors = CardDefaults.cardColors(containerColor = colors.brandBanner),
                    elevation = CardDefaults.cardElevation(defaultElevation = CleanSupplyTheme.elevations.subtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                        ) {
                            FastProductImage(
                                drawableRes = R.drawable.img_hero_cleansupply,
                                contentDescription = s.heroTitle,
                                targetSizePx = 800,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, colors.brandBanner.copy(alpha = 0.88f))
                                        )
                                    )
                            )
                            Surface(
                                color = colors.brandBanner.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, colors.heroAccent.copy(alpha = 0.55f)),
                                shape = shapes.pill,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(spacing.sm)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                                ) {
                                    Icon(CleanSupplyIcons.ShieldVerified, contentDescription = null, tint = colors.heroAccent, modifier = Modifier.size(14.dp))
                                    Text(s.heroBadge, style = type.labelSmall, color = colors.onBrandBanner)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .background(colors.brandBanner)
                                .padding(horizontal = spacing.md, vertical = spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Text(
                                text = s.heroTitle,
                                style = type.titleLarge,
                                color = colors.onBrandBanner,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = s.heroSubtitle,
                                style = type.bodySmall,
                                color = colors.onBrandBanner.copy(alpha = 0.84f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            CleanSupplyPrimaryButton(
                                text = s.exploreBulkCta,
                                icon = CleanSupplyIcons.BulkDeal,
                                containerColor = colors.interactive,
                                contentColor = colors.onInteractive,
                                testTag = "home_explore_bulk_cta",
                                onClick = onExploreBulkDeals,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Clean Single-Row Quick Actions Strip
            item(key = "home_quick_actions", contentType = "quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    QuickActionTile(
                        icon = CleanSupplyIcons.NavCategoriesActive,
                        label = s.navCategories,
                        onClick = onOpenAllCategories,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = CleanSupplyIcons.BulkDeal,
                        label = s.categoryBulkTag,
                        onClick = onExploreBulkDeals,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = CleanSupplyIcons.AllCategories,
                        label = s.quickActionCatalog,
                        onClick = onOpenAllCatalog,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = CleanSupplyIcons.Receipt,
                        label = s.quickActionOrders,
                        onClick = onOpenOrders,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Reorder Strip if user has previous orders
            if (recentOrders.isNotEmpty()) {
                item(key = "home_quick_reorder", contentType = "banner") {
                    val latest = recentOrders.first()
                    Surface(
                        color = colors.primaryContainer,
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.20f)),
                        shape = shapes.card,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(colors.surface, shapes.button),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(CleanSupplyIcons.Reorder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(s.quickReorderTitle, style = type.labelMedium, color = colors.onPrimaryContainer)
                                    Text(
                                        text = "${latest.orderNumber} • ${Formatters.money(latest.totalAmount, s.currency)}",
                                        style = type.numericSmall,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                            Spacer(Modifier.width(spacing.xs))
                            CleanSupplyPrimaryButton(
                                text = s.reorderNow,
                                icon = CleanSupplyIcons.Reorder,
                                testTag = "home_quick_reorder",
                                onClick = { onReorder(latest.id) }
                            )
                        }
                    }
                }
            }

            // Categories Horizontal Shelf (clean photos, zero heavy scrim)
            item(key = "home_categories", contentType = "categories") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionHeader(
                        title = s.categoriesTitle,
                        actionLabel = s.viewAll,
                        onAction = onOpenAllCategories
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(categories, key = { it.id }, contentType = { "category_chip" }) { cat ->
                            val count = countsByCat[cat.id] ?: 0
                            Surface(
                                shape = shapes.card,
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.outline),
                                modifier = Modifier
                                    .width(142.dp)
                                    .clip(shapes.card)
                                    .clickable { onSelectCategory(cat.id) }
                                    .testTag("home_category_${cat.id}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(78.dp)
                                            .padding(spacing.xs)
                                            .clip(shapes.smallComponent)
                                            .background(colors.surfaceVariant)
                                    ) {
                                        FastProductImage(
                                            drawableRes = Formatters.productDrawable(cat.iconKey),
                                            contentDescription = cat.localizedName(s.isArabic),
                                            targetSizePx = 360,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = colors.surface.copy(alpha = 0.94f),
                                            shape = shapes.smallComponent,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = CleanSupplyIcons.categoryIcon(cat.iconKey),
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.padding(
                                            start = spacing.sm,
                                            end = spacing.sm,
                                            bottom = spacing.xs
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = cat.localizedName(s.isArabic),
                                            style = type.labelLarge,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$count ${s.resultsCountLabel}",
                                            style = type.caption,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Best Sellers Grid Section Header
            item(key = "home_featured_header", contentType = "header") {
                SectionHeader(
                    title = s.featuredTitle,
                    subtitle = s.bestSellersTitle,
                    actionLabel = s.viewAll,
                    onAction = onOpenAllCatalog
                )
            }

            // Best Sellers 2-column rows
            items(
                items = featuredRows,
                key = { row -> row.first().id },
                contentType = { "product_row" }
            ) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.gridGap)
                ) {
                    pair.forEach { prod ->
                        ProductGridCard(
                            product = prod,
                            profile = profile,
                            onCardClick = { onOpenProductDetail(prod) },
                            onQuickAdd = { onQuickAdd(prod) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Recently Viewed Strip (strictly from real inspected products)
            if (recentlyViewed.isNotEmpty()) {
                item(key = "home_recently_viewed", contentType = "recent_viewed") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader(title = s.recentlyViewedTitle)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            items(recentlyViewed, key = { "recent_${it.id}" }) { prod ->
                                Surface(
                                    onClick = { onOpenProductDetail(prod) },
                                    shape = shapes.card,
                                    color = colors.surface,
                                    border = BorderStroke(1.dp, colors.outline),
                                    modifier = Modifier.width(152.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(spacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                                    ) {
                                        FastProductImage(
                                            drawableRes = Formatters.productDrawable(prod.imageKey),
                                            contentDescription = prod.localizedName(s.isArabic),
                                            targetSizePx = 320,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp)
                                                .clip(shapes.smallComponent)
                                        )
                                        Text(
                                            text = prod.localizedName(s.isArabic),
                                            style = type.labelMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.money(
                                                PricingEngine.resolveTierBaseUnitPrice(prod, profile),
                                                s.currency
                                            ),
                                            style = type.numericSmall,
                                            color = colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Restrained Warm Sand Institutional Supply Row
            item(key = "home_promo_info", contentType = "banner") {
                Surface(
                    onClick = onExploreBulkDeals,
                    color = colors.secondaryContainer,
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.70f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.brandBanner, shapes.button),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(CleanSupplyIcons.Offer, contentDescription = null, tint = colors.heroAccent, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.promoBannerTitle, style = type.titleSmall, color = colors.onSecondaryContainer)
                            Text(s.promoBannerSub, style = type.caption, color = colors.textSecondary)
                        }
                        Icon(CleanSupplyIcons.Forward, contentDescription = null, tint = colors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        onClick = onClick,
        shape = shapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.xxs, vertical = spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(colors.primaryContainer, shapes.smallComponent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = colors.primary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = type.labelSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
