package com.example.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Sanitizer
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod

/**
 * Centralized Material Rounded iconography mapping for consistent optical weight across CleanSupply.
 */
object CleanSupplyIcons {
    // Navigation (Active / Inactive pairs)
    val NavHomeActive: ImageVector = Icons.Rounded.Home
    val NavHomeInactive: ImageVector = Icons.Outlined.Home
    val NavCategoriesActive: ImageVector = Icons.Rounded.Category
    val NavCategoriesInactive: ImageVector = Icons.Outlined.Category
    val NavSearchActive: ImageVector = Icons.Rounded.Search
    val NavSearchInactive: ImageVector = Icons.Outlined.Search
    val NavCartActive: ImageVector = Icons.Rounded.ShoppingCart
    val NavCartInactive: ImageVector = Icons.Outlined.ShoppingCart
    val NavAccountActive: ImageVector = Icons.Rounded.Person
    val NavAccountInactive: ImageVector = Icons.Outlined.Person

    // Directional & Common Actions
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward
    val Login: ImageVector = Icons.AutoMirrored.Rounded.Login
    val Logout: ImageVector = Icons.AutoMirrored.Rounded.Logout
    val Add: ImageVector = Icons.Rounded.Add
    val Remove: ImageVector = Icons.Rounded.Remove
    val Delete: ImageVector = Icons.Rounded.DeleteOutline
    val Close: ImageVector = Icons.Rounded.Close
    val Copy: ImageVector = Icons.Rounded.ContentCopy
    val Reorder: ImageVector = Icons.Rounded.Replay
    val AddToCart: ImageVector = Icons.Rounded.AddShoppingCart
    val Search: ImageVector = Icons.Rounded.Search
    val SearchEmpty: ImageVector = Icons.Rounded.SearchOff
    val History: ImageVector = Icons.Rounded.History
    val Sort: ImageVector = Icons.Rounded.SwapVert
    val GridView: ImageVector = Icons.Rounded.GridView
    val ListView: ImageVector = Icons.Rounded.ViewAgenda
    val AllCategories: ImageVector = Icons.Rounded.Apps

    // Commerce & Status
    val Verified: ImageVector = Icons.Rounded.Verified
    val Pending: ImageVector = Icons.Rounded.HourglassTop
    val Rejected: ImageVector = Icons.Rounded.Cancel
    val Check: ImageVector = Icons.Rounded.CheckCircle
    val Warning: ImageVector = Icons.Rounded.WarningAmber
    val Error: ImageVector = Icons.Rounded.ErrorOutline
    val Info: ImageVector = Icons.Rounded.Info
    val Offline: ImageVector = Icons.Rounded.WifiOff
    val Offer: ImageVector = Icons.Rounded.LocalOffer
    val BulkDeal: ImageVector = Icons.Rounded.Bolt
    val Loyalty: ImageVector = Icons.Rounded.WorkspacePremium
    val Store: ImageVector = Icons.Rounded.Storefront
    val Business: ImageVector = Icons.Rounded.Business
    val Package: ImageVector = Icons.Rounded.Inventory2
    val Shipping: ImageVector = Icons.Rounded.LocalShipping
    val Location: ImageVector = Icons.Rounded.LocationOn
    val Schedule: ImageVector = Icons.Rounded.Schedule
    val Support: ImageVector = Icons.Rounded.SupportAgent
    val Admin: ImageVector = Icons.Rounded.AdminPanelSettings
    val Language: ImageVector = Icons.Rounded.Language
    val ThemeAuto: ImageVector = Icons.Rounded.BrightnessMedium
    val ThemeLight: ImageVector = Icons.Rounded.LightMode
    val ThemeDark: ImageVector = Icons.Rounded.DarkMode

    fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
        "disinfectants" -> Icons.Rounded.Sanitizer
        "paper" -> Icons.Rounded.Layers
        "tools" -> Icons.Rounded.HomeRepairService
        "industrial" -> Icons.Rounded.PrecisionManufacturing
        else -> Icons.Rounded.CleaningServices
    }

    fun paymentIcon(method: PaymentMethod): ImageVector = when (method) {
        PaymentMethod.COD -> Icons.Rounded.Payments
        PaymentMethod.CARD -> Icons.Rounded.CreditCard
        PaymentMethod.MOBILE_WALLET -> Icons.Rounded.AccountBalanceWallet
        PaymentMethod.CREDIT_INVOICE -> Icons.Rounded.ReceiptLong
    }

    fun orderStatusIcon(status: OrderStatus): ImageVector = when (status) {
        OrderStatus.PLACED -> Icons.Rounded.ReceiptLong
        OrderStatus.CONFIRMED -> Icons.Rounded.CheckCircle
        OrderStatus.PACKED -> Icons.Rounded.Inventory2
        OrderStatus.OUT_FOR_DELIVERY -> Icons.Rounded.LocalShipping
        OrderStatus.DELIVERED -> Icons.Rounded.Verified
        OrderStatus.CANCELLED -> Icons.Rounded.Cancel
    }
}
