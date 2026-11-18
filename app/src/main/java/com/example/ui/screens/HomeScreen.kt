package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Order
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.domain.PricingEngine
import com.example.ui.components.ProductCard
import com.example.ui.components.SegmentVerificationBadge
import com.example.ui.components.formatMoney
import com.example.ui.components.productDrawableForKey
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AquaContainer
import com.example.ui.theme.AquaTeal
import com.example.ui.theme.SlateNavy

@Composable
fun HomeScreen(
    profile: UserProfile?,
    categories: List<Category>,
    products: List<Product>,
    recentOrders: List<Order>,
    onExploreBulkDeals: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onReorder: (String) -> Unit,
    onOpenAllCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val featured = products.filter { it.featured }.ifEmpty { products.take(4) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Tier & Loyalty Status Strip
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentVerificationBadge(profile)
                        Text(
                            text = profile?.displayName ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = AmberContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(15.dp))
                            Text(
                                text = "${profile?.loyaltyPoints ?: 0L} pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_cleansupply),
                        contentDescription = s.heroTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SlateNavy.copy(alpha = 0.90f), SlateNavy.copy(alpha = 0.45f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = AquaTeal,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = s.heroBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.heroTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = s.heroSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 2
                            )
                        }
                        Button(
                            onClick = onExploreBulkDeals,
                            colors = ButtonDefaults.buttonColors(containerColor = AquaTeal),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_explore_bulk_cta")
                        ) {
                            Text(s.exploreBulkCta)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Quick Reorder Strip if user has previous orders
        if (recentOrders.isNotEmpty()) {
            item {
                val latest = recentOrders.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = AquaContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = s.quickReorderTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${latest.orderNumber} • ${formatMoney(latest.totalAmount, s.currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Button(
                            onClick = { onReorder(latest.id) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_quick_reorder")
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.reorderNow)
                        }
                    }
                }
            }
        }

        // Categories Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = s.categoriesTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        Card(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { onSelectCategory(cat.id) }
                                .testTag("home_category_${cat.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = productDrawableForKey(cat.iconKey)),
                                    contentDescription = cat.localizedName(s.isArabic),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                                Text(
                                    text = cat.localizedName(s.isArabic),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Promo Coupon Strip
        item {
            Surface(
                color = SlateNavy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AquaTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.promoBannerTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = s.promoBannerSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Featured Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = s.featuredTitle, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = onOpenAllCatalog,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(s.viewAll)
                }
            }
        }

        // Featured 2-column rows
        val rows = featured.chunked(2)
        items(rows.size) { rowIdx ->
            val pair = rows[rowIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { prod ->
                    ProductCard(
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
    }
}
