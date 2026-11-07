package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineStep

@Composable
fun StepIllustrationStage(
    step: RoutineStep,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resId = step.drawableResName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
    } ?: 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = step.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            DefaultCartoonScene(step = step)
        }
    }
}

@Composable
private fun DefaultCartoonScene(
    step: RoutineStep,
    modifier: Modifier = Modifier
) {
    val (icon, bgCol, accentCol, funMessage) = when (step.iconKey.lowercase()) {
        "potty", "toilet", "restroom" -> Quad(Icons.Default.Wc, Color(0xFFFFEDD5), Color(0xFFFB923C), "Bathroom break and hygiene time!")
        "brush_teeth", "toothbrush", "teeth" -> Quad(Icons.Default.Brush, Color(0xFFCCFBF1), Color(0xFF0D9488), "Brush up and down for a sparkly smile!")
        "wash_hands", "wash" -> Quad(Icons.Default.CleanHands, Color(0xFFE0F2FE), Color(0xFF0284C7), "Wash with warm water and soap bubbles!")
        "drink_water", "water" -> Quad(Icons.Default.LocalDrink, Color(0xFFE0F2FE), Color(0xFF0284C7), "Refreshing sip of water!")
        "read_book", "book", "story" -> Quad(Icons.Default.AutoStories, Color(0xFFEDE9FE), Color(0xFF7C3AED), "Cozy story time & adventures!")
        "pick_book", "books" -> Quad(Icons.Default.LibraryBooks, Color(0xFFFEF3C7), Color(0xFFD97706), "Choose your favorite book for bedtime!")
        "pajamas", "pjs" -> Quad(Icons.Default.Nightlight, Color(0xFFF3E8FF), Color(0xFF9333EA), "Slip into comfy cozy pajamas!")
        "clothes", "dressed" -> Quad(Icons.Default.Checkroom, Color(0xFFDCFCE7), Color(0xFF16A34A), "Dressed and looking sharp!")
        "shoes", "coat" -> Quad(Icons.Default.DirectionsWalk, Color(0xFFFFEDD5), Color(0xFFEA580C), "Tying shoes and comfy jacket on!")
        "backpack", "bag" -> Quad(Icons.Default.Backpack, Color(0xFFE0F2FE), Color(0xFF0284C7), "Backpack packed and ready to go!")
        "breakfast", "food" -> Quad(Icons.Default.Restaurant, Color(0xFFFEF3C7), Color(0xFFF59E0B), "Yummy delicious fuel for the day!")
        "bath", "shower" -> Quad(Icons.Default.Bathtub, Color(0xFFE0F2FE), Color(0xFF0284C7), "Warm bubbly bath time!")
        "wake_up", "sun" -> Quad(Icons.Default.WbSunny, Color(0xFFFEF3C7), Color(0xFFF59E0B), "Rise and shine! Good morning sunshine!")
        "tidy", "clean" -> Quad(Icons.Default.CleaningServices, Color(0xFFDCFCE7), Color(0xFF16A34A), "Toys tidied up and room neat!")
        "bed", "sleep" -> Quad(Icons.Default.Bed, Color(0xFFF1F5F9), Color(0xFF475569), "Sweet dreams and restful sleep!")
        "medicine", "vitamins" -> Quad(Icons.Default.Medication, Color(0xFFFEE2E2), Color(0xFFDC2626), "Daily vitamins to stay strong!")
        "play", "fun" -> Quad(Icons.Default.SportsEsports, Color(0xFFFEF08A), Color(0xFFCA8A04), "Playtime fun!")
        "cuddle", "hug" -> Quad(Icons.Default.Favorite, Color(0xFFFFE4E6), Color(0xFFE11D48), "Warm hugs and family love!")
        "leave_home", "bus" -> Quad(Icons.Default.RocketLaunch, Color(0xFFF3E8FF), Color(0xFF9333EA), "Launch time! Ready for an awesome day!")
        else -> Quad(Icons.Default.WbSunny, Color(0xFFF1F5F9), Color(0xFF64748B), "Let's do this step together!")
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(240.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(bgCol, RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.White, CircleShape)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = step.title,
                    tint = accentCol,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = step.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = funMessage,
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
