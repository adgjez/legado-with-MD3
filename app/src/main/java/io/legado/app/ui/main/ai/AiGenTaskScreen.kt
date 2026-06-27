package io.legado.app.ui.main.ai

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.data.appDb
import io.legado.app.data.entities.AiGenTask
import io.legado.app.help.ai.AiAssetManager
import io.legado.app.help.ai.AiGenTaskManager
import io.legado.app.ui.widget.compose.LegadoMiuixActionButton
import io.legado.app.ui.widget.compose.LegadoMiuixCard
import io.legado.app.ui.widget.compose.LegadoMiuixPalette
import io.legado.app.ui.widget.compose.rememberAppDialogStyle
import io.legado.app.ui.widget.compose.toMiuixPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiGenTaskScreen(onBack: () -> Unit) {
    val palette = rememberAppDialogStyle().toMiuixPalette()
    var tasks by remember { mutableStateOf<List<AiGenTask>>(emptyList()) }
    var stats by remember { mutableStateOf(StatsSnapshot()) }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    fun load() {
        scope.launch {
            tasks = withContext(Dispatchers.IO) { appDb.aiGenTaskDao.recent() }
            stats = withContext(Dispatchers.IO) {
                val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                StatsSnapshot(
                    totalCost = appDb.aiGenVoucherDao.totalCostSince(weekAgo),
                    imageCost = appDb.aiGenVoucherDao.costByModalitySince("image", weekAgo),
                    videoCost = appDb.aiGenVoucherDao.costByModalitySince("video", weekAgo),
                    storage = AiAssetManager.storageUsage()
                )
            }
        }
    }

    remember { load(); Unit }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = palette.primaryText)
                }
                Text("生成任务", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.primaryText)
            }
            IconButton(onClick = { load() }) {
                Icon(Icons.Default.Refresh, "刷新", tint = palette.secondaryText)
            }
        }

        // Stats summary
        StatsSummaryCard(stats, palette)

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无生成任务", color = palette.secondaryText, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(task, palette, dateFormat, onUpdate = { load() })
                }
            }
        }
    }
}

private data class StatsSnapshot(
    val totalCost: Double = 0.0,
    val imageCost: Double = 0.0,
    val videoCost: Double = 0.0,
    val storage: AiAssetManager.StorageUsage? = null
)

@Composable
private fun StatsSummaryCard(stats: StatsSnapshot, palette: LegadoMiuixPalette) {
    LegadoMiuixCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = palette.surface,
        contentColor = palette.primaryText,
        cornerRadius = 12.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("近 7 日概况", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = palette.secondaryText)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("总费用", "$${String.format("%.3f", stats.totalCost)}", palette)
                StatItem("图片", "$${String.format("%.3f", stats.imageCost)}", palette)
                StatItem("视频", "$${String.format("%.3f", stats.videoCost)}", palette)
                stats.storage?.let { s ->
                    StatItem("存储", formatBytes(s.totalBytes), palette)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, palette: LegadoMiuixPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.accent)
        Text(label, fontSize = 10.sp, color = palette.secondaryText)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format("%.1fG", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> String.format("%.1fM", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format("%.1fK", bytes / 1024.0)
    else -> "${bytes}B"
}

@Composable
private fun TaskCard(
    task: AiGenTask,
    palette: LegadoMiuixPalette,
    dateFormat: SimpleDateFormat,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val statusColor = taskStatusColor(task.status, palette)
    val isActive = task.status in setOf("pending", "submitted", "processing", "downloading")

    LegadoMiuixCard(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surface,
        contentColor = palette.primaryText,
        cornerRadius = 12.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.modalityLabel(),
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = dateFormat.format(Date(task.createdAt)),
                    fontSize = 11.sp,
                    color = palette.secondaryText
                )
            }

            Spacer(Modifier.height(6.dp))

            // Prompt
            Text(
                text = task.prompt.ifBlank { "(无提示词)" },
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = palette.primaryText
            )

            // Model info
            if (task.model.isNotBlank()) {
                Text(
                    text = "${task.providerName.ifBlank { "—" }} / ${task.model}",
                    fontSize = 11.sp,
                    color = palette.secondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Progress bar for active tasks
            if (isActive && task.progress > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = palette.surfaceVariant
                )
            }

            // Error message
            if (task.errorMessage.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.errorMessage,
                    fontSize = 11.sp,
                    color = errorColor(palette),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.status == "failed" || task.status == "cancelled") {
                    LegadoMiuixActionButton(
                        text = "重试",
                        palette = palette,
                        onClick = {
                            scope.launch {
                                AiGenTaskManager.retryTask(task.id)
                                onUpdate()
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (isActive) {
                    LegadoMiuixActionButton(
                        text = "取消",
                        palette = palette,
                        danger = true,
                        onClick = {
                            scope.launch {
                                AiGenTaskManager.cancelTask(task.id)
                                onUpdate()
                            }
                        }
                    )
                }
                if (task.status == "done" || task.status == "failed" || task.status == "cancelled") {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { appDb.aiGenTaskDao.delete(task.id) }
                                onUpdate()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, "删除", tint = palette.secondaryText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun taskStatusColor(status: String, palette: LegadoMiuixPalette) = when (status) {
    "pending" -> palette.accent.copy(alpha = 0.8f)
    "submitted", "processing", "downloading" -> palette.accent
    "done" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    "failed" -> errorColor(palette)
    "cancelled" -> palette.secondaryText
    else -> palette.secondaryText
}

@Composable
private fun errorColor(palette: LegadoMiuixPalette) = androidx.compose.ui.graphics.Color(0xFFE53935)

private fun AiGenTask.modalityLabel() = when (modality) {
    "video" -> "视频"
    "image" -> "图片"
    "text_sanitize" -> "文本净化"
    else -> modality
}