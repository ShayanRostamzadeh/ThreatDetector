package ShayanRostamzadeh.UniPassau.threatdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AbuseApiLimitWarningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbuseApiLimitWarningScreen()
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // Exit the app completely
        finishAffinity()
    }
}

@Composable
fun AbuseApiLimitWarningScreen() {
    // Infinite color transition between white and light red
    val infiniteTransition = rememberInfiniteTransition(label = "warningAnim")
    val backgroundColor by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color(0xFFFFCDD2), // light red
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgColorAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚠️All of your free AbuseIPDB API " +
                    "\n calls have been exhausted." +
                    "\n try again tomorrow.⚠️",
            color = Color.DarkGray,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
