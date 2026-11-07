package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.StarPrize
import com.example.model.StepRunHistory
import com.example.ui.theme.BonusAmber
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.SaveNavy

@Composable
fun PrizeChestDialog(
    earnedStars: Int,
    prizes: List<StarPrize>,
    runHistory: List<StepRunHistory> = emptyList(),
    onRedeem: (String) -> Unit,
    onAddPrize: (String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Weekly, 1: Monthly, 2: Independence
    var isAddingPrize by remember { mutableStateOf(false) }
    var newPrizeTitle by remember { mutableStateOf("") }
    var newPrizeStars by remember { mutableIntStateOf(15) }
    var newPrizeEmoji by remember { mutableStateOf("🎁") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("prize_chest_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Star Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Star Bank & Prizes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Earned by completing important tasks in time",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Star Balance Badge
                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(18.dp))
                        .background(BonusAmber, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$earnedStars Stars Available",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher: Weekly vs Monthly vs Independence Log
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .height(40.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Weekly",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == 0) Color(0xFF0284C7) else Color(0xFF64748B)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Monthly",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color(0xFF0284C7) else Color(0xFF64748B)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Independence",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == 2) Color(0xFF0284C7) else Color(0xFF64748B)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab in 0..1) {
                    // Prize List
                    val targetType = if (selectedTab == 0) "WEEKLY" else "MONTHLY"
                    val filteredPrizes = prizes.filter { it.type == targetType }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPrizes) { prize ->
                            PrizeCardRow(
                                prize = prize,
                                earnedStars = earnedStars,
                                onRedeem = { onRedeem(prize.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Custom Prize button & Done button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .clickable { isAddingPrize = !isAddingPrize }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom Prize", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Done")
                        }
                    }

                    // Add Prize In-Dialog Form
                    if (isAddingPrize) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("New ${if (selectedTab == 0) "Weekly" else "Monthly"} Prize", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newPrizeTitle,
                                    onValueChange = { newPrizeTitle = it },
                                    placeholder = { Text("e.g. Trip to Zoo or Extra Play") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cost: $newPrizeStars ⭐", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row {
                                        IconButton(onClick = { if (newPrizeStars > 5) newPrizeStars -= 5 }, modifier = Modifier.size(32.dp)) {
                                            Text("-5", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        }
                                        IconButton(onClick = { newPrizeStars += 5 }, modifier = Modifier.size(32.dp)) {
                                            Text("+5", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        if (newPrizeTitle.trim().isNotEmpty()) {
                                            onAddPrize(
                                                newPrizeTitle.trim(),
                                                newPrizeStars,
                                                if (selectedTab == 0) "WEEKLY" else "MONTHLY",
                                                newPrizeEmoji
                                            )
                                            newPrizeTitle = ""
                                            isAddingPrize = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Prize")
                                }
                            }
                        }
                    }
                } else {
                    // Independence Log View
                    val totalRuns = runHistory.size
                    val independentRuns = runHistory.count { !it.wasAssisted && !it.wasOvertime }
                    val assistedRuns = runHistory.count { it.wasAssisted }
                    val overtimeRuns = runHistory.count { it.wasOvertime && !it.wasAssisted }
                    val indepPercent = if (totalRuns > 0) (independentRuns * 100) / totalRuns else 0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    ) {
                        // Independence Summary Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "INDEPENDENCE SCORE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = if (totalRuns > 0) "$indepPercent% On Own" else "No runs yet",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (indepPercent >= 70) Color(0xFF16A34A) else Color(0xFFD97706)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("⭐ $independentRuns Solo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🤝 $assistedRuns Assisted", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        }
                                        if (overtimeRuns > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("⏳ $overtimeRuns Late", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                            }
                                        }
                                    }
                                }

                                if (totalRuns > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { independentRuns.toFloat() / totalRuns },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF16A34A),
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Run history entries
                        if (runHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No task runs recorded yet.\nStart a routine to track child independence!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(runHistory.reversed()) { run ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = run.stepTitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E293B)
                                                )
                                                val durM = run.actualSecondsTaken / 60
                                                val durS = run.actualSecondsTaken % 60
                                                Text(
                                                    text = "${durM}m ${durS}s taken (Goal: ${run.scheduledMinutes}m)",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            when {
                                                run.wasAssisted -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("🤝 Assisted (0⭐)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                                    }
                                                }
                                                run.wasOvertime -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("⏳ Overtime (0⭐)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                    }
                                                }
                                                else -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("⭐ Solo (+⭐)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrizeCardRow(
    prize: StarPrize,
    earnedStars: Int,
    onRedeem: () -> Unit
) {
    val canRedeem = earnedStars >= prize.starsNeeded && !prize.isRedeemed
    val progress = (earnedStars.toFloat() / prize.starsNeeded.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (prize.isRedeemed) Color(0xFFF1F5F9) else Color(0xFFF8FAFC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = prize.iconEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = prize.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (prize.isRedeemed) Color(0xFF64748B) else Color(0xFF1E293B)
                        )
                        Text(
                            text = "${prize.starsNeeded} Stars needed",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (prize.isRedeemed) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Claimed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                } else {
                    Button(
                        onClick = onRedeem,
                        enabled = canRedeem,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canRedeem) DoneButtonMagenta else Color(0xFFE2E8F0),
                            contentColor = if (canRedeem) Color.White else Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (canRedeem) "Claim! 🎉" else "${earnedStars}/${prize.starsNeeded} ⭐",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!prize.isRedeemed) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = BonusAmber,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
