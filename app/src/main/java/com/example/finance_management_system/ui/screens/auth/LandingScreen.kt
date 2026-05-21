package com.example.finance_management_system.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.finance_management_system.ui.components.AppScaffold

@Composable
fun LandingScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    AppScaffold(
        title = "",
        currentRoute = null,
        showBottomBar = false,
        onBottomNavClick = {},
        showTopBar = false,
    ) { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FlowLedgerMark()
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Welcome to FlowLedger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Track your income, expenses, and goals in one place.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowLedgerMark() {
    Box(
        modifier = Modifier.size(width = 104.dp, height = 116.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 82.dp, height = 46.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF5E9BFF), Color(0xFF335CFF), Color(0xFF47DCC4)),
                    ),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 22.dp),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 82.dp, height = 34.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3E4BFF), Color(0xFF49D5C6)),
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, bottomEnd = 20.dp),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 44.dp, height = 56.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3348F1), Color(0xFF39D0B8)),
                    ),
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 24.dp),
                ),
        )
    }
}
