package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.ui.i18n.LocalAppStrings

data class AppShellDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScaffold(
    destinations: List<AppShellDestination>,
    currentRoute: String,
    cartBadgeCount: Int,
    isCheckoutRoute: Boolean,
    isConnected: Boolean,
    snackbarHostState: SnackbarHostState,
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    badgeLabel: String? = null,
    content: @Composable () -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val showNavigation = !isCheckoutRoute

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = colors.background,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    CleanSupplySnackbar(data = data)
                }
            },
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            if (isCheckoutRoute) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.size(spacing.minTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = CleanSupplyIcons.Back,
                                        contentDescription = s.backLabel,
                                        tint = colors.textPrimary
                                    )
                                }
                            }
                        },
                        title = {
                            if (isCheckoutRoute) {
                                Text(
                                    text = s.checkoutTitle,
                                    style = type.titleLarge,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                                ) {
                                    Surface(
                                        shape = shapes.smallComponent,
                                        color = colors.primaryContainer,
                                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.22f))
                                    ) {
                                        FastProductImage(
                                            drawableRes = R.drawable.ic_cleansupply_logo,
                                            contentDescription = s.appBrand,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(shapes.smallComponent)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = s.appBrand,
                                            style = type.titleMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!badgeLabel.isNullOrBlank()) {
                                        Surface(
                                            color = colors.primaryContainer,
                                            shape = shapes.pill,
                                            border = BorderStroke(0.8.dp, colors.primary.copy(alpha = 0.25f))
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                style = type.labelSmall,
                                                color = colors.onPrimaryContainer,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = spacing.xs, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.textPrimary
                        ),
                        actions = {
                            OutlinedButton(
                                onClick = onToggleLanguage,
                                shape = shapes.pill,
                                border = BorderStroke(1.dp, colors.outlineStrong),
                                contentPadding = PaddingValues(horizontal = spacing.sm, vertical = 0.dp),
                                modifier = Modifier
                                    .padding(end = spacing.sm)
                                    .height(40.dp)
                                    .testTag("top_language_toggle")
                            ) {
                                Icon(
                                    imageVector = CleanSupplyIcons.Language,
                                    contentDescription = s.switchLangLabel,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = s.switchLangLabel,
                                    style = type.labelMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.8.dp, color = colors.outline)
                    OfflineBanner(isOffline = !isConnected)
                }
            },
            bottomBar = {
                if (!useRail && showNavigation) {
                    Column {
                        HorizontalDivider(thickness = 0.8.dp, color = colors.outline)
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = colors.surface,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onNavigate(dest.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.primary,
                                        selectedTextColor = colors.primary,
                                        indicatorColor = colors.primaryContainer,
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textSecondary
                                    ),
                                    icon = {
                                        NavItemIcon(
                                            destination = dest,
                                            selected = selected,
                                            isCart = dest.route.endsWith("cart"),
                                            cartBadgeCount = cartBadgeCount
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = type.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && showNavigation) {
                    NavigationRail(
                        containerColor = colors.surface,
                        modifier = Modifier.padding(top = spacing.xs)
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onNavigate(dest.route) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.primary,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary
                                ),
                                icon = {
                                    NavItemIcon(
                                        destination = dest,
                                        selected = selected,
                                        isCart = dest.route.endsWith("cart"),
                                        cartBadgeCount = cartBadgeCount
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = type.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 960.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItemIcon(
    destination: AppShellDestination,
    selected: Boolean,
    isCart: Boolean,
    cartBadgeCount: Int
) {
    val colors = CleanSupplyTheme.colors
    val type = CleanSupplyTheme.typography
    val iconVector = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (isCart && cartBadgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Text(
                        text = if (cartBadgeCount > 99) "99+" else cartBadgeCount.toString(),
                        style = type.caption
                    )
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = destination.label)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = destination.label)
    }
}

@Composable
private fun CleanSupplySnackbar(data: SnackbarData) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        shape = shapes.card,
        color = colors.brandSlate,
        contentColor = colors.onPrimary,
        border = BorderStroke(1.dp, colors.heroAccent.copy(alpha = 0.35f)),
        shadowElevation = CleanSupplyTheme.elevations.floating,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = CleanSupplyIcons.Check,
                contentDescription = null,
                tint = colors.heroAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = data.visuals.message,
                style = type.bodyMedium,
                color = colors.onPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
