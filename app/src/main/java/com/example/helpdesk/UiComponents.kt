package com.example.helpdesk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpdesk.data.Ticket
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.example.helpdesk.data.Notification
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.height

// Brand colors matching the CampusFix design
val IndigoPrimary: Color get() = ThemeManager.primary
val VioletAccent: Color get() = ThemeManager.accent
val SlateBackground = Color(0xFFF8FAFC)

private data class StatusStyle(val bg: Color, val text: Color, val label: String)

private fun statusStyle(status: String): StatusStyle = when (status) {
    "OPEN" -> StatusStyle(Color(0xFFFEF3C7), Color(0xFF92400E), "Pending")
    "IN_PROGRESS" -> StatusStyle(Color(0xFFDBEAFE), Color(0xFF1E40AF), "In Progress")
    "RESOLVED" -> StatusStyle(Color(0xFFD1FAE5), Color(0xFF065F46), "Resolved")
    else -> StatusStyle(Color(0xFFF1F5F9), Color(0xFF475569), status)
}

data class CategoryMeta(val key: String, val label: String, val icon: String)

val CATEGORIES = listOf(
    CategoryMeta("ELECTRICAL", "Electrical", "⚡"),
    CategoryMeta("PLUMBING", "Plumbing", "🚰"),
    CategoryMeta("IT", "IT / Wifi", "💻"),
    CategoryMeta("FURNITURE", "Furniture", "🪑"),
    CategoryMeta("OTHER", "Other", "📋")
)

fun categoryMeta(key: String): CategoryMeta = CATEGORIES.find { it.key == key } ?: CATEGORIES.last()
@Composable
fun StatusPill(status: String) {
    val style = statusStyle(status)
    Text(
        text = style.label,
        color = style.text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(style.bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun StarsReadOnly(rating: Int) {
    Row {
        repeat(5) { i ->
            Text(
                if (i < rating) "★" else "☆",
                color = if (i < rating) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StarsPicker(onRate: (Int) -> Unit) {
    Row {
        repeat(5) { i ->
            Text(
                "☆",
                color = Color(0xFFFBBF24),
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable { onRate(i + 1) }
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
fun TicketCard(
    ticket: Ticket,
    subtitle: String? = null,
    allowRating: Boolean = false,
    onRate: (Int) -> Unit = {},
    onChatClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val cat = categoryMeta(ticket.category)
            Text("${cat.icon} ${cat.label}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }

            if (!ticket.attachmentUrl.isNullOrBlank()) {
                val bitmap = remember(ticket.attachmentUrl) { base64ToBitmap(ticket.attachmentUrl) }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Ticket photo",
                        modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)
                    )
                }
            }

            StatusPill(ticket.status)
            if (!ticket.assignedStaffEmail.isNullOrBlank()) {
                Text(
                    "Claimed by: ${ticket.assignedStaffEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IndigoPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (onChatClick != null) {
                    Text(
                        "💬 Open Chat",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp).clickable { onChatClick() }
                    )
                }
            }
            if (!ticket.staffReply.isNullOrBlank()) {
                Text(
                    "Reply: ${ticket.staffReply}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (ticket.status == "RESOLVED") {
                if (ticket.rating != null) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Your rating", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        StarsReadOnly(ticket.rating)
                    }
                } else if (allowRating) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Rate this resolution", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        StarsPicker(onRate = onRate)
                    }
                }
            }
        }
    }
}

@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(IndigoPrimary, VioletAccent)
                ),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = Color(0xFFE0E7FF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (trailingContent != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                trailingContent()
            }
        }
    }
}

@Composable
fun DonutChart(
    resolved: Int,
    inProgress: Int,
    open: Int,
    modifier: Modifier = Modifier
) {
    val total = resolved + inProgress + open
    val started = remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { started.value = true }

    val spec = androidx.compose.animation.core.tween<Float>(durationMillis = 800)
    val aResolved = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (started.value) resolved.toFloat() else 0f,
        animationSpec = spec, label = "resolved"
    ).value
    val aInProgress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (started.value) inProgress.toFloat() else 0f,
        animationSpec = spec, label = "inProgress"
    ).value
    val aOpen = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (started.value) open.toFloat() else 0f,
        animationSpec = spec, label = "open"
    ).value
    val aTotal = aResolved + aInProgress + aOpen
    val denominator = if (total > 0) total.toFloat() else 1f

    Box(modifier = modifier.size(110.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            var startAngle = -90f
            listOf(
                aResolved to Color(0xFF10B981),
                aInProgress to Color(0xFF0EA5E9),
                aOpen to Color(0xFFF59E0B)
            ).forEach { (value, color) ->
                val sweep = 360f * value / denominator
                if (sweep > 0.5f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("${(aTotal + 0.5f).toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            Text("tickets", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun StatsPanel(tickets: List<Ticket>) {
    val resolved = tickets.count { it.status == "RESOLVED" }
    val inProgress = tickets.count { it.status == "IN_PROGRESS" }
    val open = tickets.count { it.status == "OPEN" }
    val total = tickets.size
    val resolutionRate = if (total > 0) resolved / total.toFloat() else 0f

    val started = remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { started.value = true }
    val rate = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (started.value) resolutionRate else 0f,
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "rate"
    ).value

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            DonutChart(resolved = resolved, inProgress = inProgress, open = open)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                StatRow("Resolved", resolved, Color(0xFF10B981))
                StatRow("In progress", inProgress, Color(0xFF0EA5E9))
                StatRow("Pending", open, Color(0xFFF59E0B))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resolution Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
            Text(
                "${(rate * 100 + 0.5f).toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(6.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rate.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(Color(0xFF34D399), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, color: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Text(label, fontSize = 11.sp, color = Color(0xFF475569), modifier = Modifier.padding(start = 6.dp))
        Text("$value", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(start = 8.dp))
    }
}

enum class StudentTab(val label: String, val icon: String) {
    HOME("Home", "🏠"),
    ISSUES("My Issues", "📋"),
    NEW("Report", "➕"),
    PROFILE("Profile", "👤")
}

@Composable
fun StudentTabBar(selectedTab: StudentTab, onTabSelected: (StudentTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround
    ) {
        StudentTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabSelected(tab) }
            ) {
                if (tab == StudentTab.NEW) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(IndigoPrimary, VioletAccent)
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(tab.icon, fontSize = 16.sp, color = Color.White)
                    }
                } else {
                    Text(tab.icon, fontSize = 18.sp)
                }
                Text(
                    tab.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) IndigoPrimary else Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable { onClick() }) {
        Text("🔔", fontSize = 20.sp, color = Color.White)
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFFF43F5E), RoundedCornerShape(50))
                    .align(androidx.compose.ui.Alignment.TopEnd),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    if (unreadCount > 9) "9+" else "$unreadCount",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NotificationListItem(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) Color.White else Color(0xFFEEF2FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.read) 1.dp else 3.dp)
    ) {
        Text(
            notification.message,
            modifier = Modifier.padding(14.dp),
            fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold
        )
    }
}