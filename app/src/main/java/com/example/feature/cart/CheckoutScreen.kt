package com.example.feature.cart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Address
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.FulfillmentType
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.CheckoutSummary
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CheckoutDraft

@Composable
fun CheckoutScreen(
    summary: CheckoutSummary,
    profile: UserProfile?,
    addresses: List<Address>,
    activeAddress: Address?,
    zones: List<DeliveryZone>,
    activeZone: DeliveryZone?,
    draft: CheckoutDraft,
    isSubmitting: Boolean,
    onSelectAddress: (String) -> Unit,
    onSaveNewAddress: (String, String, String, String, String, String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onPaymentRefChange: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    onBack: () -> Unit,
    isGuest: Boolean = false,
    onSignInToCheckout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var showAddAddressDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val canUseCreditInvoice = profile != null &&
        profile.segmentEnum() != CustomerSegment.RETAIL &&
        profile.verificationEnum() == VerificationStatus.VERIFIED &&
        profile.availableCredit >= summary.grandTotal

    val deliverySlots = remember(s.isArabic) {
        if (s.isArabic) {
            listOf("اليوم 16:00 - 20:00", "غداً 09:00 - 13:00", "غداً 14:00 - 18:00")
        } else {
            listOf("Today 16:00 - 20:00", "Tomorrow 09:00 - 13:00", "Tomorrow 14:00 - 18:00")
        }
    }

    val activeStepIndex = when {
        !isGuest && activeAddress == null -> 0
        (draft.paymentMethod == PaymentMethod.CARD || draft.paymentMethod == PaymentMethod.MOBILE_WALLET) &&
            draft.paymentReference.isBlank() -> 1
        else -> 2
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("checkout_list"),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item(key = "checkout_steps_header") {
                StepProgressHeader(
                    steps = listOf(s.stepDelivery, s.stepPayment, s.stepReview),
                    activeStepIndex = activeStepIndex
                )
            }

            if (isGuest) {
                item(key = "guest_checkout_banner") {
                    Card(
                        shape = shapes.card,
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.28f)),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                        ) {
                            Text(s.guestSignInBannerTitle, style = type.titleSmall, color = colors.onPrimaryContainer)
                            Text(s.guestSignInBannerDesc, style = type.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                // Step 1: Delivery Address, Window & Fulfillment
                item(key = "step_delivery_card") {
                    Card(
                        shape = shapes.card,
                        border = BorderStroke(1.dp, colors.outline),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
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
                                Text(s.deliveryAddressTitle, style = type.titleMedium, color = colors.textPrimary)
                                TextButton(
                                    onClick = { showAddAddressDialog = true },
                                    modifier = Modifier.height(spacing.minTouchTarget)
                                ) {
                                    Icon(CleanSupplyIcons.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Text(s.addAddress, style = type.labelLarge, color = colors.primary)
                                }
                            }

                            if (addresses.isEmpty()) {
                                Surface(
                                    color = colors.warningContainer,
                                    shape = shapes.button,
                                    border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (s.isArabic) "أضف عنوان التوصيل لإتمام الطلب" else "Add a delivery address to place your order",
                                        style = type.bodySmall,
                                        color = colors.warning,
                                        modifier = Modifier.padding(spacing.sm)
                                    )
                                }
                            } else {
                                addresses.forEach { addr ->
                                    val selected = activeAddress?.id == addr.id
                                    val addrZone = zones.firstOrNull { it.id == addr.zoneId }
                                    Surface(
                                        onClick = { onSelectAddress(addr.id) },
                                        shape = shapes.button,
                                        color = if (selected) colors.primaryContainer else colors.surfaceVariant,
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) colors.interactive else colors.outline
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                                        ) {
                                            Icon(
                                                imageVector = CleanSupplyIcons.Location,
                                                contentDescription = null,
                                                tint = if (selected) colors.primary else colors.textSecondary
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${addr.label} • ${addrZone?.localizedName(s.isArabic) ?: addr.zoneId}",
                                                    style = type.labelLarge,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "${addr.streetDetails} (${addr.phone})",
                                                    style = type.bodySmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            if (selected) {
                                                Icon(CleanSupplyIcons.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Text(s.deliverySlotTitle, style = type.titleSmall, color = colors.textPrimary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                items(deliverySlots) { slot ->
                                    FilterChip(
                                        selected = draft.selectedSlot == slot,
                                        onClick = { onSelectSlot(slot) },
                                        shape = shapes.pill,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colors.surfaceVariant,
                                            selectedContainerColor = colors.primaryContainer,
                                            selectedLabelColor = colors.onPrimaryContainer
                                        ),
                                        label = { Text(slot, style = type.labelMedium) }
                                    )
                                }
                            }

                            activeZone?.let { z ->
                                Surface(
                                    color = colors.secondaryContainer,
                                    shape = shapes.button,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(spacing.sm),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(CleanSupplyIcons.Shipping, contentDescription = null, tint = colors.primary)
                                            Column {
                                                Text(s.fulfillmentPreviewTitle, style = type.caption, color = colors.textSecondary)
                                                Text(
                                                    text = if (z.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET) {
                                                        "${s.internalFleetLabel} • ${z.carrierDefaultName}"
                                                    } else {
                                                        "${s.thirdPartyCarrierLabel} • ${z.carrierDefaultName}"
                                                    },
                                                    style = type.bodySmall,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }
                                        Surface(color = colors.primaryContainer, shape = shapes.pill) {
                                            Text(
                                                text = "${z.estimatedHours} ${s.hoursShort}",
                                                style = type.numericSmall,
                                                color = colors.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = spacing.xs, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Payment Method
            item(key = "step_payment_card") {
                Card(
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(s.paymentMethodTitle, style = type.titleMedium, color = colors.textPrimary)

                        PaymentOptionRow(
                            label = s.paymentCod,
                            subtitle = s.paymentCodDesc,
                            icon = CleanSupplyIcons.paymentIcon(PaymentMethod.COD),
                            selected = draft.paymentMethod == PaymentMethod.COD,
                            enabled = true,
                            onClick = { onSelectPayment(PaymentMethod.COD) },
                            testTag = "pay_cod"
                        )
                        PaymentOptionRow(
                            label = s.paymentCard,
                            subtitle = s.paymentCardDesc,
                            icon = CleanSupplyIcons.paymentIcon(PaymentMethod.CARD),
                            selected = draft.paymentMethod == PaymentMethod.CARD,
                            enabled = true,
                            onClick = { onSelectPayment(PaymentMethod.CARD) },
                            testTag = "pay_card"
                        )
                        PaymentOptionRow(
                            label = s.paymentWallet,
                            subtitle = s.paymentWalletDesc,
                            icon = CleanSupplyIcons.paymentIcon(PaymentMethod.MOBILE_WALLET),
                            selected = draft.paymentMethod == PaymentMethod.MOBILE_WALLET,
                            enabled = true,
                            onClick = { onSelectPayment(PaymentMethod.MOBILE_WALLET) },
                            testTag = "pay_wallet"
                        )
                        PaymentOptionRow(
                            label = if (canUseCreditInvoice) {
                                "${s.paymentCreditInvoice} (${Formatters.money(profile.availableCredit, s.currency)})"
                            } else {
                                s.paymentCreditInvoice
                            },
                            subtitle = if (canUseCreditInvoice) null else s.creditLockedHint,
                            icon = CleanSupplyIcons.paymentIcon(PaymentMethod.CREDIT_INVOICE),
                            selected = draft.paymentMethod == PaymentMethod.CREDIT_INVOICE,
                            enabled = canUseCreditInvoice,
                            onClick = { if (canUseCreditInvoice) onSelectPayment(PaymentMethod.CREDIT_INVOICE) },
                            testTag = "pay_credit"
                        )
                    }
                }
            }

            if (draft.paymentMethod == PaymentMethod.CARD || draft.paymentMethod == PaymentMethod.MOBILE_WALLET) {
                item(key = "step_payment_reference") {
                    Card(
                        shape = shapes.card,
                        border = BorderStroke(1.dp, colors.info.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            OutlinedTextField(
                                value = draft.paymentReference,
                                onValueChange = onPaymentRefChange,
                                placeholder = { Text(s.paymentRefHint, style = type.bodyMedium) },
                                singleLine = true,
                                shape = shapes.input,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = colors.infoContainer,
                                shape = shapes.smallComponent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(spacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(CleanSupplyIcons.Info, contentDescription = null, tint = colors.info, modifier = Modifier.size(16.dp))
                                    Text(s.paymentPendingVerificationNote, style = type.caption, color = colors.info)
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Order Summary & Itemized Review
            item(key = "step_review_card") {
                Card(
                    shape = shapes.largeSurface,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(s.stepReview, style = type.titleMedium, color = colors.textPrimary)
                        summary.lines.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${line.cartItem.quantity}x ${line.product.localizedName(s.isArabic)}",
                                    style = type.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Formatters.money(line.lineEffectiveTotal, s.currency),
                                    style = type.numericSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        HorizontalDivider(color = colors.outline)
                        CartSummaryLine(s.subtotalLabel, Formatters.money(summary.retailSubtotal, s.currency))
                        if (summary.tierAndBulkSavings > 0.0) {
                            CartSummaryLine(s.tierSavingsLabel, "-${Formatters.money(summary.tierAndBulkSavings, s.currency)}", colors.success)
                        }
                        if (summary.couponDiscount > 0.0) {
                            CartSummaryLine(s.couponDiscountLabel, "-${Formatters.money(summary.couponDiscount, s.currency)}", colors.success)
                        }
                        if (summary.loyaltyDiscount > 0.0) {
                            CartSummaryLine(s.loyaltyDiscountLabel, "-${Formatters.money(summary.loyaltyDiscount, s.currency)}", colors.warning)
                        }
                        CartSummaryLine(
                            s.shippingFeeLabel,
                            if (summary.shippingFee == 0.0) s.freeShippingLabel else Formatters.money(summary.shippingFee, s.currency),
                            if (summary.shippingFee == 0.0) colors.success else colors.textPrimary
                        )
                        HorizontalDivider(color = colors.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.totalLabel, style = type.titleMedium, color = colors.textPrimary)
                            Text(
                                text = Formatters.money(summary.grandTotal, s.currency),
                                style = type.numericLarge,
                                color = colors.primary
                            )
                        }
                        if (summary.pointsEarned > 0L) {
                            Surface(
                                color = colors.warningContainer,
                                shape = shapes.smallComponent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(s.pointsEarnedPreview, style = type.labelSmall, color = colors.warning)
                                    Text("+${summary.pointsEarned} ${s.pointsUnit}", style = type.numericSmall, color = colors.warning)
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column {
                HorizontalDivider(color = colors.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(s.totalLabel, style = type.caption, color = colors.textSecondary)
                        Text(
                            text = Formatters.money(summary.grandTotal, s.currency),
                            style = type.numericMedium,
                            color = colors.primary
                        )
                    }
                    CleanSupplyPrimaryButton(
                        text = if (isGuest) s.signInToPlaceOrder else s.placeOrder,
                        icon = CleanSupplyIcons.Check,
                        isLoading = isSubmitting,
                        enabled = !summary.hasMoqViolation && (isGuest || activeAddress != null),
                        onClick = { if (isGuest) onSignInToCheckout() else onPlaceOrder() },
                        testTag = "place_order_button",
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            zones = zones,
            onDismiss = { showAddAddressDialog = false },
            onSave = { label, rec, phone, zoneId, street, floor ->
                onSaveNewAddress(label, rec, phone, zoneId, street, floor)
                showAddAddressDialog = false
            }
        )
    }
}

@Composable
private fun StepProgressHeader(
    steps: List<String>,
    activeStepIndex: Int
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        color = colors.surface,
        shape = shapes.card,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, label ->
                val doneOrCurrent = idx <= activeStepIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (doneOrCurrent) colors.interactive else colors.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = type.numericSmall,
                            color = if (doneOrCurrent) colors.onInteractive else colors.textSecondary
                        )
                    }
                    Text(
                        text = label,
                        style = type.labelSmall,
                        color = if (doneOrCurrent) colors.primary else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(
    label: String,
    subtitle: String?,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        onClick = { if (enabled) onClick() },
        shape = shapes.button,
        color = if (selected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) colors.interactive else colors.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            RadioButton(
                selected = selected,
                onClick = if (enabled) onClick else null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = colors.interactive)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = type.bodyMedium,
                    color = if (enabled) colors.textPrimary else colors.disabled
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = type.caption,
                        color = if (enabled) colors.textSecondary else colors.disabled
                    )
                }
            }
        }
    }
}

@Composable
fun AddAddressDialog(
    zones: List<DeliveryZone>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var label by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var selectedZoneId by remember(zones) { mutableStateOf(zones.firstOrNull()?.id ?: "zone_cairo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.dialog,
        containerColor = colors.surface,
        title = { Text(s.addAddress, style = type.titleLarge, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(if (s.isArabic) "التسمية (فرع/مخزن)" else "Label (Store/Branch)") },
                    singleLine = true,
                    shape = shapes.input
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(if (s.isArabic) "اسم المستلم" else "Recipient Name") },
                    singleLine = true,
                    shape = shapes.input
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (s.isArabic) "رقم الهاتف" else "Phone") },
                    singleLine = true,
                    shape = shapes.input
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    items(zones, key = { it.id }) { z ->
                        FilterChip(
                            selected = selectedZoneId == z.id,
                            onClick = { selectedZoneId = z.id },
                            label = { Text(z.localizedName(s.isArabic), style = type.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text(if (s.isArabic) "العنوان بالتفصيل" else "Street Address") },
                    singleLine = true,
                    shape = shapes.input
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text(if (s.isArabic) "الدور / الملاحظات" else "Building / Floor") },
                    singleLine = true,
                    shape = shapes.input
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (street.isNotBlank() || label.isNotBlank()) {
                        onSave(label, recipient, phone, selectedZoneId, street, floor)
                    }
                },
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = colors.interactive, contentColor = colors.onInteractive)
            ) {
                Text(s.saveBtn, style = type.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.closeBtn, style = type.labelLarge, color = colors.textSecondary)
            }
        }
    )
}
