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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_list"),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
        ) {
            // Greeting, Delivery Context & Account Tier Header
            item(key = "home_greeting", contentType = "header") {
                Surface(
                    color = colors.surface,
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(40.dp)
                                        .background(colors.primaryContainer, shapes.button),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CleanSupplyIcons.NavAccountActive,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = profile?.displayName?.takeIf { it.isNotBlank() } ?: s.guestBadge,
                                        style = type.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    SegmentVerificationBadge(profile)
                                }
                            }

                            Surface(
                                color = colors.warningContainer,
                                shape = shapes.pill,
                                border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(CleanSupplyIcons.Loyalty, contentDescription = null, tint = colors.warning, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "${profile?.loyaltyPoints ?: 0L} pts",
                                        style = type.numericSmall,
                                        color = colors.warning,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (activeZone != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                Icon(CleanSupplyIcons.Location, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = activeZone.localizedName(s.isArabic),
                                    style = type.caption,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Dedicated Search Entry Bar
                        Surface(
                            onClick = onOpenSearch,
                            shape = shapes.input,
                            color = colors.surfaceVariant,
                            border = BorderStroke(1.dp, colors.outline),
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
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Editorial Commercial Hero Block
            item(key = "home_hero", contentType = "hero") {
                Card(
                    shape = shapes.largeSurface,
                    colors = CardDefaults.cardColors(containerColor = colors.brandSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        ) {
                            FastProductImage(
                                drawableRes = R.drawable.img_hero_cleansupply,
                                contentDescription = s.heroTitle,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = colors.brandSlate.copy(alpha = 0.90f),
                                shape = shapes.pill,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(spacing.sm)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                                ) {
                                    Icon(CleanSupplyIcons.Verified, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                    Text(s.heroBadge, style = type.labelSmall, color = colors.onPrimary)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.padding(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Text(
                                text = s.heroTitle,
                                style = type.titleLarge,
                                color = colors.onPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = s.heroSubtitle,
                                style = type.bodySmall,
                                color = colors.onPrimary.copy(alpha = 0.80f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(spacing.xxs))
                            CleanSupplyPrimaryButton(
                                text = s.exploreBulkCta,
                                icon = CleanSupplyIcons.BulkDeal,
                                testTag = "home_explore_bulk_cta",
                                onClick = onExploreBulkDeals
                            )
                        }
                    }
                }
            }

            // Quick Reorder Strip if user has previous orders
            if (recentOrders.isNotEmpty()) {
                item(key = "home_quick_reorder", contentType = "banner") {
                    val latest = recentOrders.first()
                    Surface(
                        color = colors.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f)),
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
                                        .background(colors.primary.copy(alpha = 0.14f), shapes.button),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(CleanSupplyIcons.Reorder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
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

            // Categories Horizontal Shelf
            item(key = "home_categories", contentType = "categories") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionHeader(
                        title = s.categoriesTitle,
                        actionLabel = s.viewAll,
                        onAction = onOpenAllCategories
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(categories, key = { it.id }, contentType = { "category_chip" }) { cat ->
                            Surface(
                                shape = shapes.card,
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.outline),
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(shapes.card)
                                    .clickable { onSelectCategory(cat.id) }
                                    .testTag("home_category_${cat.id}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(78.dp)
                                    ) {
                                        FastProductImage(
                                            drawableRes = Formatters.productDrawable(cat.iconKey),
                                            contentDescription = cat.localizedName(s.isArabic),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = colors.surface.copy(alpha = 0.92f),
                                            shape = shapes.smallComponent,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(spacing.xs)
                                                .size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = CleanSupplyIcons.categoryIcon(cat.iconKey),
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = cat.localizedName(s.isArabic),
                                        style = type.labelLarge,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                                    )
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(82.dp)
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

            // Institutional & Promo Info Card
            item(key = "home_promo_info", contentType = "banner") {
                Surface(
                    color = colors.brandSlateSurface,
                    shape = shapes.card,
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
                                .background(colors.primary.copy(alpha = 0.22f), shapes.button),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(CleanSupplyIcons.Offer, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.promoBannerTitle, style = type.titleSmall, color = colors.onPrimary)
                            Text(s.promoBannerSub, style = type.caption, color = colors.onPrimary.copy(alpha = 0.78f))
                        }
                    }
                }
            }
        }
    }
}
