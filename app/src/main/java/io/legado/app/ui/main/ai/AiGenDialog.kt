package io.legado.app.ui.main.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.legado.app.ui.widget.compose.AppDialogStyle
import io.legado.app.ui.widget.compose.LegadoMiuixActionButton
import io.legado.app.ui.widget.compose.rememberAppDialogStyle
import io.legado.app.ui.widget.compose.toMiuixPalette

// ── 预设模板 ──

data class GenPreset(
    val name: String,
    val icon: String,
    val duration: Long,
    val aspectRatio: String,
    val description: String
)

data class ImageGenPreset(
    val name: String,
    val icon: String,
    val size: String,
    val aspectRatio: String,
    val description: String
)

// ── 主入口 ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiGenDialog(
    onDismiss: () -> Unit,
    onGenerateVideo: (prompt: String, negativePrompt: String, duration: Long, aspectRatio: String, providerId: String?, inputImageId: String?, tailImageId: String?) -> Unit,
    onGenerateImage: (prompt: String, negativePrompt: String, size: String, providerId: String?, referenceImageId: String?) -> Unit,
    initialTab: Int = 0,
    initialInputImageId: String? = null,
    initialTailImageId: String? = null
) {
    val style = rememberAppDialogStyle()
    val palette = style.toMiuixPalette()
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("AI 生图", "AI 生视频")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(style.panelRadius),
            color = style.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 标题
                Text(
                    text = "AI 创作中心",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = style.primaryText,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 标签页
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = palette.accent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = palette.accent
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) palette.accent else style.secondaryText
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ImageGenTab(
                        style = style,
                        onGenerateImage = onGenerateImage,
                        onDismiss = onDismiss,
                        initialInputImageId = initialInputImageId
                    )
                    1 -> VideoGenTab(
                        style = style,
                        onGenerateVideo = onGenerateVideo,
                        onDismiss = onDismiss,
                        initialInputImageId = initialInputImageId,
                        initialTailImageId = initialTailImageId
                    )
                }
            }
        }
    }
}

