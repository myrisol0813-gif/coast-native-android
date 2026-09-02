package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.TurnDeskDetail
import com.elementeracoast.app.core.model.TurnDeskReceipt
import com.elementeracoast.app.core.model.TurnDeskSection
import com.elementeracoast.app.core.remote.RemoteDeskSlip

internal object TurnDeskMapper {
    fun toUi(value: RemoteDeskSlip): TurnDeskReceipt = TurnDeskReceipt(
        summary = value.summary.ifBlank { "本轮递给模型" },
        comfort = value.comfort,
        sections = listOf(
            TurnDeskSection(
                title = "当前消息",
                status = value.currentMessage.status,
                details = detail("", value.currentMessage.content)
            ),
            TurnDeskSection(
                title = "最近上下文",
                status = value.recentContext.status,
                details = value.recentContext.messages.map { message ->
                    TurnDeskDetail(if (message.role == "assistant") "Myri 回复" else "用户消息", message.content)
                }
            ),
            TurnDeskSection(
                title = "自定义指令",
                status = if (value.customInstructions.delivered && value.customInstructions.length > 0) {
                    "${value.customInstructions.status}，全文 ${value.customInstructions.length} 字"
                } else value.customInstructions.status,
                details = detail("全文", value.customInstructions.content)
            ),
            TurnDeskSection(
                title = "思维壤",
                status = value.thinkingSoil.status,
                details = buildList {
                    if (value.thinkingSoil.currentText.isNotBlank()) add(TurnDeskDetail("当前整理", value.thinkingSoil.currentText))
                    value.thinkingSoil.handSeeds.forEach { seed -> add(TurnDeskDetail("手持种", seed)) }
                    add(TurnDeskDetail("待确认候选", "${value.thinkingSoil.pocketCandidatesCount} · 未递给正文"))
                    if (value.thinkingSoil.context.isNotBlank()) add(TurnDeskDetail("实际递给模型", value.thinkingSoil.context))
                }
            ),
            TurnDeskSection(
                title = "相关记忆",
                status = value.relatedMemory.status,
                details = value.relatedMemory.items.flatMap { item ->
                    buildList {
                        val title = item.title.ifBlank { "未命名" }
                        add(TurnDeskDetail(title, listOf(item.entryType, item.tag, item.sourceWindow).filter(String::isNotBlank).joinToString(" · ")))
                        if (item.lifeCore.isNotBlank()) add(TurnDeskDetail("核心", item.lifeCore))
                        if (item.usageHint.isNotBlank()) add(TurnDeskDetail("使用时机", item.usageHint))
                        if (item.avoidHint.isNotBlank()) add(TurnDeskDetail("勿误用", item.avoidHint))
                        if (item.deliveredText.isNotBlank()) add(TurnDeskDetail("实际递给模型", item.deliveredText))
                    }
                }
            ),
            TurnDeskSection(
                title = "世界书",
                status = value.worldbook.status,
                details = value.worldbook.entries.flatMap { item ->
                    buildList {
                        add(TurnDeskDetail(item.title.ifBlank { "未命名" }, item.content))
                        if (item.deliveredText.isNotBlank()) add(TurnDeskDetail("实际递给模型", item.deliveredText))
                    }
                }
            ),
            TurnDeskSection(
                title = "神秘狗话",
                status = value.dogtalk.status,
                details = if (value.dogtalk.delivered) detail("实际递给模型", value.dogtalk.context) else emptyList()
            ),
            TurnDeskSection(
                title = "工作台",
                status = value.workbench.status,
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
            )
        )
    )

    private fun detail(label: String, text: String): List<TurnDeskDetail> =
        text.takeIf(String::isNotBlank)?.let { listOf(TurnDeskDetail(label, it)) }.orEmpty()

    private fun toolLabel(tool: com.elementeracoast.app.core.remote.RemoteDeskTool): String {
        val display = tool.displayName.ifBlank { tool.name.ifBlank { tool.toolKey } }
        val technical = tool.name.ifBlank { tool.toolKey }
        return if (display.isNotBlank() && technical.isNotBlank() && display != technical) "$display｜$technical" else display
    }
}
