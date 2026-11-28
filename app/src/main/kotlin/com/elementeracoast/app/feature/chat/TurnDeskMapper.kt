package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.TurnDeskDetail
import com.elementeracoast.app.core.model.TurnDeskReceipt
import com.elementeracoast.app.core.model.TurnDeskSection
import com.elementeracoast.app.core.remote.RemoteDeskCrossWindow
import com.elementeracoast.app.core.remote.RemoteDeskAttachments
import com.elementeracoast.app.core.remote.RemoteDeskCrossWindowSource
import com.elementeracoast.app.core.remote.RemoteDeskSlip
import com.elementeracoast.app.core.remote.RemoteWakeDeskReceipt
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import com.elementeracoast.app.core.remote.RemoteDeskWebSearch
import com.elementeracoast.app.core.remote.RemoteDeskTool

internal object TurnDeskMapper {
    private fun wakeToUi(value: RemoteDeskSlip, wake: RemoteWakeDeskReceipt): TurnDeskReceipt {
        val context=wake.context
        val stage=when((context["stage"] as? JsonPrimitive)?.contentOrNull){
            "provider_response_received"->"已收到首轮模型响应"
            "prepared_not_confirmed_delivery"->"已准备，递送未确认"
            else->"无上下文确认记录"
        }
        val labels=mapOf("custom_instructions" to "核心自定义","user_portrait" to "用户画像","global_excerpt" to "自我画像","soil" to "思维壤","memory" to "记忆","worldbook" to "世界书","dogtalk" to "私人草稿","cross_window" to "跨窗口","workbench" to "工作台","reality" to "现实上下文","response_mode" to "回应节奏")
        val details=buildList{
            for((key,label) in listOf("estimated_tokens" to "估算 token","comfort_ceiling" to "舒服区间","trimmed_count" to "裁剪处数","omitted_turns" to "省略历史轮次")){
                (context[key] as? JsonPrimitive)?.longOrNull?.let{add(TurnDeskDetail(label,it.toString()))}
            }
            (context["over_budget"] as? JsonPrimitive)?.booleanOrNull?.let{add(TurnDeskDetail("超出舒服区间",if(it)"是" else "否"))}
            val kept=context["kept"] as? JsonObject
            for((key,label) in labels){
                val item=kept?.get(key) as? JsonPrimitive?:continue
                val text=item.booleanOrNull?.let{if(it)"保留" else "未保留"}?:item.longOrNull?.let{"$it 条"}
                if(text!=null)add(TurnDeskDetail(label,text))
            }
        }
        return TurnDeskReceipt(summary=value.summary,comfort=value.comfort,sections=listOf(
            section("自主活动","正式消息已保存","没有伪造用户发言。",listOf(TurnDeskDetail("活动 ID",wake.activityId),TurnDeskDetail("发送请求序号",(wake.requestIndex+1L).toString()))),
            section("首轮上下文",stage,"仅为首轮组包统计，不是逐项阅读或全文递送证明。",details),
            section("本段工具回执","实际记录 ${wake.toolCount} 条","仅列发送前、上一条自主消息之后的工具事件；最多展示128条。",emptyList())
        )+wake.tools.map{tool->
            val status=when(tool.status){"completed"->"执行成功";"failed"->"执行失败";else->"执行结果未确认"}
            section(tool.toolName,status,"执行状态不等于工具结果已递送模型。",listOf(TurnDeskDetail("请求序号",(tool.stepIndex+1L).toString()),TurnDeskDetail("工具序号",(tool.callIndex+1L).toString()))+detail("记录 ID",tool.recordId.orEmpty()))
        })
    }

