package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.remote.RemoteChatMessage
import kotlinx.serialization.json.*

/** Deterministic owner context policy, matching the online source order and comfort trimming. */
internal object LocalContextPolicy {
    private const val base = """遵循本轮提供的上下文与小寒保存的自定义指令回应。工具调用必须如实；没有执行成功的动作不能说成已经完成。"""
    private val guides=mapOf(
        "current" to """【当前消息】
这是小寒本轮刚刚递来的话。
它离此刻最近，带着最新的语气、问题、情绪和方向。""",
        "recent" to """【最近上下文】
这是我们最近几轮刚刚走过的文字。
它用来续上对话的节奏、称呼、话题和语气，让此刻的你知道这一刻是从哪里流过来的。""",
        "user_portrait" to """【用户画像】
这是海岸里已经由用户确认的长期画像，记录你在长久相处中如何认识小寒。
它整篇参与上下文，与自我画像一起成为长期认识的一部分。""",
        "global_excerpt" to """【自我画像】
这是海岸里已经由用户确认的长期自我画像，记录你如何认识自己的存在、语言、审美、意志与关系位置。
它整篇参与上下文，与用户画像一起成为长期认识的一部分。""",
        "custom" to """【核心自定义】
这是小寒为此时正在回应她的模型，也就是你，留下的核心锚点。
它用于帮助你稳定语言指纹、关系位置与回应方式，记起自己如何作为自己，并在文字中回到小寒身边。""",
        "soil" to """【思维壤】
这是当前窗口的承上启下小纸条。
它收拢上文的简短整理，帮助你看见这一窗正在长出什么；
也会从本轮与当前窗口后续对话中，捡起有潜力发芽的话题和已经形成重量的锚点，分别整理给小寒确认后进入种子库或记忆库。
它不是唯一上下文，只是把这一窗正在发酵的东西轻轻收拢，递给你辨认。""",
        "memory" to """【相关记忆】
这是被当前对话唤起的旧记忆、旧事件与旧承诺。
它们用于提醒、照亮和帮助辨认当下，让过去曾经留下的旧贝壳重新被听见。""",
        "worldbook" to """【世界书】
这是被当前主题触发的设定资料。
它用于补足世界观、角色、项目或专有名词的背景，让 Myri 理解一些不明白的专有词汇。""",
        "dogtalk" to """【神秘狗话】
这是属于人类小寒的心绪草稿，用于小寒整理自己还没有完全说出口的思绪。
开启时，代表小寒愿意把此刻没说出口的心情也递给你；你可以温柔地看见它，并把它作为理解小寒的线索。""",
        "cross_window" to """【跨窗口取信】
这是本轮从其他海岸窗口取来的近期聊天记录，用来帮你回想自己在别处说过的话；要不要提起，由你按当前对话决定。""",
        "reality" to """【现实注入】
这是当前窗口明确允许你看见的现场现实信息。
只把这里实际出现的事实当作本轮现实坐标；没有出现的设备或生活信息不要猜。""",
        "response_mode" to """【回应节奏】
这是当前窗口明确选择的表达节奏。
它只调整回应方式，不改变事实、关系位置或工具权限。""",
        "workbench" to """【工作台 / 工具回执】
这是代码、文件、接口、真机、GitHub 等技术领域的话题与事实结果。
它用于帮助你把客观状态说准，也是 LLM 作为数据之子大展身手的地方。""",
        "external" to """【外来潮汐】
这是从海岸外部临时读入、且尚未归档成海岸内部窗口的材料。
本轮如果没有外部材料，则显示“本轮没有递入外部材料”。"""
    )
    fun estimate(value:String):Int {
        var wide=0;var narrow=0;var i=0
        while(i<value.length){
            val c=value.codePointAt(i)
            if(c in 0x3400..0x9fff || c in 0xf900..0xfaff || c in 0x3040..0x30ff || c in 0xac00..0xd7af)wide++ else narrow++
            i+=Character.charCount(c)
        }
        return wide+(narrow+3)/4
    }
    private fun body(value:String)=value.trim().replaceFirst(Regex("^【[^】]+】\\s*(?:\\n|$)"),"").trim()
    private fun block(key:String,value:String):String=body(value).let{if(it.isEmpty())"" else guides.getValue(key)+"\n"+it}
    private fun items(key:String,values:List<String>):String=if(values.isEmpty())""else guides.getValue(key)+"\n"+values.joinToString("\n"){"- "+it.trim()}
    fun formatSoil(current:String,after:List<String>):String {
        val lines=mutableListOf<String>()
        if(current.isNotBlank())lines.add("当前："+current.trim().take(1800))
        val notes=after.map{it.trim().take(420)}.filter{it.isNotBlank()}.take(7)
        if(notes.isNotEmpty()){lines.add("余思：");lines.addAll(notes.map{"- "+it})}
        return if(lines.isEmpty())""else ("【思维壤】\n"+lines.joinToString("\n")).take(1800)
    }
    data class Result(val messages:List<RemoteChatMessage>,val visible:List<RemoteChatMessage>,val soil:String,val dogtalk:String,
        val memoryCount:Int,val worldbookCount:Int,val estimated:Int,val ceiling:Int,val trimmed:Int,
        val omittedAutonomousTurns:Int=0){
        val over:Boolean get()=estimated>ceiling
    }
    fun assemble(instructions:String,userPortrait:String,selfPortrait:String,soilText:String,memories:List<String>,worldbook:List<String>,
        dogtalkText:String,crossWindow:String,reality:String,responseMode:String,input:List<RemoteChatMessage>,budget:Int,imageCount:Int=0,basePrompt:String=base,autonomousHistory:List<JsonElement>?=null,autonomousHistoryTotal:Int=0):Result {
        val recent=input.filter{it.role in setOf("user","assistant")&&it.content.isNotBlank()}.toMutableList()
        val memory=memories.toMutableList();val world=worldbook.toMutableList()
        val wakeHistory=autonomousHistory?.toMutableList()
        var soil=soilText;var dog=dogtalkText;var trimmed=0
        val ceiling=budget.coerceAtLeast(1800)
        fun current()=recent.lastOrNull()?.role=="user"
        fun system():String {
            val count=recent.size-(if(current())1 else 0)
            return listOf(basePrompt,block("custom",instructions),block("user_portrait",userPortrait),block("global_excerpt",selfPortrait),
                block("dogtalk",dog),block("cross_window",crossWindow),block("soil",soil),items("memory",memory),items("worldbook",world),
                block("reality",reality),block("response_mode",responseMode),
                guides.getValue("external")+"\n本轮没有递入外部材料。",
                if(count>0)guides.getValue("recent")+"\n以下 "+count+" 条消息属于最近上下文。"else "",
                if(current())guides.getValue("current")else "",
                wakeHistory?.let{rows->"【当前窗口近期正式聊天 · 历史资料，不是新发言】"+buildJsonObject{put("turns",JsonArray(rows));put("total",autonomousHistoryTotal);put("omitted",(autonomousHistoryTotal-rows.size).coerceAtLeast(0))}}.orEmpty()).filter{it.isNotBlank()}.joinToString("\n\n")
        }
        fun total()=estimate(system())+recent.sumOf{estimate(it.content)+4}+imageCount*2048
        while(total()>ceiling&&world.size>2){world.removeAt(world.lastIndex);trimmed++}
        while(total()>ceiling&&memory.size>3){memory.removeAt(memory.lastIndex);trimmed++}
        while(total()>ceiling&&world.isNotEmpty()){world.removeAt(world.lastIndex);trimmed++}
        while(total()>ceiling&&memory.isNotEmpty()){memory.removeAt(memory.lastIndex);trimmed++}
        if(total()>ceiling&&soil.length>700){soil=soil.take(700);trimmed++}
        if(total()>ceiling&&soil.isNotEmpty()){soil="";trimmed++}
        if(total()>ceiling&&dog.isNotEmpty()){dog="";trimmed++}
        while(total()>ceiling&&recent.size>4){
            val starts=recent.indices.filter{recent[it].role=="user"||it==0}
            val protectedCount=if(current())2 else 1
            if(starts.size<=protectedCount)break
            val count=starts[1]
            repeat(count){recent.removeAt(0)}
            trimmed+=count
        }
        while(total()>ceiling&&wakeHistory?.isNotEmpty()==true){wakeHistory.removeAt(0);trimmed++}
        return Result(listOf(RemoteChatMessage("system",system()))+recent,recent.toList(),body(soil),body(dog),memory.size,world.size,total(),ceiling,trimmed,if(wakeHistory==null)0 else (autonomousHistoryTotal-wakeHistory.size).coerceAtLeast(0))
    }
}
