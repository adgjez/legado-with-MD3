package io.legado.app.help.ai

import org.json.JSONArray
import org.json.JSONObject

object AiImageTool {
    fun resolvedTools(): List<AiResolvedTool> = listOf(
        AiResolvedTool(
            name = "generate_image",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_image")
                    put("description", "Generate an image and return an image url or data url.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("prompt", JSONObject().apply {
                                put("type", "string")
                                put("description", "Image prompt")
                            })
                            put("size", JSONObject().apply {
                                put("type", "string")
                                put("description", "Image size, e.g. 1024x1024, 1792x1024, 1024x1792. Default is 1024x1024.")
                            })
                            put("providerId", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional image provider id. Use only when user explicitly selects an image model; otherwise omit it.")
                            })
                        })
                        put("required", JSONArray().put("prompt"))
                    })
                })
            },
            execute = { args ->
                val prompt = args?.optString("prompt").orEmpty().trim()
                if (prompt.isBlank()) {
                    JSONObject().put("ok", false).put("success", false).put("error", "prompt is empty").toString()
                } else {
                    val providerId = args?.optString("providerId").orEmpty().trim()
                    val size = args?.optString("size")?.takeIf { it.isNotBlank() }
                    val provider = if (providerId.isBlank()) {
                        null
                    } else {
                        AiImageService.providerByIdOrNull(providerId)
                    }
                    if (providerId.isNotBlank() && provider == null) {
                        JSONObject()
                            .put("ok", false)
                            .put("success", false)
                            .put("error", "image provider is unavailable: $providerId")
                            .toString()
                    } else {
                        val targetProvider = provider ?: AiImageService.currentProviderOrNull()
                        runCatching {
                            val image = AiImageService.generateAndStore(
                                prompt,
                                provider,
                                size,
                                metadata = AiImageGalleryManager.ImageMetadata(
                                    sourceType = AiImageGalleryManager.SOURCE_TYPE_CHAT,
                                    sourceText = prompt
                                )
                            )
                            JSONObject()
                                .put("ok", true)
                                .put("success", true)
                                .put("type", "image")
                                .put("imageId", image.id)
                                .put("imagePath", image.localPath)
                                .put("name", image.name)
                                .put("prompt", prompt)
                                .put("provider", image.providerName)
                                .put("model", image.model)
                                .toString()
                        }.getOrElse {
                            JSONObject()
                                .put("ok", false)
                                .put("success", false)
                                .put("error", it.localizedMessage ?: it.javaClass.simpleName)
                                .apply {
                                    targetProvider?.let { current ->
                                        put("provider", current.displayName())
                                        put("providerType", current.type)
                                        put("baseUrl", current.baseUrl)
                                        put("model", current.model)
                                    }
                                }
                                .toString()
                        }
                    }
                }
            }
        ),
        // ── 图生图 ──
        AiResolvedTool(
            name = "generate_image_from_image",
            definition = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "generate_image_from_image")
                    put("description", "Generate a new image from an existing image and an optional prompt. The input image is used as a visual reference. Use this when the user wants to create a variation or edit of an image they sent or referenced.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("prompt", JSONObject().apply {
                                put("type", "string")
                                put("description", "Image prompt describing the desired output.")
                            })
                            put("inputImageId", JSONObject().apply {
                                put("type", "string")
                                put("description", "AI image gallery image id to use as the base/reference image.")
                            })
                            put("size", JSONObject().apply {
                                put("type", "string")
                                put("description", "Image size, e.g. 1024x1024, 1792x1024, 1024x1792. Default is 1024x1024.")
                            })
                            put("providerId", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional image provider id. Use only when user explicitly selects an image model; otherwise omit it.")
                            })
                        })
                        put("required", JSONArray().put("prompt").put("inputImageId"))
                    })
                })
            },
            execute = { args ->
                val prompt = args?.optString("prompt").orEmpty().trim()
                val inputImageId = args?.optString("inputImageId").orEmpty().trim()
                if (prompt.isBlank()) {
                    JSONObject().put("ok", false).put("success", false).put("error", "prompt is empty").toString()
                } else if (inputImageId.isBlank()) {
                    JSONObject().put("ok", false).put("success", false).put("error", "inputImageId is empty").toString()
                } else {
                    val providerId = args?.optString("providerId").orEmpty().trim()
                    val size = args?.optString("size")?.takeIf { it.isNotBlank() }
                    val provider = if (providerId.isBlank()) null else AiImageService.providerByIdOrNull(providerId)
                    val targetProvider = provider ?: AiImageService.currentProviderOrNull()
                    if (providerId.isNotBlank() && provider == null) {
                        JSONObject()
                            .put("ok", false)
                            .put("success", false)
                            .put("error", "image provider is unavailable: $providerId")
                            .toString()
                    } else {
                        runCatching {
                            val image = AiImageService.generateFromImage(
                                prompt,
                                inputImageId,
                                null,
                                provider,
                                size,
                                metadata = AiImageGalleryManager.ImageMetadata(
                                    sourceType = AiImageGalleryManager.SOURCE_TYPE_CHAT,
                                    sourceText = prompt
                                )
                            )
                            JSONObject()
                                .put("ok", true)
                                .put("success", true)
                                .put("type", "image")
                                .put("imageId", image.id)
                                .put("imagePath", image.localPath)
                                .put("name", image.name)
                                .put("prompt", prompt)
                                .put("provider", image.providerName)
                                .put("model", image.model)
                                .toString()
                        }.getOrElse {
                            JSONObject()
                                .put("ok", false)
                                .put("success", false)
                                .put("error", it.localizedMessage ?: it.javaClass.simpleName)
                                .apply {
                                    targetProvider?.let { current ->
                                        put("provider", current.displayName())
                                        put("providerType", current.type)
                                        put("baseUrl", current.baseUrl)
                                        put("model", current.model)
                                    }
                                }
                                .toString()
                        }
                    }
                }
            }
        )
    )
}
