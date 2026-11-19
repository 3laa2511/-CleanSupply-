package com.example.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.FastProductImage
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.ui.i18n.LocalAppStrings
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Credential Manager")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun BrandedLaunchSplash(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.brandSlate),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Surface(
                    shape = shapes.largeSurface,
                    color = colors.surface.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, colors.onPrimary.copy(alpha = 0.16f))
                ) {
                    FastProductImage(
                        drawableRes = R.drawable.ic_cleansupply_logo,
                        contentDescription = s.appBrand,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(shapes.largeSurface)
                    )
                }
                Text(s.appBrand, style = type.headlineMedium, color = colors.onPrimary)
                Text(s.appTagline, style = type.bodySmall, color = colors.onPrimary.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
fun AuthScreen(
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entered = true
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "authLogoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Top bar with Brand Logo & Language Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                ) {
                    Surface(
                        shape = shapes.button,
                        border = BorderStroke(1.dp, colors.outline),
                        color = colors.surface
                    ) {
                        FastProductImage(
                            drawableRes = R.drawable.ic_cleansupply_logo,
                            contentDescription = s.appBrand,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(shapes.button)
                        )
                    }
                    Column {
                        Text(
                            text = s.appBrand,
                            style = type.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (s.isArabic) "جملة • شركات • تجزئة" else "B2B • Wholesale • Retail",
                            style = type.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onToggleLanguage,
                    shape = shapes.pill,
                    border = BorderStroke(1.dp, colors.outlineStrong),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = spacing.sm, vertical = 0.dp),
                    modifier = Modifier
                        .height(spacing.minTouchTarget)
                        .testTag("auth_language_toggle")
                ) {
                    Icon(CleanSupplyIcons.Language, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text(s.switchLangLabel, style = type.labelMedium)
                }
            }

            // Editorial Welcome & Authentication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = shapes.largeSurface,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = CleanSupplyTheme.elevations.subtle),
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(shapes.card)
                    ) {
                        FastProductImage(
                            drawableRes = R.drawable.img_hero_cleansupply,
                            contentDescription = s.appBrand,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = colors.brandSlate.copy(alpha = 0.90f),
                            shape = shapes.pill,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                            ) {
                                Icon(
                                    imageVector = CleanSupplyIcons.Verified,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = s.heroBadge,
                                    style = type.labelSmall,
                                    color = colors.onPrimary
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = s.signInTitle,
                            style = type.headlineSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = s.signInSubtitle,
                            style = type.bodyMedium,
                            color = colors.textSecondary
                        )
                    }

                    Surface(
                        color = colors.surfaceVariant,
                        shape = shapes.card,
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            AuthValuePropRow(
                                icon = CleanSupplyIcons.Store,
                                label = if (s.isArabic) "تجزئة + جملة + تعاقدات شركات" else "Retail + Wholesale + Institutional Pricing"
                            )
                            HorizontalDivider(color = colors.outline)
                            AuthValuePropRow(
                                icon = CleanSupplyIcons.Shipping,
                                label = if (s.isArabic) "أسطول الشركة + شركات الشحن المعتمدة" else "Company Fleet + Authorized Freight Carriers"
                            )
                            HorizontalDivider(color = colors.outline)
                            AuthValuePropRow(
                                icon = CleanSupplyIcons.Loyalty,
                                label = if (s.isArabic) "نقاط ولاء، كوبونات، وفاتورة آجلة" else "Loyalty Rewards, Coupons & B2B Credit"
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = colors.errorContainer,
                            shape = shapes.button,
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                Icon(CleanSupplyIcons.Error, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = type.bodySmall,
                                    color = colors.error
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        CleanSupplyPrimaryButton(
                            text = s.signInWithGoogle,
                            isLoading = isLoading,
                            icon = CleanSupplyIcons.Login,
                            testTag = "google_sign_in_button",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            }
                        )

                        CleanSupplySecondaryButton(
                            text = s.continueAsGuest,
                            enabled = !isLoading,
                            icon = CleanSupplyIcons.NavAccountInactive,
                            testTag = "continue_as_guest_button",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onContinueAsGuest
                        )
                    }

                    Text(
                        text = s.signInNote,
                        style = type.caption,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthValuePropRow(icon: ImageVector, label: String) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.primaryContainer, shapes.smallComponent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = type.bodyMedium,
            color = colors.textPrimary
        )
    }
}
