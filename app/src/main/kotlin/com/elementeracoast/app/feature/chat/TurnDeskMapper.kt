package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.TurnDeskDetail
import com.elementeracoast.app.core.model.TurnDeskReceipt
import com.elementeracoast.app.core.model.TurnDeskSection
import com.elementeracoast.app.core.remote.RemoteDeskSlip
import com.elementeracoast.app.core.remote.RemoteDeskTool

internal object TurnDeskMapper {
    fun toUi(value: RemoteDeskSlip): TurnDeskReceipt = TurnDeskReceipt(
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
                title = value.thinkingSoil.label.ifBlank { "思维壤" },
                status = value.thinkingSoil.status,
                description = value.thinkingSoil.description,
                details = buildList {
                    if (value.thinkingSoil.currentText.isNotBlank()) add(TurnDeskDetail("当前整理", value.thinkingSoil.currentText))
                    value.thinkingSoil.handSeeds.forEach { seed -> add(TurnDeskDetail("手持种", seed)) }
                    add(TurnDeskDetail(
                        "待确认候选",
                        "${value.thinkingSoil.pocketCandidatesStatus.ifBlank { if (value.thinkingSoil.pocketCandidatesCount > 0) "待确认" else "未递入" }} · ${value.thinkingSoil.pocketCandidatesCount} 条 · 未递给正文"
                    ))
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
                title = value.dogtalk.label.ifBlank { "神秘狗话" },
                status = value.dogtalk.status,
                description = value.dogtalk.description,
                details = if (value.dogtalk.delivered) detail("实际递给模型", value.dogtalk.context) else emptyList()
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
                    add(TurnDeskDetail("工作台提示", if (value.workbench.promptDelivered) "已递给" else "未递给"))
                    if (value.workbench.prompt.isNotBlank()) add(TurnDeskDetail("工作台提示全文", value.workbench.prompt))
                    if (value.workbench.furniture.isNotEmpty()) add(TurnDeskDetail("本轮动用", value.workbench.furniture.joinToString("、")))
                    value.workbench.toolResults.filter { it.delivered && it.content.isNotBlank() }.forEach { result ->
                        add(TurnDeskDetail("${result.name} · 工具结果 JSON", result.content))
                    }
                }
            ),
            section(
                title = value.externalTide.label.ifBlank { "外来潮汐" },
                status = value.externalTide.status,
                description = value.externalTide.description,
                details = detail("", value.externalTide.content.ifBlank { "本轮没有递入外部材料。" })
            )
        )
    )

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