    fun toUi(value: RemoteDeskSlip): TurnDeskReceipt = value.wakeReceipt?.let { wakeToUi(value,it) } ?: TurnDeskReceipt(
        summary = value.summary.ifBlank { "本轮递给模型" },
        comfort = value.comfort,
        sections = listOf(
            section(
                title = value.currentMessage.label.ifBlank { "当前消息" },
                status = value.currentMessage.status,
                description = value.currentMessage.description,
                details = detail("正文", value.currentMessage.content)
            ),
            section(
                title = value.recentContext.label.ifBlank { "最近上下文" },
                status = withDetail(value.recentContext.status, value.recentContext.statusDetail),
                description = value.recentContext.description,
                details = value.recentContext.messages.map { message ->
                    TurnDeskDetail(if (message.role == "assistant") "Myri 回复" else "用户消息", message.content)
                }
            ),
            section(
                title = value.customInstructions.label.ifBlank { "核心自定义" },
                status = if (value.customInstructions.delivered && value.customInstructions.length > 0) {
                    "${value.customInstructions.status} · 全文 ${value.customInstructions.length} 字"
                } else value.customInstructions.status,
                description = value.customInstructions.description,
                details = detail("全文", value.customInstructions.content)
            ),
            section(
                title = value.userPortrait.label.ifBlank { "用户画像" },
                status = withDetail(
                    value.userPortrait.status,
                    if (value.userPortrait.estimatedTokens > 0) "约 ${value.userPortrait.estimatedTokens} tokens" else ""
                ),
                description = value.userPortrait.description,
                details = buildList {
                    if (value.userPortrait.injection.isNotBlank()) add(TurnDeskDetail("注入状态", value.userPortrait.injection))
                    if (value.userPortrait.length > 0) add(TurnDeskDetail("正文长度", "${value.userPortrait.length} 字"))
                    if (value.userPortrait.content.isNotBlank()) add(TurnDeskDetail("实际递给模型", value.userPortrait.content))
                }
            ),
            section(
                title = value.globalExcerpt.label.ifBlank { "自我画像" },
                status = withDetail(
                    value.globalExcerpt.status,
                    if (value.globalExcerpt.estimatedTokens > 0) "约 ${value.globalExcerpt.estimatedTokens} tokens" else ""
                ),
                description = value.globalExcerpt.description,
                details = buildList {
                    if (value.globalExcerpt.injection.isNotBlank()) add(TurnDeskDetail("注入状态", value.globalExcerpt.injection))
                    if (value.globalExcerpt.length > 0) add(TurnDeskDetail("正文长度", "${value.globalExcerpt.length} 字"))
                    if (value.globalExcerpt.content.isNotBlank()) add(TurnDeskDetail("实际递给模型", value.globalExcerpt.content))
                }
            ),
            section(
                title = value.thinkingSoil.label.ifBlank { "思维壤" },
                status = value.thinkingSoil.status,
                description = value.thinkingSoil.description,
                details = buildList {
                    if (value.thinkingSoil.currentText.isNotBlank()) add(TurnDeskDetail("当前整理", value.thinkingSoil.currentText))
                    value.thinkingSoil.afterthoughts.forEach { item -> add(TurnDeskDetail("余思", item)) }
                    add(TurnDeskDetail("记忆候选", "${value.thinkingSoil.memoryCandidatesCount} 条 · 未递给正文"))
                    add(TurnDeskDetail("种子候选", "${value.thinkingSoil.seedCandidatesCount} 条 · 未递给正文"))
                    value.thinkingSoil.userPortraitCandidate?.let {
                        add(TurnDeskDetail("用户画像候选", listOf(it.reason, it.sourceExcerpt).filter(String::isNotBlank).joinToString("\n").ifBlank { "1 条 · 未递给正文" }))
                    }
                    value.thinkingSoil.selfPortraitCandidate?.let {
                        add(TurnDeskDetail("自我画像候选", listOf(it.reason, it.sourceExcerpt).filter(String::isNotBlank).joinToString("\n").ifBlank { "1 条 · 未递给正文" }))
                    }
                    if (value.thinkingSoil.context.isNotBlank()) add(TurnDeskDetail("实际递给模型", value.thinkingSoil.context))
                }
            ),
            section(
                title = value.relatedMemory.label.ifBlank { "相关记忆" },
                status = withDetail(
                    value.relatedMemory.status,
                    if (value.relatedMemory.confirmationStatus.isBlank()) "" else "${value.relatedMemory.confirmationStatus} ${value.relatedMemory.count} 条"
                ),
                description = value.relatedMemory.description,
                details = value.relatedMemory.items.flatMap { item ->
                    buildList {
                        val title = item.title.ifBlank { "未命名" }
                        add(TurnDeskDetail(title, listOf(item.entryType, item.tag, item.sourceWindow).filter(String::isNotBlank).joinToString(" · ")))
                        if (item.sourceModel.isNotBlank()) add(TurnDeskDetail("来源模型", item.sourceModel))
                        if (item.sourceTime.isNotBlank() || item.sourceDate.isNotBlank()) add(TurnDeskDetail("来源时间", item.sourceTime.ifBlank { item.sourceDate }))
                        if (item.reason.isNotBlank()) add(TurnDeskDetail("命中原因", item.reason))
                        if (item.lifeCore.isNotBlank()) add(TurnDeskDetail("核心", item.lifeCore))
                        if (item.usageHint.isNotBlank()) add(TurnDeskDetail("使用时机", item.usageHint))
                        if (item.avoidHint.isNotBlank()) add(TurnDeskDetail("勿误用", item.avoidHint))
                        if (item.content.isNotBlank()) add(TurnDeskDetail("正文", item.content))
                        if (item.deliveredText.isNotBlank()) add(TurnDeskDetail("实际递给模型", item.deliveredText))
                    }
                }
            ),
            section(
                title = value.worldbook.label.ifBlank { "世界书" },
                status = withDetail(value.worldbook.status, if (value.worldbook.matchedCount > 0) "命中 ${value.worldbook.matchedCount} 条" else ""),
                description = value.worldbook.description,
                details = value.worldbook.entries.flatMap { item ->
                    buildList {
                        add(TurnDeskDetail(item.title.ifBlank { "未命名" }, item.content))
                        if (item.scope.isNotBlank()) add(TurnDeskDetail("范围", item.scope))
                        if (item.matchedBy.isNotBlank()) add(TurnDeskDetail("命中方式", item.matchedBy))
                        if (item.deliveredText.isNotBlank()) add(TurnDeskDetail("实际递给模型", item.deliveredText))
                    }
                }
            ),
            section(
                title = value.dogtalk.label.ifBlank { "狗话" },
                status = value.dogtalk.status,
                description = value.dogtalk.description,
                details = if (value.dogtalk.delivered) detail("实际递给模型", value.dogtalk.context) else emptyList()
            ),
            section(
                title = value.realityContext.label.ifBlank { "现实注入" },
                status = value.realityContext.status,
                description = value.realityContext.description,
                details = buildList {
                    value.realityContext.deliveredItems.forEach { item ->
                        add(TurnDeskDetail(item.label.ifBlank { item.key }, item.text))
                    }
                    if (value.realityContext.missing.isNotEmpty()) {
                        add(TurnDeskDetail("已开启但本轮无数据", value.realityContext.missing.joinToString("、")))
                    }
                    if (value.realityContext.context.isNotBlank()) {
                        add(TurnDeskDetail("实际递给模型", value.realityContext.context))
                    }
                }
            ),
            section(
                title = value.responseMode.label.ifBlank { "回应节奏" },
                status = value.responseMode.status,
                description = value.responseMode.description,
                details = detail("实际递给模型", value.responseMode.context)
            ),
            section(
                title = value.crossWindow.label.ifBlank { "跨窗口取信" },
                status = crossWindowStatus(value.crossWindow),
                description = value.crossWindow.description,
                details = buildList {
                    val requestedTurns = value.crossWindow.requestedTurns.takeIf { it > 0 } ?: value.crossWindow.totalRequestedTurns
                    val loadedTurns = value.crossWindow.loadedTurns.takeIf { it > 0 } ?: value.crossWindow.totalLoadedTurns
                    val deliveredTurns = value.crossWindow.deliveredToModelTurns.takeIf { it > 0 } ?: value.crossWindow.totalDeliveredTurns
                    value.crossWindow.sources.forEach { source ->
                        val sourceLoaded = source.loadedTurns.takeIf { it > 0 } ?: source.deliveredTurns
                        val sourceDelivered = source.deliveredToModelTurns.takeIf { it > 0 } ?: source.deliveredTurns
                        add(TurnDeskDetail(
                            crossWindowSourceLabel(source),
                            "请求 ${source.requestedTurns}轮 · 读取 ${sourceLoaded}轮 · 递给 ${sourceDelivered}轮"
                        ))
                    }
                    if (requestedTurns > 0 || loadedTurns > 0 || deliveredTurns > 0) {
                        add(TurnDeskDetail("本轮统计", "请求 ${requestedTurns}轮 · 读取 ${loadedTurns}轮 · 递给 ${deliveredTurns}轮"))
                    }
                    if (value.crossWindow.requestedMessages > 0 || value.crossWindow.loadedMessages > 0 || value.crossWindow.deliveredToModelMessages > 0) {
                        add(TurnDeskDetail(
                            "消息级统计",
                            "请求 ${value.crossWindow.requestedMessages}条 · 读取 ${value.crossWindow.loadedMessages}条 · 递给 ${value.crossWindow.deliveredToModelMessages}条"
                        ))
                    }
                    if (value.crossWindow.attemptedDeliveredTurns > 0) add(TurnDeskDetail("尝试递送", "${value.crossWindow.attemptedDeliveredTurns}轮"))
                    if (value.crossWindow.attemptedChars > 0) add(TurnDeskDetail("尝试字符", value.crossWindow.attemptedChars.toString()))
                    if (value.crossWindow.attemptedEstimatedTokens > 0) add(TurnDeskDetail("估算输入 token", value.crossWindow.attemptedEstimatedTokens.toString()))
                    value.crossWindow.failureReason?.takeIf(String::isNotBlank)?.let { add(TurnDeskDetail("失败原因", it)) }
                    if (value.crossWindow.providerErrorType.isNotBlank()) add(TurnDeskDetail("Provider 错误类型", value.crossWindow.providerErrorType))
                    if (value.crossWindow.providerErrorMessage.isNotBlank()) add(TurnDeskDetail("Provider 错误摘要", value.crossWindow.providerErrorMessage))
                    if (value.crossWindow.trimReason.isNotBlank()) add(TurnDeskDetail("裁剪原因", value.crossWindow.trimReason))
                    if (value.crossWindow.error.isNotBlank()) add(TurnDeskDetail("错误", value.crossWindow.error))
                    val sourceById = value.crossWindow.sources.associateBy { it.conversationId }
                    value.crossWindow.messages.forEach { group ->
                        val source = sourceById[group.conversationId]
                        val label = source?.let(::crossWindowSourceLabel).orEmpty().ifBlank { "来源窗口" }
                        group.messages.forEach { message ->
                            add(TurnDeskDetail("$label · ${if (message.role == "assistant") "Myri" else "用户"}", message.content))
                        }
                    }
                }
            ),
            section(
                title = value.workbench.label.ifBlank { "工作台 / 工具回执" },
                status = value.workbench.status,
                description = value.workbench.description,
                details = buildList {
                    if (value.workbench.coreTools.isNotEmpty()) add(TurnDeskDetail("常用工具", value.workbench.coreTools.joinToString("、", transform = ::toolLabel)))
                    if (value.workbench.sideTools.isNotEmpty()) add(TurnDeskDetail("海岸日报小工具", value.workbench.sideTools.joinToString("、", transform = ::toolLabel)))
                    if (value.workbench.modelVisibleTools.isNotEmpty()) add(TurnDeskDetail("模型可见工具", value.workbench.modelVisibleTools.joinToString("、", transform = ::toolLabel)))
                    if (value.workbench.backendTools.isNotEmpty()) add(TurnDeskDetail("后端可用工具", value.workbench.backendTools.joinToString("、", transform = ::toolLabel)))
                    val hasWorkbenchMaterial = value.workbench.promptDelivered ||
                        value.workbench.prompt.isNotBlank() ||
                        value.workbench.coreTools.isNotEmpty() ||
                        value.workbench.sideTools.isNotEmpty() ||
                        value.workbench.modelVisibleTools.isNotEmpty() ||
                        value.workbench.backendTools.isNotEmpty() ||
                        value.workbench.furniture.isNotEmpty() ||
                        value.workbench.toolResults.any { it.delivered && it.content.isNotBlank() }
                    if (hasWorkbenchMaterial) add(TurnDeskDetail("工作台提示", if (value.workbench.promptDelivered) "已递给" else "未递给"))
                    if (value.workbench.prompt.isNotBlank()) add(TurnDeskDetail("工作台提示全文", value.workbench.prompt))
                    if (value.workbench.furniture.isNotEmpty()) add(TurnDeskDetail("本轮动用", value.workbench.furniture.joinToString("、")))
                    value.workbench.toolResults.filter { it.delivered && it.content.isNotBlank() }.forEach { result ->
                        add(TurnDeskDetail("${result.name} · 工具结果 JSON", result.content))
                    }
                }
            ),
            section(
                title = value.contextBudget.label.ifBlank { "上下文预算" },
                status = buildString {
                    append("${value.contextBudget.estimatedTokens} / ${value.contextBudget.comfortCeiling} tokens")
                    if (value.contextBudget.exceedsComfortCeiling) append(" · 超出舒服区间")
                    else if (value.contextBudget.trimmed) append(" · 已裁剪")
                    else append(" · 未裁剪")
                },
                description = "",
                details = buildList {
                    add(TurnDeskDetail("裁剪", if (value.contextBudget.trimmed) "是 · ${value.contextBudget.trimmedCount} 项" else "否"))
                    if (value.contextBudget.sourcesPreserved.isNotEmpty()) add(TurnDeskDetail("保留来源", value.contextBudget.sourcesPreserved.joinToString("、")))
                    if (value.contextBudget.userPortrait.isNotBlank()) add(TurnDeskDetail("用户画像", value.contextBudget.userPortrait))
                    if (value.contextBudget.globalExcerpt.isNotBlank()) add(TurnDeskDetail("自我画像", value.contextBudget.globalExcerpt))
                }
            ),
            section(
                title = value.externalTide.label.ifBlank { "外来潮汐" },
                status = value.externalTide.status,
                description = value.externalTide.description,
                details = detail("", value.externalTide.content.ifBlank { "本轮没有递入外部材料。" })
            )
        ) + listOfNotNull(
            attachmentSection(value.attachments),
            webSearchSection(value.webSearch)
        )
    )

