package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Address
import com.example.data.model.Coupon
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.FulfillmentType
import com.example.data.model.PackMode
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.CheckoutSummary
import com.example.domain.PricedCartLine
import com.example.ui.components.formatMoney
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AquaContainer
import com.example.ui.theme.AquaTeal
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.CheckoutDraft

@Composable
fun CartCheckoutScreen(
    summary: CheckoutSummary,
    profile: UserProfile?,
    addresses: List<Address>,
    activeAddress: Address?,
    zones: List<DeliveryZone>,
    activeZone: DeliveryZone?,
    coupons: List<Coupon>,
    draft: CheckoutDraft,
    isSubmitting: Boolean,
    onBrowseCatalog: () -> Unit,
    onUpdateCartQty: (String, Long, PackMode) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onToggleLoyalty: (Boolean) -> Unit,
    onSelectAddress: (String) -> Unit,
    onSaveNewAddress: (String, String, String, String, String, String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onPaymentRefChange: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var couponInput by remember(draft.appliedCouponCode) { mutableStateOf(draft.appliedCouponCode) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    if (summary.lines.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = AquaTeal,
                    modifier = Modifier.size(64.dp)
                )
                Text(s.emptyCartTitle, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = s.emptyCartDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onBrowseCatalog,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("empty_cart_browse_btn")
                ) {
                    Text(s.browseCatalog)
                }
            }
        }
        return
    }

    val canUseCreditInvoice = profile != null &&
        profile.segmentEnum() != CustomerSegment.RETAIL &&
        profile.verificationEnum() == VerificationStatus.VERIFIED &&
        profile.availableCredit >= summary.grandTotal

    val deliverySlots = if (s.isArabic) {
        listOf("اليوم 16:00 - 20:00", "غداً 09:00 - 13:00", "غداً 14:00 - 18:00")
    } else {
        listOf("Today 16:00 - 20:00", "Tomorrow 09:00 - 13:00", "Tomorrow 14:00 - 18:00")
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(s.cartTitle, style = MaterialTheme.typography.headlineSmall)
        }

        // MOQ violation banner
        if (summary.hasMoqViolation) {
            item {
                Surface(
                    color = ErrorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                        Text(s.moqWarning, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                }
            }
        }

        // Free shipping progress
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shipMsg = if (summary.shippingFee == 0.0) {
                        if (s.isArabic) "مبروك! حصلت على شحن مجاني لهذه المنطقة" else "Unlocked free delivery for this zone!"
                    } else {
                        if (s.isArabic) "أضف ${formatMoney(summary.amountToFreeShipping, s.currency)} للحصول على شحن مجاني"
                        else "Add ${formatMoney(summary.amountToFreeShipping, s.currency)} more for free shipping"
                    }
                    Text(shipMsg, style = MaterialTheme.typography.labelLarge, color = AquaTeal)
                    LinearProgressIndicator(
                        progress = { summary.freeShippingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        }

        // Cart lines
        items(summary.lines, key = { it.cartItem.id }) { line ->
            CartLineCard(
                line = line,
                onUpdateQty = { newQty ->
                    onUpdateCartQty(line.product.id, newQty, line.cartItem.packModeEnum())
                }
            )
        }

        // Coupon & Loyalty section
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it },
                            placeholder = { Text(s.couponHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coupon_input")
                        )
                        if (draft.appliedCouponCode.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    couponInput = ""
                                    onRemoveCoupon()
                                },
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(s.removeCoupon)
                            }
                        } else {
                            Button(
                                onClick = { onApplyCoupon(couponInput) },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("apply_coupon_btn")
                            ) {
                                Text(s.applyCoupon)
                            }
                        }
                    }
                    if (draft.couponError) {
                        Text(s.invalidCoupon, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(coupons, key = { it.id }) { c ->
                            FilterChip(
                                selected = draft.appliedCouponCode.equals(c.code, ignoreCase = true),
                                onClick = {
                                    couponInput = c.code
                                    onApplyCoupon(c.code)
                                },
                                label = { Text("${c.code} (${c.localizedTitle(s.isArabic)})") }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.loyaltyRedeemLabel, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${profile?.loyaltyPoints ?: 0L} pts (${formatMoney((profile?.loyaltyPoints ?: 0L) / 10.0, s.currency)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberOnContainer
                            )
                        }
                        Switch(
                            checked = draft.redeemLoyalty,
                            enabled = (profile?.loyaltyPoints ?: 0L) >= 10L,
                            onCheckedChange = onToggleLoyalty,
                            modifier = Modifier.testTag("loyalty_redeem_switch")
                        )
                    }
                }
            }
        }

        // Delivery Address & Zone + Fulfillment Preview
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.deliveryAddressTitle, style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showAddAddressDialog = true }) {
                            Text(s.addAddress)
                        }
                    }

                    addresses.forEach { addr ->
                        val selected = activeAddress?.id == addr.id
                        val addrZone = zones.firstOrNull { it.id == addr.zoneId }
                        Surface(
                            onClick = { onSelectAddress(addr.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) AquaContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(1.5.dp, AquaTeal) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (selected) AquaTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${addr.label} • ${addrZone?.localizedName(s.isArabic) ?: addr.zoneId}", style = MaterialTheme.typography.labelLarge)
                                    Text("${addr.streetDetails} (${addr.phone})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Text(s.deliverySlotTitle, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(deliverySlots) { slot ->
                            FilterChip(
                                selected = draft.selectedSlot == slot,
                                onClick = { onSelectSlot(slot) },
                                label = { Text(slot) }
                            )
                        }
                    }

                    // Dual Fulfillment Indicator
                    activeZone?.let { z ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AquaTeal)
                                Column {
                                    Text(s.fulfillmentPreviewTitle, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = if (z.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET) {
                                            "${s.internalFleetLabel} • ${z.carrierDefaultName}"
                                        } else {
                                            "${s.thirdPartyCarrierLabel} • ${z.carrierDefaultName}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Payment Method Selector
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.paymentMethodTitle, style = MaterialTheme.typography.titleSmall)

                    PaymentOptionRow(
                        label = s.paymentCod,
                        selected = draft.paymentMethod == PaymentMethod.COD,
                        enabled = true,
                        onClick = { onSelectPayment(PaymentMethod.COD) },
                        testTag = "pay_cod"
                    )
                    PaymentOptionRow(
                        label = s.paymentCard,
                        selected = draft.paymentMethod == PaymentMethod.CARD,
                        enabled = true,
                        onClick = { onSelectPayment(PaymentMethod.CARD) },
                        testTag = "pay_card"
                    )
                    PaymentOptionRow(
                        label = s.paymentWallet,
                        selected = draft.paymentMethod == PaymentMethod.MOBILE_WALLET,
                        enabled = true,
                        onClick = { onSelectPayment(PaymentMethod.MOBILE_WALLET) },
                        testTag = "pay_wallet"
                    )
                    PaymentOptionRow(
                        label = if (canUseCreditInvoice) {
                            "${s.paymentCreditInvoice} (${formatMoney(profile?.availableCredit ?: 0.0, s.currency)})"
                        } else {
                            "${s.paymentCreditInvoice} — ${s.creditLockedHint}"
                        },
                        selected = draft.paymentMethod == PaymentMethod.CREDIT_INVOICE,
                        enabled = canUseCreditInvoice,
                        onClick = { if (canUseCreditInvoice) onSelectPayment(PaymentMethod.CREDIT_INVOICE) },
                        testTag = "pay_credit"
                    )

                    if (draft.paymentMethod == PaymentMethod.CARD || draft.paymentMethod == PaymentMethod.MOBILE_WALLET) {
                        OutlinedTextField(
                            value = draft.paymentReference,
                            onValueChange = onPaymentRefChange,
                            placeholder = { Text(s.paymentRefHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Totals & Place Order
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(s.subtotalLabel, formatMoney(summary.retailSubtotal, s.currency))
                    if (summary.tierAndBulkSavings > 0.0) {
                        SummaryRow(s.tierSavingsLabel, "-${formatMoney(summary.tierAndBulkSavings, s.currency)}", SuccessGreen)
                    }
                    if (summary.couponDiscount > 0.0) {
                        SummaryRow(s.couponDiscountLabel, "-${formatMoney(summary.couponDiscount, s.currency)}", SuccessGreen)
                    }
                    if (summary.loyaltyDiscount > 0.0) {
                        SummaryRow(s.loyaltyDiscountLabel, "-${formatMoney(summary.loyaltyDiscount, s.currency)}", AmberAccent)
                    }
                    SummaryRow(
                        s.shippingFeeLabel,
                        if (summary.shippingFee == 0.0) s.freeShippingLabel else formatMoney(summary.shippingFee, s.currency)
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.totalLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatMoney(summary.grandTotal, s.currency),
                            style = MaterialTheme.typography.titleLarge,
                            color = AquaTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onPlaceOrder,
                        enabled = !summary.hasMoqViolation && !isSubmitting && activeAddress != null,
                        colors = ButtonDefaults.buttonColors(containerColor = AquaTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("place_order_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(s.placeOrder, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
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
private fun CartLineCard(
    line: PricedCartLine,
    onUpdateQty: (Long) -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (!line.meetsMoq) BorderStroke(1.dp, ErrorRed) else null
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(line.product.localizedName(s.isArabic), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (line.cartItem.packModeEnum() == PackMode.CARTON) {
                            "${s.cartonMode} (${line.product.cartonSize}x) • ${formatMoney(line.effectivePackPrice, s.currency)}"
                        } else {
                            "${s.unitMode} • ${formatMoney(line.effectivePackPrice, s.currency)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!line.meetsMoq) {
                        Text(
                            text = "${s.moqLabel}: ${line.requiredMoqUnits}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    } else if (line.bulkDiscountApplied) {
                        Surface(color = AmberContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "-${line.product.bulkDiscountPercent}% ${s.bulkSavingsTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberOnContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onUpdateQty(0L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = ErrorRed)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateQty(line.cartItem.quantity - 1L) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = line.cartItem.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onUpdateQty(line.cartItem.quantity + 1L) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(AquaContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = AquaTeal)
                    }
                }

                Text(
                    text = formatMoney(line.lineEffectiveTotal, s.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AquaContainer.copy(alpha = 0.45f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = if (enabled) onClick else null, enabled = enabled)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun AddAddressDialog(
    zones: List<DeliveryZone>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    val s = LocalAppStrings.current
    var label by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var selectedZoneId by remember(zones) { mutableStateOf(zones.firstOrNull()?.id ?: "zone_cairo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addAddress) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(if (s.isArabic) "التسمية (فرع/مخزن)" else "Label (Store/Branch)") }, singleLine = true)
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text(if (s.isArabic) "اسم المستلم" else "Recipient Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(if (s.isArabic) "رقم الهاتف" else "Phone") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(zones, key = { it.id }) { z ->
                        FilterChip(
                            selected = selectedZoneId == z.id,
                            onClick = { selectedZoneId = z.id },
                            label = { Text(z.localizedName(s.isArabic)) }
                        )
                    }
                }
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text(if (s.isArabic) "العنوان بالتفصيل" else "Street Address") }, singleLine = true)
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text(if (s.isArabic) "الدور / الملاحظات" else "Building / Floor") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(label, recipient, phone, selectedZoneId, street, floor) }) {
                Text(s.saveBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.closeBtn) }
        }
    )
}
