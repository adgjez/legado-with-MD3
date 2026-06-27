package io.legado.app.help.ai

import io.legado.app.model.ReadBook
import org.json.JSONArray
import org.json.JSONObject

/**
 * 阅读相关的 AI 工具：书籍封面生成、场景插画、角色画像、场景视频。
 * 融合聊天模型（图片理解）-> 图片模型（文生图/图生图）-> 视频模型（图生视频）能力。
 */
object AiReadingTool {

    fun resolvedTools(): List<AiResolvedTool> = listOf(
        // ── 图片生成 ──
        AiResolvedTool(
            name = "generate_book_cover",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_book_cover")
                    put("description", "根据书名、作者、简介生成书籍封面图")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("title", JSONObject(mapOf("type" to "string", "description" to "书名")))
                            put("author", JSONObject(mapOf("type" to "string", "description" to "作者")))
                            put("description", JSONObject(mapOf("type" to "string", "description" to "书籍简介或风格描述")))
                            put("style", JSONObject(mapOf("type" to "string", "description" to "封面风格：fantasy/xianxia/scifi/romance/horror/modern")))
                            put("size", JSONObject(mapOf("type" to "string", "description" to "图片尺寸，如 1024x768, 1024x1024")))
                        })
                        put("required", JSONArray().put("title"))
                    })
                })
            },
            execute = { params -> generateBookCover(params) }
        ),
        AiResolvedTool(
            name = "generate_scene_illustration",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_scene_illustration")
                    put("description", "根据当前阅读场景生成插画。融合聊天模型理解剧情->图片模型生成")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("scene_description", JSONObject(mapOf("type" to "string", "description" to "场景描述")))
                            put("style", JSONObject(mapOf("type" to "string", "description" to "画风：anime/realistic/watercolor/ink")))
                            put("size", JSONObject(mapOf("type" to "string", "description" to "图片尺寸，如 1024x768")))
                        })
                        put("required", JSONArray().put("scene_description"))
                    })
                })
            },
            execute = { params -> generateSceneIllustration(params) }
        ),
        AiResolvedTool(
            name = "generate_character_portrait",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_character_portrait")
                    put("description", "根据角色描述生成角色立绘/头像")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("character_name", JSONObject(mapOf("type" to "string", "description" to "角色名")))
                            put("description", JSONObject(mapOf("type" to "string", "description" to "角色外貌描述")))
                            put("style", JSONObject(mapOf("type" to "string", "description" to "风格")))
                            put("size", JSONObject(mapOf("type" to "string", "description" to "图片尺寸，如 1024x1024")))
                        })
                        put("required", JSONArray().put("character_name").put("description"))
                    })
                })
            },
            execute = { params -> generateCharacterPortrait(params) }
        ),
        // ── 视频生成（阅读场景）──
        AiResolvedTool(
            name = "generate_scene_video",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_scene_video")
                    put("description", "根据阅读场景生成视频。先根据场景描述生成插画，再根据插画生成动态视频。适用于将当前阅读的精彩场景转化为短视频。")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("scene_description", JSONObject(mapOf("type" to "string", "description" to "场景描述")))
                            put("style", JSONObject(mapOf("type" to "string", "description" to "画风：anime/realistic/watercolor/ink")))
                            put("numFrames", JSONObject(mapOf("type" to "integer", "description" to "视频帧数（8n+1，默认121）")))
                            put("aspectRatio", JSONObject(mapOf("type" to "string", "description" to "视频比例：16:9, 9:16, 1:1")))
                        })
                        put("required", JSONArray().put("scene_description"))
                    })
                })
            },
            execute = { params -> generateSceneVideo(params) }
        )
    )

    private suspend fun generateBookCover(args: JSONObject?): String {
        val title = args?.optString("title")?.trim().orEmpty()
        if (title.isBlank()) return errorJson("title is required")
        val author = args?.optString("author")?.trim().orEmpty()
        val description = args?.optString("description")?.trim().orEmpty()
        val style = args?.optString("style")?.trim().orEmpty().ifBlank { "fantasy" }
        val styleHint = when (style) {
            "fantasy" -> "东方玄幻风格，仙气缭绕"
            "xianxia" -> "仙侠风格，御剑飞行，灵气"
            "scifi" -> "科幻风格，未来科技感"
            "romance" -> "唯美言情风格，柔和色调"
            "horror" -> "恐怖悬疑风格，暗色调"
            "modern" -> "现代都市风格，简洁设计"
            else -> "精美插画风格"
        }
        val coverPrompt = buildString {
            append("Book cover design: \"$title\"")
            if (author.isNotBlank()) append(" by $author")
            append(", $styleHint, ")
            append("professional book cover, high quality, ")
            append(description.take(200))
        }
        val size = args?.optString("size")?.takeIf { it.isNotBlank() }
        return generateImage(coverPrompt, size)
    }

    private suspend fun generateSceneIllustration(args: JSONObject?): String {
        val scene = args?.optString("scene_description")?.trim().orEmpty()
        if (scene.isBlank()) return errorJson("scene_description is required")
        val style = args?.optString("style")?.trim().orEmpty().ifBlank { "anime" }
        val styleHint = when (style) {
            "anime" -> "anime style, vibrant colors"
            "realistic" -> "cinematic realism, dramatic lighting"
            "watercolor" -> "watercolor painting style, soft edges"
            "ink" -> "traditional Chinese ink wash painting"
            else -> "illustration style"
        }
        val scenePrompt = "Scene illustration: $scene, $styleHint, high quality, detailed"
        val size = args?.optString("size")?.takeIf { it.isNotBlank() }
        return generateImage(scenePrompt, size)
    }

    private suspend fun generateCharacterPortrait(args: JSONObject?): String {
        val name = args?.optString("character_name")?.trim().orEmpty()
        if (name.isBlank()) return errorJson("character_name is required")
        val desc = args?.optString("description")?.trim().orEmpty()
        if (desc.isBlank()) return errorJson("description is required")
        val style = args?.optString("style")?.trim().orEmpty().ifBlank { "anime" }
        val prompt = "Character portrait of $name: $desc, $style character design, full body, high quality, detailed"
        val size = args?.optString("size")?.takeIf { it.isNotBlank() }
        return generateImage(prompt, size)
    }

    private suspend fun generateImage(prompt: String, size: String? = null): String {
        val provider = AiImageService.currentProviderOrNull()
            ?: return errorJson("No available image provider")
        return runCatching {
            val image = AiImageService.generateAndStore(
                prompt,
                provider,
                size,
                metadata = AiImageGalleryManager.ImageMetadata(
                    sourceType = AiImageGalleryManager.SOURCE_TYPE_CHAT,
                    sourceText = prompt
                )
            )
            JSONObject().apply {
                put("ok", true)
                put("success", true)
                put("type", "image")
                put("imageId", image.id)
                put("imagePath", image.localPath)
                put("name", image.name)
                put("prompt", prompt)
            }.toString()
        }.getOrElse {
            errorJson(it.localizedMessage ?: it.javaClass.simpleName)
        }
    }

    private fun errorJson(message: String): String {
        return JSONObject().apply {
            put("ok", false)
            put("error", message)
        }.toString()
    }

    private suspend fun generateSceneVideo(args: JSONObject?): String {
        val scene = args?.optString("scene_description")?.trim().orEmpty()
        if (scene.isBlank()) return errorJson("scene_description is required")
        val style = args?.optString("style")?.trim().orEmpty().ifBlank { "anime" }
        val styleHint = when (style) {
            "anime" -> "anime style, vibrant colors"
            "realistic" -> "cinematic realism, dramatic lighting"
            "watercolor" -> "watercolor painting style, soft edges"
            "ink" -> "traditional Chinese ink wash painting"
            else -> "illustration style"
        }
        val scenePrompt = "Scene illustration: $scene, $styleHint, high quality, detailed"
        // 1. 先生成场景图片
        val imageJson = generateImage(scenePrompt)
        val imageResult = runCatching { JSONObject(imageJson) }.getOrNull() ?: return imageJson
        if (!imageResult.optBoolean("ok", false)) return imageJson
        val imageId = imageResult.optString("imageId", "")
        if (imageId.isBlank()) return errorJson("Failed to extract imageId from scene image generation")
        // 2. 从图片生成视频
        val videoPrompt = "$scene, cinematic motion, smooth camera movement, $styleHint"
        val extraParams = AiVideoTool.buildExtraParams(args)
        return runCatching {
            val video = AiVideoService.generateAndStore(
                videoPrompt, imageId, null, null, null, extraParams,
                metadata = AiVideoGalleryManager.VideoMetadata(
                    sourceType = AiVideoGalleryManager.SOURCE_TYPE_READ_INSERT,
                    sourceText = videoPrompt
                )
            )
            JSONObject().apply {
                put("ok", true)
                put("success", true)
                put("type", "video")
                put("videoId", video.id)
                put("videoPath", video.localPath)
                put("name", video.name)
                put("prompt", videoPrompt)
                put("sourceType", "read_insert")
            }.toString()
        }.getOrElse {
            errorJson(it.localizedMessage ?: it.javaClass.simpleName)
        }
    }
}