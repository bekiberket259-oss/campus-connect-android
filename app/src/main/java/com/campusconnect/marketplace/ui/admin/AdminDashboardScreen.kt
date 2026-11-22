package com.campusconnect.marketplace.ui.admin

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campusconnect.marketplace.data.AdminUser
import com.campusconnect.marketplace.data.Category
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.Report
import com.campusconnect.marketplace.data.VerificationRequest
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val requests by adminViewModel.requests.collectAsState()
    val reports by adminViewModel.reports.collectAsState()
    val users by adminViewModel.users.collectAsState()
    val categories by adminViewModel.categories.collectAsState()
    val listings by adminViewModel.listings.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Admin Console", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AdminStatCard(title = "Users", count = users.size, icon = Icons.Default.People)
                        AdminStatCard(title = "Requests", count = requests.size, icon = Icons.Default.Verified)
                        AdminStatCard(title = "Reports", count = reports.size, icon = Icons.Default.Warning)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GreenPrimary,
                edgePadding = 0.dp
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Security, null) }, text = { Text("Verify", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Flag, null) }, text = { Text("Reports", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.People, null) }, text = { Text("Users", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Category, null) }, text = { Text("Tags", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.PieChart, null) }, text = { Text("Stats", fontWeight = FontWeight.Bold) })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary) }
            } else {
                when (selectedTab) {
                    0 -> VerificationsTab(requests, adminViewModel)
                    1 -> ReportsTab(reports, adminViewModel)
                    2 -> UsersTab(users, adminViewModel)
                    3 -> CategoriesTab(categories, adminViewModel)
                    4 -> StatsTab(users, listings, categories)
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(100.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

// ======================= STATS TAB =======================

@Composable
fun StatsTab(
    users: List<AdminUser>,
    listings: List<Listing>,
    categories: List<Category>
) {
    if (users.isEmpty()) {
        EmptyState("No analytics data available", Icons.Default.PieChart)
        return
    }

    val totalUsers = users.size
    val verifiedUsers = users.count { it.verification_status == "verified" }
    val pendingUsers = users.count { it.verification_status == "pending_verification" }
    val rejectedUsers = users.count { it.verification_status == "rejected" }

    val totalListings = listings.size
    val soldListings = listings.count { it.status == "sold" }

    var animationPlayed by remember { mutableStateOf(false) }

    val animateVerified by animateFloatAsState(
        targetValue = if (animationPlayed) (verifiedUsers.toFloat() / totalUsers) * 100f else 0f,
        animationSpec = tween(1000), label = "verified"
    )
    val animatePending by animateFloatAsState(
        targetValue = if (animationPlayed) (pendingUsers.toFloat() / totalUsers) * 100f else 0f,
        animationSpec = tween(1000), label = "pending"
    )
    val animateRejected by animateFloatAsState(
        targetValue = if (animationPlayed) (rejectedUsers.toFloat() / totalUsers) * 100f else 0f,
        animationSpec = tween(1000), label = "rejected"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    val verifiedColor = Color(0xFF4CAF50)
    val pendingColor = Color(0xFFFF9800)
    val rejectedColor = Color(0xFFF44336)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Listings",
                    value = totalListings.toString(),
                    icon = Icons.Default.Inventory,
                    color = GreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Items Sold",
                    value = soldListings.toString(),
                    icon = Icons.Default.ShoppingBag,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Student Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 36f
                            val canvasSize = size.minDimension
                            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                startAngle = 0f, sweepAngle = 360f,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = verifiedColor, startAngle = -90f,
                                sweepAngle = (animateVerified / 100f) * 360f,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = pendingColor, startAngle = -90f + (animateVerified / 100f) * 360f,
                                sweepAngle = (animatePending / 100f) * 360f,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = rejectedColor, startAngle = -90f + ((animateVerified + animatePending) / 100f) * 360f,
                                sweepAngle = (animateRejected / 100f) * 360f,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(totalUsers.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("Students", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem("Verified", verifiedUsers, animateVerified, verifiedColor)
                        LegendItem("Pending", pendingUsers, animatePending, pendingColor)
                        LegendItem("Rejected", rejectedUsers, animateRejected, rejectedColor)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Listings by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    categories.forEach { category ->
                        val count = listings.count { it.category_id == category.id }
                        if (count > 0) {
                            CategoryBar(
                                name = category.name_en,
                                count = count,
                                maxCount = listings.maxOfOrNull { l -> listings.count { it.category_id == l.category_id } } ?: 1
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (listings.isEmpty()) {
                        Text("No listings data yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, percent: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
        Text(
            text = "${count} (%.0f%%)".format(percent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryBar(name: String, count: Int, maxCount: Int) {
    var animationPlayed by remember { mutableStateOf(false) }
    val widthFraction by animateFloatAsState(
        targetValue = if (animationPlayed) (count.toFloat() / maxCount) else 0f,
        animationSpec = tween(800), label = "bar"
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GreenPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(GreenPrimary, Color(0xFF4CAF50))))
            )
        }
    }
}

// ======================= EXISTING TABS =======================

@Composable
fun VerificationsTab(requests: List<VerificationRequest>, adminViewModel: AdminViewModel) {
    if (requests.isEmpty()) {
        EmptyState("No pending verifications", Icons.Default.CheckCircle)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requests) { request ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GreenPrimary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Text(request.full_name.firstOrNull()?.uppercase() ?: "S", color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(request.full_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("ID: ${request.student_id_number} | ${request.department}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { adminViewModel.approve(request.id!!) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Approve", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = { adminViewModel.reject(request.id!!) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reject", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsTab(reports: List<Report>, adminViewModel: AdminViewModel) {
    if (reports.isEmpty()) {
        EmptyState("No open reports", Icons.Default.ThumbUp)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reports) { report ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Report", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reason: ${report.reason.replace("_", " ").uppercase()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!report.comment.isNullOrBlank()) { Text("\"${report.comment}\"", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                        Text("Listing ID: ${report.listing_id.take(8)}...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { adminViewModel.resolveReportAndRemoveListing(report.id!!, report.listing_id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove Listing", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = { adminViewModel.dismissReport(report.id!!) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsersTab(users: List<AdminUser>, adminViewModel: AdminViewModel) {
    if (users.isEmpty()) {
        EmptyState("No users found", Icons.Default.People)
        return
    }

    // FOR DEBUGGING
    val context = LocalContext.current

    var userToDelete by remember { mutableStateOf<AdminUser?>(null) }
    var userToSuspend by remember { mutableStateOf<AdminUser?>(null) }

    // DELETE DIALOG
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User?") },
            text = { Text("Are you sure you want to delete ${userToDelete?.full_name}? All their data will be permanently removed.") },
            confirmButton = {
                Button(onClick = { userToDelete?.let { adminViewModel.deleteUser(it.id) }; userToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancel") } }
        )
    }

    // PREMIUM SUSPEND DIALOG
    if (userToSuspend != null) {
        SuspendUserDialog(
            user = userToSuspend!!,
            onDismiss = { userToSuspend = null },
            onConfirm = { days ->
                Toast.makeText(context, "Suspending user...", Toast.LENGTH_SHORT).show()
                adminViewModel.toggleSuspension(userToSuspend!!.id, true, days)
                userToSuspend = null
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(users) { user ->
            val status = user.verification_status ?: "pending_verification"

            // SAFE DATE PARSING FOR SUPABASE TIMESTAMPS
            val isActuallySuspended = remember(user.is_suspended, user.suspended_until) {
                if (user.is_suspended != true) {
                    false
                } else if (user.suspended_until == null) {
                    true // Permanent
                } else {
                    try {
                        // Supabase returns timestamps like 2023-10-27T10:00:00+00:00
                        val until = OffsetDateTime.parse(user.suspended_until)
                        until.isAfter(OffsetDateTime.now())
                    } catch (e: Exception) {
                        true // If parsing fails, assume suspended
                    }
                }
            }

            val statusColor = if (isActuallySuspended) Color.Red else if (status == "verified") Color(0xFF4CAF50) else if (status == "rejected") Color(0xFFF44336) else Color(0xFFFF9800)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = if (isActuallySuspended) BorderStroke(1.dp, Color.Red) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "User", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.full_name ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    if (isActuallySuspended) "SUSPENDED" else status.replace("_", " ").uppercase(),
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { userToDelete = user }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SUSPEND / MANUAL UNLOCK BUTTON
                        Button(
                            onClick = {
                                if (isActuallySuspended) {
                                    Toast.makeText(context, "Unlocking...", Toast.LENGTH_SHORT).show()
                                    adminViewModel.toggleSuspension(user.id, false, null)
                                } else {
                                    Toast.makeText(context, "Opening Dialog", Toast.LENGTH_SHORT).show()
                                    userToSuspend = user
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActuallySuspended) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = if (isActuallySuspended) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Lock",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isActuallySuspended) "Unlock" else "Suspend", fontSize = 12.sp)
                        }

                        if (!isActuallySuspended) {
                            if (status == "verified") {
                                OutlinedButton(
                                    onClick = { adminViewModel.updateVerificationStatus(user.id, "rejected") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = "Block", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unverify", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { adminViewModel.updateVerificationStatus(user.id, "verified") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Verify", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM SUSPEND DIALOG
// ==========================================
@Composable
fun SuspendUserDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedDays by remember { mutableStateOf<Int?>(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Suspend",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Suspend User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose a duration for ${user.full_name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SuspendOption(
                        title = "24 Hours",
                        subtitle = "Quick cool-down period",
                        icon = Icons.Default.Timer,
                        isSelected = selectedDays == 1,
                        onClick = { selectedDays = 1 }
                    )
                    SuspendOption(
                        title = "7 Days",
                        subtitle = "Standard suspension",
                        icon = Icons.Default.DateRange,
                        isSelected = selectedDays == 7,
                        onClick = { selectedDays = 7 }
                    )
                    SuspendOption(
                        title = "30 Days",
                        subtitle = "Severe violation",
                        icon = Icons.Default.CalendarToday,
                        isSelected = selectedDays == 30,
                        onClick = { selectedDays = 30 }
                    )
                    SuspendOption(
                        title = "Permanent Ban",
                        subtitle = "Account permanently locked",
                        icon = Icons.Default.Gavel,
                        isSelected = selectedDays == null,
                        isDestructive = true,
                        onClick = { selectedDays = null }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedDays) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SuspendOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected && isDestructive -> MaterialTheme.colorScheme.error
        isSelected -> GreenPrimary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    val backgroundColor = when {
        isSelected && isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
        isSelected -> GreenPrimary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    val animatedBorder by animateColorAsState(targetValue = borderColor, label = "border")
    val animatedBg by animateColorAsState(targetValue = backgroundColor, label = "bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .border(1.5.dp, animatedBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) (if (isDestructive) MaterialTheme.colorScheme.error else GreenPrimary) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) (if (isDestructive) MaterialTheme.colorScheme.error else GreenPrimary) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = if (isDestructive) MaterialTheme.colorScheme.error else GreenPrimary
            )
        )
    }
}

@Composable
fun CategoriesTab(categories: List<Category>, adminViewModel: AdminViewModel) {
    var newNameEn by remember { mutableStateOf("") }
    var newNameAm by remember { mutableStateOf("") }
    var newSlug by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add New Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newNameEn, onValueChange = { newNameEn = it }, label = { Text("English") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                        OutlinedTextField(value = newNameAm, onValueChange = { newNameAm = it }, label = { Text("Amharic") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newSlug, onValueChange = { newSlug = it }, label = { Text("Slug (e.g. sports)") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                        Button(onClick = { if (newNameEn.isNotBlank() && newSlug.isNotBlank()) { adminViewModel.addCategory(newNameEn, newNameAm, newSlug); newNameEn = ""; newNameAm = ""; newSlug = "" } }, modifier = Modifier.align(Alignment.CenterVertically).height(56.dp), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Add, contentDescription = "Add") }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Existing Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(categories) { category ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(category.name_en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        if (!category.name_am.isNullOrBlank()) { Text(category.name_am, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    }
                    Switch(checked = category.is_active ?: true, onCheckedChange = { adminViewModel.toggleCategory(category.id, category.is_active ?: true) }, colors = SwitchDefaults.colors(checkedTrackColor = GreenPrimary))
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = message, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}