    private fun attachmentSection(value: RemoteDeskAttachments?): TurnDeskSection? {
        if (value == null) return null
        return section(
            title = "本轮附件",
            status = "上传 ${value.uploaded} · 递给 ${value.deliveredToModel}",
            description = "",
            details = buildList {
                add(TurnDeskDetail(
                    "识图",
                    if (value.vision.supported) "当前模型支持 · 已递 ${value.vision.imagesDelivered} 张"
                    else "当前模型未确认支持 · 已递 ${value.vision.imagesDelivered} 张"
                ))
                value.delivered.forEach { item ->
                    add(TurnDeskDetail(
                        item.name.ifBlank { item.id.ifBlank { "附件" } },
                        when (item.mode) {
                            "vision", "image" -> "图片 · 已递给模型（不代表已识别）"
                            "text" -> "文件 · 已递入文本"
                            else -> "已递给模型"
                        }
                    ))
                }
                value.notDelivered.forEach { item ->
                    add(TurnDeskDetail(
                        item.name.ifBlank { item.id.ifBlank { "附件" } },
                        "未递给模型 · ${item.reason.ifBlank { "unknown" }}"
                    ))
                }
            }
        )
    }

    private fun webSearchSection(value: RemoteDeskWebSearch?): TurnDeskSection? {
        if (value == null) return null
        val status = when {
            !value.available -> "不可用"
            value.used -> "已搜索 · ${value.requests} 次 · ${value.resultsCount} 来源"
            else -> "可用 · 本轮未调用"
        }
        return section(
            title = "本轮搜索",
            status = status,
            description = "",
            details = buildList {
                if (value.requestedQuery.isNotBlank()) add(TurnDeskDetail("本轮触发语句", value.requestedQuery))
                if (!value.providerQueryReturned) add(TurnDeskDetail("搜索 query", "provider 未回传内部原始 query；海岸不伪造。"))
                value.reason?.takeIf(String::isNotBlank)?.let { add(TurnDeskDetail("不可用原因", it)) }
                value.results.forEach { item ->
                    val body = listOf(item.url, item.content).filter(String::isNotBlank).joinToString("\n")
                    add(TurnDeskDetail(item.title.ifBlank { item.url.ifBlank { "搜索结果" } }, body))
                }
            }
        )
    }