// ── 图片生成页 ──

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ImageGenTab(
    style: AppDialogStyle,
    onGenerateImage: (prompt: String, negativePrompt: String, size: String, providerId: String?, referenceImageId: String?) -> Unit,
    onDismiss: () -> Unit,
    initialInputImageId: String? = null
) {
    val palette = style.toMiuixPalette()
    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("1024x1024") }
    var customSize by remember { mutableStateOf("") }
    var stylePrompt by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(-1) }
    var showAdvanced by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("text_to_image") }
    var referenceImageId by remember { mutableStateOf(initialInputImageId) }

    val presets = remember {
        listOf(
            ImageGenPreset("高清写真", "📷", "1024x1024", "1:1", "人像/产品"),
            ImageGenPreset("横版海报", "🖼️", "1280x720", "16:9", "封面/横幅"),
            ImageGenPreset("竖版壁纸", "📱", "720x1280", "9:16", "手机壁纸"),
            ImageGenPreset("电商主图", "🛍️", "1024x1024", "1:1", "白底产品"),
            ImageGenPreset("自定义", "✏️", "1024x1024", "自定义", "手动输入")
        )
    }

    val styleTags = remember {
        listOf(
            "写实摄影" to "photorealistic, 8K, detailed",
            "二次元" to "anime, manga style, vibrant",
            "油画风" to "oil painting, textured, artistic",
            "赛博朋克" to "cyberpunk, neon, futuristic",
            "水墨画" to "ink wash painting, traditional Chinese",
            "像素风" to "pixel art, retro, 8-bit"
        )
    }

    val effectiveSize = customSize.ifBlank { size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("text_to_image" to "文生图", "image_to_image" to "图生图").forEach { (m, label) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (mode == m) palette.accent else style.fieldSurface,
                    contentColor = if (mode == m) Color.White else style.primaryText,
                    modifier = Modifier.clickable { mode = m }
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (mode == m) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 图生图：参考图提示
        if (mode == "image_to_image") {
            if (referenceImageId != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.accent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖼️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "已关联参考图，生成将基于此图风格",
                            fontSize = 12.sp,
                            color = palette.accent
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = style.fieldSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* will be handled by caller to pick image */ }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖼️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "点击选择参考图（可选）",
                            fontSize = 12.sp,
                            color = style.secondaryText
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 预设
        Text("一键模板", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEachIndexed { index, preset ->
                val isCustom = preset.name == "自定义"
                PresetChip(
                    name = preset.name,
                    icon = preset.icon,
                    subtitle = if (isCustom && customSize.isNotBlank()) customSize else preset.description,
                    selected = selectedPreset == index,
                    onClick = {
                        selectedPreset = index
                        if (isCustom) {
                            // keep custom size, don't override
                        } else {
                            size = preset.size
                            customSize = ""
                        }
                    },
                    style = style,
                    modifier = Modifier.width(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prompt
        Text(
            if (mode == "image_to_image") "描述你想要的图片（基于参考图）" else "描述你想要生成的图片",
            fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("例：一只可爱的橘猫坐在窗台上，阳光透过窗户", color = style.secondaryText.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = style.accent,
                unfocusedBorderColor = style.stroke,
                focusedTextColor = style.primaryText,
                unfocusedTextColor = style.primaryText,
                focusedContainerColor = style.fieldSurface,
                unfocusedContainerColor = style.fieldSurface,
                cursorColor = style.accent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 风格标签 - FlowRow 自动换行
        Text("风格", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            styleTags.forEach { (name, tag) ->
                val isActive = stylePrompt.contains(tag)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) palette.accent else style.fieldSurface,
                    modifier = Modifier.clickable {
                        stylePrompt = if (isActive) stylePrompt.replace(tag, "").trim().replace("  ", " ")
                        else "$stylePrompt $tag".trim()
                    }
                ) {
                    Text(
                        name,
                        fontSize = 11.sp,
                        color = if (isActive) Color.White else style.primaryText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 输出尺寸 - 始终可见
        Spacer(modifier = Modifier.height(12.dp))
        Text("输出尺寸", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("1024x1024", "1280x720", "720x1280", "1152x864").forEach { s ->
                val isActive = effectiveSize == s
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) palette.accent else style.fieldSurface,
                    contentColor = if (isActive) Color.White else style.primaryText,
                    modifier = Modifier.clickable {
                        size = s
                        customSize = ""
                    }
                ) {
                    Text(
                        s.replace("x", " × "),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 自定义分辨率输入
        OutlinedTextField(
            value = customSize,
            onValueChange = {
                customSize = it
                if (it.isNotBlank()) selectedPreset = presets.indexOfFirst { p -> p.name == "自定义" }
            },
            placeholder = { Text("自定义分辨率，如 800x600", fontSize = 12.sp, color = style.secondaryText.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = style.accent,
                unfocusedBorderColor = style.stroke,
                focusedTextColor = style.primaryText,
                unfocusedTextColor = style.primaryText,
                focusedContainerColor = style.fieldSurface,
                unfocusedContainerColor = style.fieldSurface,
                cursorColor = style.accent
            )
        )

        // 高级参数
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("高级参数", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
            Text(if (showAdvanced) "▲" else "▼", fontSize = 12.sp, color = style.secondaryText)
        }

        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // 负向提示词
                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { negativePrompt = it },
                    label = { Text("不想要的内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = style.accent,
                        unfocusedBorderColor = style.stroke,
                        focusedTextColor = style.primaryText,
                        unfocusedTextColor = style.primaryText,
                        focusedContainerColor = style.fieldSurface,
                        unfocusedContainerColor = style.fieldSurface,
                        cursorColor = style.accent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮
        val generateEnabled = prompt.isNotBlank() && !generating &&
            (mode == "text_to_image" || referenceImageId != null || prompt.isNotBlank())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegadoMiuixActionButton(
                text = "取消",
                palette = palette,
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp)
            )
            LegadoMiuixActionButton(
                text = if (generating) "生成中..." else "生成图片",
                palette = palette,
                onClick = {
                    if (generateEnabled) {
                        generating = true
                        val fullPrompt = if (stylePrompt.isNotBlank()) "$prompt, $stylePrompt" else prompt
                        onGenerateImage(fullPrompt, negativePrompt, effectiveSize, null, referenceImageId)
                    }
                },
                primary = true
            )
        }
    }
}

// ── 视频生成页 ──

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun VideoGenTab(
    style: AppDialogStyle,
    onGenerateVideo: (prompt: String, negativePrompt: String, duration: Long, aspectRatio: String, providerId: String?, inputImageId: String?, tailImageId: String?) -> Unit,
    onDismiss: () -> Unit,
    initialInputImageId: String? = null,
    initialTailImageId: String? = null
) {
    val palette = style.toMiuixPalette()
    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(5_000L) }
    var aspectRatio by remember { mutableStateOf("16:9") }
    var showAdvanced by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(-1) }
    var generating by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(
        when {
            initialTailImageId != null -> "keyframes"
            initialInputImageId != null -> "image_to_video"
            else -> "text_to_video"
        }
    ) }
    var inputImageId by remember { mutableStateOf(initialInputImageId) }
    var tailImageId by remember { mutableStateOf(initialTailImageId) }

    val presets = remember {
        listOf(
            GenPreset("快速短片", "🎬", 3_000L, "16:9", "3秒 16:9"),
            GenPreset("循环壁纸", "🎨", 5_000L, "9:16", "5秒 9:16"),
            GenPreset("分镜故事", "📖", 5_000L, "16:9", "5秒 16:9"),
            GenPreset("动态海报", "🖼️", 3_000L, "1:1", "3秒 1:1"),
            GenPreset("长视频", "🎥", 18_000L, "16:9", "18秒 16:9")
        )
    }

    val motionTags = remember {
        listOf(
            "缓慢推镜" to "slow push in, cinematic",
            "环绕运镜" to "orbit around subject, smooth",
            "垂直升降" to "crane up, reveal",
            "手持跟拍" to "handheld tracking, dynamic",
            "固定镜头" to "static shot, tripod",
            "微距特写" to "macro close-up, shallow depth of field"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "text_to_video" to "文生视频",
                "image_to_video" to "图生视频",
                "keyframes" to "关键帧"
            ).forEach { (m, label) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (mode == m) palette.accent else style.fieldSurface,
                    contentColor = if (mode == m) Color.White else style.primaryText,
                    modifier = Modifier.clickable { mode = m }
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (mode == m) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 模式提示
        when (mode) {
            "keyframes" -> {
                if (tailImageId != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.accent.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎞️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("关键帧动画模式：首尾帧驱动", fontSize = 12.sp, color = palette.accent, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            "image_to_video" -> {
                if (inputImageId != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.accent.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖼️→🎬", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("图生视频模式：基于图片生成视频", fontSize = 12.sp, color = palette.accent, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = style.fieldSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* pick image from gallery */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖼️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("点击选择起始图片（可选）", fontSize = 12.sp, color = style.secondaryText)
                        }
                    }
                }
            }
        }
        if (mode != "text_to_video") {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 预设
        Text("一键模板", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEachIndexed { index, preset ->
                PresetChip(
                    name = preset.name,
                    icon = preset.icon,
                    subtitle = preset.description,
                    selected = selectedPreset == index,
                    onClick = {
                        selectedPreset = index
                        duration = preset.duration
                        aspectRatio = preset.aspectRatio
                    },
                    style = style,
                    modifier = Modifier.width(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prompt
        Text("描述你想要的视频", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = {
                Text(
                    when (mode) {
                        "keyframes" -> "描述两个关键帧之间的过渡动画..."
                        "image_to_video" -> "描述图片中应该发生的动态变化..."
                        else -> "例：一群肥羊在绿草地上悠闲地吃草"
                    },
                    color = style.secondaryText.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = style.accent,
                unfocusedBorderColor = style.stroke,
                focusedTextColor = style.primaryText,
                unfocusedTextColor = style.primaryText,
                focusedContainerColor = style.fieldSurface,
                unfocusedContainerColor = style.fieldSurface,
                cursorColor = style.accent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 运镜风格 - FlowRow 自动换行
        Text("运镜风格", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            motionTags.forEach { (name, tag) ->
                val isActive = prompt.contains(tag)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) palette.accent else style.fieldSurface,
                    modifier = Modifier.clickable {
                        prompt = if (isActive) prompt.replace(tag, "").trim().replace("  ", " ")
                        else "$prompt, $tag".trim()
                    }
                ) {
                    Text(
                        name,
                        fontSize = 11.sp,
                        color = if (isActive) Color.White else style.primaryText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 时长 - 始终可见
        Spacer(modifier = Modifier.height(12.dp))
        Text("时长: ${duration / 1000}秒", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Slider(
            value = duration.toFloat() / 1000f,
            onValueChange = { duration = (it * 1000).toLong() },
            valueRange = 3f..18f,
            steps = 14,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent
            )
        )

        // 画面比例 - 始终可见
        Spacer(modifier = Modifier.height(4.dp))
        Text("画面比例", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("16:9", "9:16", "1:1", "4:3", "3:4").forEach { ratio ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (aspectRatio == ratio) palette.accent else style.fieldSurface,
                    contentColor = if (aspectRatio == ratio) Color.White else style.primaryText,
                    modifier = Modifier.clickable { aspectRatio = ratio }
                ) {
                    Text(
                        ratio,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 高级参数
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("高级参数", fontSize = 13.sp, color = style.secondaryText, fontWeight = FontWeight.Medium)
            Text(if (showAdvanced) "▲" else "▼", fontSize = 12.sp, color = style.secondaryText)
        }

        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // 负向提示词
                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { negativePrompt = it },
                    label = { Text("不想要的内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = style.accent,
                        unfocusedBorderColor = style.stroke,
                        focusedTextColor = style.primaryText,
                        unfocusedTextColor = style.primaryText,
                        focusedContainerColor = style.fieldSurface,
                        unfocusedContainerColor = style.fieldSurface,
                        cursorColor = style.accent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮
        val generateEnabled = prompt.isNotBlank() && !generating
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegadoMiuixActionButton(
                text = "取消",
                palette = palette,
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp)
            )
            LegadoMiuixActionButton(
                text = if (generating) "生成中..." else "生成视频",
                palette = palette,
                onClick = {
                    if (generateEnabled) {
                        generating = true
                        onGenerateVideo(prompt, negativePrompt, duration, aspectRatio, null, inputImageId, tailImageId)
                    }
                },
                primary = true
            )
        }
    }
}

// ── 共享组件 ──

@Composable
private fun PresetChip(
    name: String,
    icon: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    style: AppDialogStyle,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) style.accent.copy(alpha = 0.15f) else style.fieldSurface,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, style.accent)
        else androidx.compose.foundation.BorderStroke(1.dp, style.stroke),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp)
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = style.primaryText)
            Text(subtitle, fontSize = 10.sp, color = style.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}