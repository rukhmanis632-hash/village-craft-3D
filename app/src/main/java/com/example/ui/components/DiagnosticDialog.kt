package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.CheckStatus
import com.example.auth.DiagnosticReport
import com.example.auth.FirebaseDiagnostics
import kotlinx.coroutines.launch

@Composable
fun FirebaseDiagnosticModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<DiagnosticReport?>(null) }

    LaunchedEffect(Unit) {
        isRunning = true
        report = FirebaseDiagnostics.runFullDiagnostics(context)
        isRunning = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD9080E0B))
            .padding(16.dp)
            .testTag("firebase_diagnostic_modal"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .heightIn(max = 650.dp)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4B453F), Color(0xFF2D2925), Color(0xFF1A1715))
                    )
                )
                .border(3.dp, GoldMid, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🩺 FIREBASE & NETWORK DIAGNOSTICS",
                        color = GoldLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Auth • Credential Manager • Network Probes • Session Logs",
                        color = Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }
                StoneGoldBilingualButton(
                    enLabel = "✕",
                    testTag = "close_diagnostics_btn",
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = GoldLight,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Checking Firebase Auth, CredentialManager & HTTPS endpoints...",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                val currentReport = report
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentReport != null) {
                        // Section 1: Dependency & SDK Initialization Checks
                        Text(
                            text = "1. SDK & DEPENDENCY CHECKS",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        currentReport.checks.forEach { item ->
                            val badgeColor = when (item.status) {
                                CheckStatus.PASS -> Color(0xFF56E364)
                                CheckStatus.WARN -> Color(0xFFFFC940)
                                CheckStatus.FAIL -> Color(0xFFFF6B81)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x66000000))
                                    .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.category}: ${item.title}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = item.status.name,
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = item.detail,
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Section 2: Live HTTPS Network Probes
                        Text(
                            text = "2. LIVE GOOGLE & FIREBASE HTTPS PROBES",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        currentReport.endpointProbes.forEach { probe ->
                            val okColor = if (probe.reachable) Color(0xFF56E364) else Color(0xFFFF6B81)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x66000000))
                                    .border(1.dp, okColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = probe.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (probe.reachable) "OK (${probe.latencyMs}ms • HTTP ${probe.httpStatus})" else "FAILED (${probe.latencyMs}ms)",
                                        color = okColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = probe.url,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (!probe.errorDetail.isNullOrBlank()) {
                                    Text(
                                        text = "Error: ${probe.errorDetail}",
                                        color = Color(0xFFFF8FA3),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Section 3: Detailed Session Error Log
                        Text(
                            text = "3. CURRENT SESSION NETWORK & AUTH ERROR LOG (${currentReport.recentErrors.size})",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (currentReport.recentErrors.isEmpty()) {
                            Text(
                                text = "No session errors recorded yet. All diagnostic events are also written to Logcat tag 'FirebaseDiag'.",
                                color = Color(0xFF98F584),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x55000000))
                                    .padding(10.dp)
                            )
                        } else {
                            currentReport.recentErrors.forEach { err ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x661C060A))
                                        .border(1.dp, Color(0xFFFF6B81), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "[${err.timestampFormatted}] ${err.subsystem} • ${err.operation}",
                                        color = Color(0xFFFF8FA3),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${err.exceptionType} (${err.errorCode}): ${err.message}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Net: ${err.networkStateSummary}",
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Fix: ${err.remediationHint}",
                                        color = Color(0xFFFFD269),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoneGoldBilingualButton(
                    enLabel = "🔄 RE-RUN CHECKS",
                    hiLabel = "फिर से जाँचें",
                    testTag = "rerun_diagnostics_btn",
                    isHighlighted = true,
                    onClick = {
                        scope.launch {
                            isRunning = true
                            report = FirebaseDiagnostics.runFullDiagnostics(context)
                            isRunning = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                )
                StoneGoldBilingualButton(
                    enLabel = "CLOSE",
                    hiLabel = "बंद करें",
                    testTag = "dismiss_diagnostics_footer_btn",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                )
            }
        }
    }
}