    private fun crossWindowStatus(value: RemoteDeskCrossWindow): String {
        val windows = value.requestedWindows.takeIf { it > 0 } ?: value.windowCount
        val requested = value.requestedTurns.takeIf { it > 0 } ?: value.totalRequestedTurns
        val loaded = value.loadedTurns.takeIf { it > 0 } ?: value.totalLoadedTurns
        val delivered = value.deliveredToModelTurns.takeIf { it > 0 } ?: value.totalDeliveredTurns
        val attempted = value.attemptedDeliveredTurns
        val detail = when {
            value.status == "递送失败" -> "${windows}窗 · 请求 ${requested} · 读取 ${loaded} · 尝试 ${attempted}轮"
            windows > 0 || requested > 0 || loaded > 0 || delivered > 0 -> "${windows}窗 · 请求 ${requested} · 读取 ${loaded} · 递给 ${delivered}轮"
            else -> ""
        }
        return withDetail(value.status, detail)
    }

    private fun crossWindowSourceLabel(source: RemoteDeskCrossWindowSource): String = when {
        source.source == "rikkahub" -> "【Rikka】${source.title}"
        source.roomType == "radio" -> "电波｜${source.title}"
        source.roomType == "lighthouse" -> "灯塔｜${source.title}"
        else -> "主聊天｜${source.title}"
    }

    private fun section(
        title: String,
        status: String,
        description: String,
        details: List<TurnDeskDetail>
    ): TurnDeskSection = TurnDeskSection(
        title = title,
        status = status,
        details = buildList {
            if (description.isNotBlank()) add(TurnDeskDetail("来源说明", description))
            addAll(details)
            if (details.isEmpty()) add(TurnDeskDetail("", "本轮未递入"))
        }
    )

    private fun withDetail(status: String, detail: String): String =
        listOf(status, detail).filter(String::isNotBlank).joinToString(" · ")

    private fun detail(label: String, text: String): List<TurnDeskDetail> =
        text.takeIf(String::isNotBlank)?.let { listOf(TurnDeskDetail(label, it)) }.orEmpty()

    private fun toolLabel(tool: RemoteDeskTool): String {
        val display = tool.displayName.ifBlank { tool.name.ifBlank { tool.toolKey } }
        val technical = tool.name.ifBlank { tool.toolKey }
        return if (display.isNotBlank() && technical.isNotBlank() && display != technical) "$display｜$technical" else display
    }
}
