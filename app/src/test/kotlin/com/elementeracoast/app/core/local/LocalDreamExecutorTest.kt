package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.remote.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class LocalDreamExecutorTest {
    private val cid="wake-window"
    private val model="local:fixture/fixture/model"
    private val at=Instant.parse("2026-11-27T09:00:00Z").toEpochMilli()
    private val cfg=DreamConfig(timeZone="UTC",dailyTime="09:00",allowExtra=true,
        execution=DreamExecutionConfig(invitation="自在活动",tokenBudget=12000,outputLimit=500,toolBudget=4,readTurns=5,extraPerDay=1,readHistory=true,writeNote=true,includeReplies=true))
    private fun fixture():Pair<MemoryLocalPersistence,DreamSettings>{
        val store=MemoryLocalPersistence();val cache=RemoteCacheStore(store)
        val stamp=Instant.ofEpochMilli(at-86400000).toString()
        cache.putConversations(listOf(RemoteConversation(cid,"wake","main",createdAt=stamp,updatedAt=stamp),RemoteConversation("other","other","main",createdAt=stamp,updatedAt=stamp)))
        cache.putHistory(cid,RemoteHistory(conversationId=cid,updatedAt=stamp,turns=listOf(RemoteTurn("t",user=RemoteUserBranch(variants=listOf(RemoteVariant("u","window-original")))))))
        cache.putHistory("other",RemoteHistory(conversationId="other",updatedAt=stamp,turns=listOf(RemoteTurn("secret",user=RemoteUserBranch(variants=listOf(RemoteVariant("s","other-window-secret")))))))
        val ledger=LocalDreamLedger(store)
        val draft=ledger.configure(cid,DreamSettingsRequest(cid,null,"save",cfg,model),false,at-86400000)!!
        assertFalse(draft.enabled)
        assertNull(ledger.configure(cid,DreamSettingsRequest(cid,draft.revision,"arm",cfg,model,false),true,at-86400000))
        val armed=ledger.configure(cid,DreamSettingsRequest(cid,draft.revision,"arm",cfg,model,true),true,at-86400000)!!
        return store to armed
    }
    private fun call(id:String,name:String,args:JsonObject)=buildJsonObject{
        put("id",id);put("type","function");putJsonObject("function"){put("name",name);put("arguments",args.toString())}
    }
    private fun response(vararg calls:JsonObject,withUsage:Boolean=true)=buildJsonObject{
        put("model","fixture/model")
        if(withUsage)putJsonObject("usage"){put("total_tokens",120);put("prompt_tokens",100);put("completion_tokens",20);put("cost",0.002)}
        putJsonArray("choices"){add(buildJsonObject{
            put("finish_reason",if(calls.isEmpty())"stop"else "tool_calls")
            putJsonObject("message"){put("content","");put("tool_calls",JsonArray(calls.toList()))}
        })}
    }
    private val provider=LocalProviderConfig("fixture","fixture","https://example.com/v1","fixture-key",listOf("fixture/model"))
    @Test fun autonomousActivityPersistsToolReceiptsAndDoesNotAppendChat()=runBlocking{
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);var requests=0
        val engine=LocalDreamExecutor(store,{if(it==model)provider to "fixture/model" else null},{_,_,messages,_,_->
            requests++;assertFalse(messages.toString().contains("other-window-secret"))
            when(requests){
                1->{assertEquals("system",messages.single().jsonObject.getValue("role").jsonPrimitive.content)
                    assertFalse(messages.toString().contains("window-original"))
                    response(call("read","dream_read_history",buildJsonObject{put("mode","window");put("start",0);put("count",1)}))}
                2->{assertTrue(messages.toString().contains("window-original"))
                    response(call("note","dream_write_note",buildJsonObject{put("body","独立手记")}),call("extra","dream_schedule_extra",buildJsonObject{put("at",Instant.ofEpochMilli(at+3600000).toString())}))}
                else->response()
            }
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        assertEquals(3,requests)
        assertFalse(ledger.active(cid))
        assertEquals(at+3600000,ledger.settings(cid).extraAt)
        val notes=LocalDreamArchive(store).notes(cid,0).getValue("items").jsonArray
        assertEquals("独立手记",notes.single().jsonObject.getValue("body").jsonPrimitive.content)
        val claim=notes.single().jsonObject.getValue("claim_id").jsonPrimitive.content
        assertTrue(LocalDreamArchive(store).reply(cid,claim,"reply","历史回复"))
        assertEquals(3,requests)
        val history=LocalDreamArchive(store).export(cid)!!
        assertEquals(3,history.getValue("steps").jsonArray.size)
        assertEquals(3,history.getValue("tools").jsonArray.size)
        assertFalse(history.toString().contains("window-original"))
        assertFalse(history.getValue("settings").jsonObject.containsKey("enabled"))
        assertEquals(1,RemoteCacheStore(store).history(cid)!!.turns.size)
        assertEquals("not_due",engine.run(cid,settings.revision!!,model,at))
        assertEquals(3,requests)
    }
    @Test fun wakeStickerCatalogueAndRepeatedPureStickerDelivery()=runBlocking{
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val h=cache.history(cid)!!
        cache.putHistory(cid,h.copy(contextSettings=h.contextSettings.copy(stickerSendEnabled=true,localModelToolsEnabled=true,localModelWriteEnabled=true)))
        val bytes=ByteArray(24);byteArrayOf(137.toByte(),80,78,71,13,10,26,10).copyInto(bytes)
        val library=LocalStickerLibrary(store)
        val sticker=library.upload("fixture.png","image/png","fixture happy",bytes)
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowMessage=true,allowStickerSend=true,tokenBudget=40000,toolBudget=6))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,tools,_->
            requests++
            assertTrue(tools.toString().contains("dream_sticker_search"))
            when(requests){
                1->response(call("search","dream_sticker_search",buildJsonObject{put("q","fixture")}))
                2->{assertTrue(messages.toString().contains(sticker.id));response(call("send","dream_send_message",buildJsonObject{put("body","");put("sticker_ids",JsonArray(listOf(JsonPrimitive(sticker.id),JsonPrimitive(sticker.id))))}))}
                3->{library.update(sticker.id,"owner",enabled=false);response(call("off","dream_send_message",buildJsonObject{put("body","");put("sticker_ids",JsonArray(listOf(JsonPrimitive(sticker.id))))}))}
                else->response()
            }
        },clock={at})
        assertEquals("completed",engine.run(cid,arm.revision!!,model,at))
        val msg=cache.history(cid)!!.turns.filter{it.turnType=="dream_tide"}.single().assistant.variantsByUserVariant.getValue("0").single()
        assertEquals("",msg.content);assertEquals(2,msg.attachments.size);assertNotEquals(msg.attachments[0].id,msg.attachments[1].id)
        val originals=LocalAttachmentStore(store)
        msg.attachments.forEach{a->assertEquals(sticker.id,a.stickerDelivery?.stickerId);assertArrayEquals(bytes,originals.bytes(originals.entries().single{it.metadata.getValue("id").jsonPrimitive.content==a.id}))}
    }
    @Test fun voiceOnlyWakeMessageLinksSavedOriginalWithoutSynthesis(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val h=cache.history(cid)!!
        cache.putHistory(cid,h.copy(contextSettings=h.contextSettings.copy(localModelWriteEnabled=true)))
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowMessage=true,allowVoice=true,toolBudget=6))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,arm.revision!!,model,at)!!
        assertTrue(ledger.reserveStep(claim,0,at));assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
        assertTrue(ledger.reserveTool(claim,0,0,"voice_create",at))
        val voiceId="voice_dream_"+claim.id+"_0_0";val mp3=byteArrayOf(73,68,51,4,0,0,0,0,0,0,1,2)
        store.atomic{s->saveLocalVoice(s,cid,null,null,"dream_tide",model,LocalVoiceProvider.Generated("fixtureVoice","fixture speech",mp3),voiceId)}
        assertTrue(ledger.recordTool(claim,0,0,true,null,at,voiceId))
        assertTrue(ledger.reserveTool(claim,0,1,"dream_send_message",at))
        assertNull(ledger.sendMessage(claim,0,1,"",at,voiceIds=listOf("voice_dream_foreign_0_0")))
        val sent=ledger.sendMessage(claim,0,1,"",at,voiceIds=listOf(voiceId));assertNotNull(sent)
        assertTrue(ledger.recordTool(claim,0,1,true,null,at,sent))
        assertTrue(ledger.reserveTool(claim,0,2,"dream_send_message",at))
        assertNull(ledger.sendMessage(claim,0,2,"",at,voiceIds=listOf(voiceId)))
        val turn=cache.history(cid)!!.turns.last();val msg=turn.assistant.variantsByUserVariant.getValue("0").single()
        assertEquals("",msg.content);assertEquals(listOf(voiceId),msg.voiceIds)
        val records=LocalSideRecords(store);val clip=records.audio(voiceId)!!
        assertEquals(turn.id,clip.data.getValue("turn_id").jsonPrimitive.content);assertEquals(sent,clip.data.getValue("message_id").jsonPrimitive.content);assertArrayEquals(mp3,records.bytes(clip))
    }
    @Test fun localApiWakeOrganizesTheActualSentMessageWithinBudget()=runBlocking{
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store)
        LocalSoilStore(store).patch(cid,buildJsonObject{put("auto_refresh_enabled",true)})
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowMessage=true,tokenBudget=22000,toolBudget=5))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,tools,_->
            requests++
            when(requests){
                1->response(call("send","dream_send_message",buildJsonObject{put("body","wake soil source")}))
                2->response()
                else->{
                    assertTrue(tools.isEmpty());assertTrue(messages.toString().contains("wake soil source"))
                    buildJsonObject{
                        put("model","fixture/model");putJsonObject("usage"){put("total_tokens",120);put("prompt_tokens",100);put("completion_tokens",20);put("cost",0.002)}
                        putJsonArray("choices"){add(buildJsonObject{put("finish_reason","stop");putJsonObject("message"){put("content","""{"current_text":"wake soil","afterthoughts":[]}""")}})}
                    }
                }
            }
        },clock={at})
        assertEquals("completed",engine.run(cid,arm.revision!!,model,at));assertEquals(3,requests)
        val message=RemoteCacheStore(store).history(cid)!!.turns.last().assistant.variantsByUserVariant.getValue("0").single()
        val soil=LocalSoilStore(store).read()
        assertEquals(message.id,soil.history.last().getValue("assistant_message_id").jsonPrimitive.content)
        assertEquals("wake soil",soil.current.single().getValue("current_text").jsonPrimitive.content)
    }
    @Test fun furnitureReceiptsAttachOnceToEachFormalWakeMessage(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store)
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowMessage=true,allowLibrary=true,toolBudget=5))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,arm.revision!!,model,at)!!
        for(step in 0..1){
            assertTrue(ledger.reserveStep(claim,step,at))
            assertTrue(ledger.recordStep(claim,step,true,buildJsonObject{},at))
            assertTrue(ledger.reserveTool(claim,step,0,"library_books",at))
            assertTrue(ledger.recordTool(claim,step,0,true,null,at,furniture=RemoteFurnitureRun("original-audit-"+step,"library.books","翻阅书架")))
            assertTrue(ledger.reserveTool(claim,step,1,"dream_send_message",at))
            val id=ledger.sendMessage(claim,step,1,"message "+step,at)!!
            assertTrue(ledger.recordTool(claim,step,1,true,null,at,id))
        }
        val messages=RemoteCacheStore(store).history(cid)!!.turns.filter{it.turnType=="dream_tide"}.map{it.assistant.variantsByUserVariant.getValue("0").single()}
        assertEquals(2,messages.size)
        assertEquals(listOf("library.books","library.books"),messages.map{it.furnitureRuns.single().toolKey})
        assertNotEquals(messages[0].furnitureRuns.single().id,messages[1].furnitureRuns.single().id)
        assertEquals("dream:"+claim.id+":0:0",messages[0].furnitureRuns.single().id)
        assertEquals("dream:"+claim.id+":1:0",messages[1].furnitureRuns.single().id)
    }
    @Test fun sharedComfortTrimsWholeAutonomousHistoryWithoutFakeUser(){
        val old=buildJsonObject{put("turn_id","old");put("body","旧".repeat(2200)+"old-tail")}
        val recent=buildJsonObject{put("turn_id","new");put("body","new-wake")}
        val result=LocalContextPolicy.assemble("whole-core","whole-portrait","","",emptyList(),emptyList(),"","","","",emptyList(),1800,basePrompt="自主活动，不是人类新发言",autonomousHistory=listOf(old,recent),autonomousHistoryTotal=5)
        val message=result.messages.single()
        assertEquals("system",message.role);assertTrue(message.content.contains("whole-core"))
        assertTrue(message.content.contains("whole-portrait"));assertFalse(message.content.contains("old-tail"))
        assertTrue(message.content.contains("new-wake"));assertEquals(4,result.omittedAutonomousTurns)
    }
    @Test fun autonomousCrossWindowAndReadbackRespectBothGrants(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val history=cache.history(cid)!!
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowCrossWindow=true,allowDreamReadback=true))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,arm.revision!!,model,at)!!
        val names=setOf("cross_window_search","cross_window_read_recent","cross_window_keyword_search","cross_window_read_messages","dream_readback")
        assertNull(LocalToolSession.openAutonomous(store,claim))
        val ctx=history.contextSettings.copy(localModelToolsEnabled=true,crossWindowAuto=true,dreamReadbackEnabled=true)
        cache.putHistory(cid,history.copy(contextSettings=ctx))
        val session=LocalToolSession.openAutonomous(store,claim)!!
        val exposed=session.completionsTools.map{it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content}.toSet()
        assertTrue(exposed.containsAll(names))
        val denied=claim.copy(config=cfg)
        assertTrue(names.none{LocalDreamToolPolicy.allowed(denied,it)})
        cache.putHistory(cid,history.copy(contextSettings=ctx.copy(crossWindowAuto=false,dreamReadbackEnabled=false)))
        val closed=LocalToolSession.openAutonomous(store,claim)?.completionsTools.orEmpty().map{it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content}
        assertTrue(closed.none{it in names})
    }
    @Test fun autonomousDogtalkDeliveryKeepsChangedOrFailedDraft()=runBlocking{
        for(mode in listOf("off","private","success","failed","changed")){
            val (store,settings)=fixture();val ledger=LocalDreamLedger(store)
            val dog=LocalDogtalkStore(store)
            fun draft(body:String,readMode:String)=dog.save(buildJsonObject{put("room_scope","conversation");put("conversation_id",cid);put("body",body);put("read_mode",readMode)})
            draft("dogtalk-original",if(mode=="private")"keep_private"else "read_now")
            val updated=cfg.copy(execution=cfg.execution!!.copy(includeDogtalk=mode!="off",tokenBudget=22000))
            val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
            val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,_,_->
                assertEquals(mode !in listOf("off","private"),messages.toString().contains("dogtalk-original"))
                if(mode=="changed")draft("dogtalk-new-edit","read_now")
                if(mode=="failed")error("provider failed")
                response()
            },clock={at})
            engine.run(cid,arm.revision!!,model,at)
            val live=dog.public("conversation",cid)
            assertEquals(if(mode in listOf("private","success"))"keep_private"else "read_now",live.getValue("read_mode").jsonPrimitive.content)
            if(mode=="changed")assertEquals("dogtalk-new-edit",live.getValue("body").jsonPrimitive.content)
            val snapshots=dog.read().filter{it.module=="dogtalk_snapshots"}
            assertEquals(if(mode in listOf("off","private"))0 else 1,snapshots.size)
            if(snapshots.isNotEmpty())assertEquals("dream_tide",snapshots.single().data.getValue("source_type").jsonPrimitive.content)
            assertEquals(1,RemoteCacheStore(store).history(cid)!!.turns.size)
        }
    }
    @Test fun autonomousDailyAndWorldbookReadersRequireExplicitGrant(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val history=cache.history(cid)!!
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowDailyRead=true,allowWorldbookRead=true))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,arm.revision!!,model,at)!!
        val names=setOf("list_daily_moments","list_daily_diaries","worldbook_list")
        assertNull(LocalToolSession.openAutonomous(store,claim))
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelToolsEnabled=true)))
        val session=LocalToolSession.openAutonomous(store,claim)!!
        val exposed=session.completionsTools.map{it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content}.toSet()
        assertTrue(exposed.containsAll(names))
        assertTrue(names.none{LocalDreamToolPolicy.allowed(claim.copy(config=cfg),it)})
    }
    @Test fun autonomousDailyInteractionIsSeparateFromPosting(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val history=cache.history(cid)!!
        val updated=cfg.copy(execution=cfg.execution!!.copy(allowDailyInteract=true))
        val arm=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"arm",updated,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,arm.revision!!,model,at)!!
        assertNull(LocalToolSession.openAutonomous(store,claim))
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val session=LocalToolSession.openAutonomous(store,claim)!!
        val exposed=session.completionsTools.map{it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content}.toSet()
        assertTrue(exposed.containsAll(setOf("moment_comment","moment_like")))
        assertFalse(exposed.contains("create_moment"))
        assertFalse(LocalDreamToolPolicy.allowed(claim.copy(config=cfg),"moment_comment"))
    }
    @Test fun wakePortraitProposalKeepsOfficialDocumentUnchanged()=runBlocking{
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        LocalSoilStore(store).patch(cid,buildJsonObject{put("auto_refresh_enabled",true)})
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(includeSelfPortrait=true,allowSelfPortraitProposal=true,includeUserPortrait=false,allowUserPortraitProposal=true,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        val before=LocalPortraitStore(store).body("self_portrait")
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,tools,_->
            requests++
            if(requests==1){
                assertTrue(tools.toString().contains("global_excerpt_propose"))
                assertFalse(tools.toString().contains("user_portrait_propose"))
                response(call("p","global_excerpt_propose",buildJsonObject{put("proposed_body","fixture complete new portrait");put("reason","fixture lasting observation");put("change_kind","rewrite")}))
            }else response()
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        val portraits=LocalPortraitStore(store)
        assertEquals(before,portraits.body("self_portrait"))
        val candidate=portraits.read("self_portrait")!!.candidates.single()
        assertEquals(cid,candidate.getValue("source_conversation_id").jsonPrimitive.content)
        assertEquals(JsonNull,candidate["source_message_id"])
        assertTrue(candidate.getValue("id").jsonPrimitive.content.startsWith("portrait_dream_"))
        assertTrue(portraits.read("user_portrait")?.candidates.orEmpty().isEmpty())
    }
    @Test fun wakeCalendarMutationProtectsOwnerAndOtherWindowRecords()=runBlocking{
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val cal=LocalCalendarStore(store)
        fun add(title:String)=cal.save(null,buildJsonObject{put("day","2026-11-28");put("title",title);put("kind","note")})
        val owner=add("owner")
        val mine=add("mine")
        val key=mine.getValue("id").jsonPrimitive.content
        val ownerKey=owner.getValue("id").jsonPrimitive.content
        cal.restoreSnapshot(JsonArray(cal.snapshot().map{row->if(row.jsonObject.getValue("id").jsonPrimitive.content==key)JsonObject(row.jsonObject+buildJsonObject{put("author","assistant");put("conversation_id",cid);put("source","chat_tool");put("source_turn_id",JsonNull);put("tool_call_id","dream:fixture:0:0");put("model_label",model)})else row}))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowCalendarEdit=true,tokenBudget=24000,toolBudget=5))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,tools,_->
            requests++
            if(requests==1){
                assertTrue(tools.toString().contains("calendar_update"))
                assertFalse(tools.toString().contains("calendar_create"))
                response(call("edit","calendar_update",buildJsonObject{put("id",key);put("title","edited")}),
                    call("owner","calendar_delete",buildJsonObject{put("id",ownerKey)}))
            }else response()
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        val rows=cal.snapshot().map{it.jsonObject}
        assertEquals("edited",rows.single{it.getValue("id").jsonPrimitive.content==key}.getValue("title").jsonPrimitive.content)
        assertEquals("owner",rows.single{it.getValue("id").jsonPrimitive.content==ownerKey}.getValue("title").jsonPrimitive.content)
    }
    @Test fun historyTextOriginalRequiresCorrectWindowAndVisibleMessage()=runBlocking{
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val text="history-original-text-fixture"
        val originals=LocalAttachmentStore(store)
        val original=originals.upload(cid,"fixture.txt","text/plain",text.encodeToByteArray())
        val other=originals.upload("other","other.txt","text/plain","secret".encodeToByteArray())
        val history=cache.history(cid)!!
        val turn=history.turns.single()
        cache.putHistory(cid,history.copy(turns=listOf(turn.copy(user=turn.user.copy(variants=listOf(turn.user.variants.single().copy(attachments=listOf(original))))))))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(includeHistoryAttachments=true,allowHistoryFileRead=true,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        var requests=0
        fun args(id:String)=buildJsonObject{put("start",0);put("message_id","u");put("attachment_id",id)}
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,tools,_->
            if(++requests==1){assertTrue(tools.toString().contains("dream_read_history_file"));response(call("good","dream_read_history_file",args(original.id)),call("wrong","dream_read_history_file",args(other.id)))}
            else{assertTrue(messages.toString().contains(text));response()}
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        val events=LocalDreamArchive(store).export(cid)!!.getValue("tools").jsonArray.map{it.jsonObject}
        assertEquals(listOf("completed","failed"),events.map{it.getValue("status").jsonPrimitive.content})
        assertFalse(events.toString().contains(text))
    }
    @Test fun historicalImageOriginalIsDeliveredOnlyToKnownVisionModel()=runBlocking{
        for(vision in listOf(true,false)){
            val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
            val bytes=byteArrayOf(137.toByte(),80,78,71,13,10,26,10)+ByteArray(16)
            val original=LocalAttachmentStore(store).upload(cid,"fixture.png","image/png",bytes)
            val h=cache.history(cid)!!;val t=h.turns.single()
            cache.putHistory(cid,h.copy(turns=listOf(t.copy(user=t.user.copy(variants=listOf(t.user.variants.single().copy(attachments=listOf(original))))))))
            val cfg2=cfg.copy(execution=cfg.execution!!.copy(includeHistoryAttachments=true,allowHistoryImageRead=true,tokenBudget=24000))
            val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
            var requests=0;var seen=0
            val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,tools,_->
                if(++requests==1){
                    assertEquals(vision,tools.toString().contains("dream_read_history_image"))
                    if(vision)response(call("i","dream_read_history_image",buildJsonObject{put("start",0);put("message_id","u");put("attachment_id",original.id)}))else response()
                }else{
                    if(messages.toString().contains("data:image/png;base64,"))seen++
                    assertTrue(messages.toString().contains("历史图像原件"))
                    response()
                }
            },clock={at},modelBudget={_,_,_->LocalInputBudget(18000,null,100000,"fixture",vision)})
            assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
            assertEquals(if(vision)1 else 0,seen)
            val archive=LocalDreamArchive(store).export(cid)!!.toString()
            assertEquals(vision,archive.contains("image_readback_accepted"))
            assertFalse(archive.contains("data:image/"));assertEquals(1,cache.history(cid)!!.turns.size)
        }
    }
    @Test fun editsPreserveFutureExtraAppointmentIdentity(){
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store)
        val claim=ledger.claim(cid,settings.revision!!,model,at)!!
        val extraAt=at+7200000
        assertTrue(ledger.finish(claim,"completed",buildJsonObject{},extraAt,at+1))
        val original=ledger.settings(cid)
        val changed=cfg.copy(dailyTime="10:00",execution=cfg.execution!!.copy(tokenBudget=24000))
        val edited=ledger.configure(cid,DreamSettingsRequest(cid,original.revision,"arm",changed,model,true),true,at+2)!!
        assertEquals(extraAt,edited.extraAt);assertEquals(original.extraId,edited.extraId)
        val again=ledger.configure(cid,DreamSettingsRequest(cid,edited.revision,"arm",changed.copy(execution=changed.execution!!.copy(invitation="new invitation")),model,true),true,at+3)!!
        assertEquals(extraAt,again.extraAt);assertEquals(original.extraId,again.extraId);assertEquals(edited.armedAt,again.armedAt)
        val fixed=ledger.claim(cid,again.revision!!,model,at+3600000)!!
        assertEquals("daily",fixed.slot.kind);assertTrue(ledger.finish(fixed,"completed",buildJsonObject{},null,at+3600001))
        val extra=ledger.claim(cid,again.revision!!,model,extraAt)!!
        assertEquals("extra:"+original.extraId,extra.slot.key)
        assertTrue(ledger.finish(extra,"completed",buildJsonObject{},null,extraAt+1))
        assertNull(ledger.settings(cid).extraAt)
        assertNull(ledger.claim(cid,again.revision!!,model,extraAt+2))
    }
    @Test fun unknownUsageStopsBeforeToolsAndRevokedClaimCannotWrite()=runBlocking{
        val (store,settings)=fixture();val ledger=LocalDreamLedger(store);var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,_,_->
            requests++;response(call("note","dream_write_note",buildJsonObject{put("body","not saved")}),withUsage=false)
        },clock={at})
        assertEquals("failed",engine.run(cid,settings.revision!!,model,at))
        assertEquals(1,requests);assertEquals(0,LocalDreamArchive(store).notes(cid,0).getValue("items").jsonArray.size)
        val tomorrow=at+86400000
        val claim=ledger.claim(cid,settings.revision!!,model,tomorrow)!!
        assertTrue(ledger.reserveStep(claim,0,tomorrow))
        val disarmed=ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"disarm"),false,tomorrow)!!
        assertFalse(disarmed.enabled);assertNotEquals(settings.revision,disarmed.revision)
        assertFalse(ledger.authorized(claim))
        assertFalse(ledger.note(claim,"revoked",tomorrow))
        assertTrue(ledger.recover(cid,tomorrow+1))
        assertFalse(ledger.active(cid))
        assertNull(ledger.claim(cid,settings.revision!!,model,tomorrow))
        val rows=LocalDreamArchive(store).export(cid)!!.getValue("runs").jsonArray
        assertEquals("interrupted",rows.last().jsonObject.getValue("status").jsonPrimitive.content)
    }
    @Test fun notificationsFollowPersistedEventsAndDoNotRetryInference()=runBlocking{
        val (store,settings)=fixture()
        val events=mutableListOf<Pair<String,String>>()
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,_,_->
            requests++
            if(requests==1)response(call("note","dream_write_note",buildJsonObject{put("body","notification fixture")})) else response()
        },clock={at},onNotice={window,claim,kind->
            val history=LocalDreamArchive(store).runtimeHistory(window)
            val run=history.getValue("runs").jsonArray.first{it.jsonObject.getValue("claim_id").jsonPrimitive.content==claim}
            events.add(kind to if(kind=="note")history.getValue("notes").jsonArray.single().jsonObject.getValue("claim_id").jsonPrimitive.content else run.jsonObject.getValue("claim_id").jsonPrimitive.content)
            // A system notification failure must not affect the saved activity.
            if(kind=="note")throw IllegalStateException("notification disabled")
        })
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        assertEquals(2,requests)
        assertEquals(listOf("start","note"),events.map{it.first})
        assertEquals(1,events.map{it.second}.distinct().size)
        assertEquals("not_due",engine.run(cid,settings.revision!!,model,at))
        assertEquals(2,events.size)
        val (emptyStore,emptySettings)=fixture()
        val silent=mutableListOf<String>()
        val empty=LocalDreamExecutor(emptyStore,{provider to "fixture/model"},{_,_,_,_,_->response()},clock={at},onNotice={_,_,kind->silent.add(kind)})
        assertEquals("completed",empty.run(cid,emptySettings.revision!!,model,at))
        assertEquals(listOf("start"),silent)
        val (badStore,badSettings)=fixture()
        val failed=mutableListOf<Pair<String,String>>()
        val bad=LocalDreamExecutor(badStore,{provider to "fixture/model"},{_,_,_,_,_->response(withUsage=false)},clock={at},onNotice={window,claim,kind->
            val status=LocalDreamArchive(badStore).runtimeHistory(window).getValue("runs").jsonArray.single{it.jsonObject.getValue("claim_id").jsonPrimitive.content==claim}.jsonObject.getValue("status").jsonPrimitive.content
            failed.add(kind to status)
        })
        assertEquals("failed",bad.run(cid,badSettings.revision!!,model,at))
        assertEquals(listOf("start" to "running","failure" to "failed"),failed)
    }

    @Test fun largerOwnerBudgetsSaveAndArchiveWithoutClamping(){
        val (store,armed)=fixture()
        val e=cfg.execution!!.copy(tokenBudget=2000000,outputLimit=100000,toolBudget=200,readTurns=500,extraPerDay=200,voiceLimit=200,imageLimit=200)
        val ledger=LocalDreamLedger(store)
        val paused=ledger.configure(cid,DreamSettingsRequest(cid,armed.revision,"disarm"),false,at)!!
        val updated=ledger.configure(cid,DreamSettingsRequest(cid,paused.revision,"save",cfg.copy(execution=e),model),false,at)!!
        assertEquals(e,updated.config!!.execution)
        assertNotNull(LocalDreamArchive(store).export(cid))
        assertFalse(updated.enabled)
    }
    @Test fun ordinarySaveKeepsExistingAuthorizationAndDoesNotWidenPermissions(){
        val (store,armed)=fixture();val ledger=LocalDreamLedger(store)
        val unchanged=ledger.configure(cid,DreamSettingsRequest(cid,armed.revision,"save",cfg,model),false,at)!!
        assertTrue(unchanged.enabled);assertEquals(armed.revision,unchanged.revision)
        try{
            ledger.configure(cid,DreamSettingsRequest(cid,armed.revision,"save",cfg.copy(execution=cfg.execution!!.copy(tokenBudget=20000)),model),false,at)
            fail("permission change must require explicit authorization")
        }catch(_:IllegalArgumentException){}
        assertEquals(armed,ledger.settings(cid))
        val edited=ledger.configure(cid,DreamSettingsRequest(cid,armed.revision,"save",cfg.copy(dailyTime="10:00"),model),false,at)!!
        assertTrue(edited.enabled);assertEquals("10:00",edited.config!!.dailyTime);assertNotEquals(armed.revision,edited.revision)
    }
    @Test fun planScheduleEditKeepsIndependentGrantAndChangesConfigurationFence(){
        val (store,armed)=fixture();val ledger=LocalDreamLedger(store)
        val plan="chatgpt-plan:gpt-6.2-sol";val grant="grant-fixture"
        val enabled=ledger.configure(cid,DreamSettingsRequest(cid,armed.revision,"arm",cfg,plan,true,true,5),true,at-86400000,grant)!!
        assertEquals(grant,enabled.grantRevision())
        val edited=ledger.configure(cid,DreamSettingsRequest(cid,enabled.revision,"save",cfg.copy(dailyTime="10:00"),plan),false,at)!!
        assertTrue(edited.enabled);assertNotEquals(enabled.revision,edited.revision);assertEquals(grant,edited.grantRevision())
        assertNull(ledger.claim(cid,enabled.revision!!,plan,at+3600000))
        assertNotNull(ledger.claim(cid,edited.revision!!,plan,at+3600000))
        assertFalse(LocalDreamArchive(store).export(cid)!!.toString().contains("authorizationRevision"))
    }
    @Test fun scheduleDisplayDistinguishesDueQuietConsumedAndExpired(){
        val (_,armed)=fixture()
        val extra=armed.copy(extraAt=at-60000,extraId="fixture")
        val daily=LocalDreamSchedule.view(extra,emptySet(),0,at)
        assertEquals("due",daily.dailyStatus);assertEquals("due",daily.extraStatus);assertEquals("daily",daily.eligibleKind)
        val used=setOf("daily:2026-11-27")
        assertEquals("extra",LocalDreamSchedule.view(extra,used,0,at).eligibleKind)
        assertEquals("quiet",LocalDreamSchedule.view(extra.copy(config=cfg.copy(quietHours=DreamQuietHours("00:00","00:00"))),used,0,at).extraStatus)
        assertNull(LocalDreamSchedule.view(extra.copy(running=true),used,0,at).eligibleKind)
        assertNull(LocalDreamSchedule.view(extra,used,1,at).extraAt)
        assertNull(LocalDreamSchedule.view(extra,used,0,at+86400000).extraAt)
    }
    @Test fun scheduleQuietAndAlreadyConsumedSlots(){
        val (_,a)=fixture()
        assertEquals("daily:2026-11-27",LocalDreamSchedule.candidate(a,emptySet(),at)?.key)
        assertNull(LocalDreamSchedule.candidate(a,setOf("daily:2026-11-27"),at))
        assertNull(LocalDreamSchedule.candidate(a.copy(config=cfg.copy(quietHours=DreamQuietHours("08:00","10:00"))),emptySet(),at))
        assertNull(LocalDreamSchedule.candidate(a.copy(armedAt=at+60000),emptySet(),at+60000))
        val extra=a.copy(extraAt=at+3600000,extraId="prior")
        assertEquals("extra:prior",LocalDreamSchedule.candidate(extra,setOf("daily:2026-11-27"),at+3600000)?.key)
        val lease=LocalDreamRuntime.acquire()!!
        try{assertNull(LocalDreamRuntime.acquire())}finally{lease.close()}
        LocalDreamRuntime.acquire()!!.close()
    }
    @Test fun ordinaryPermissionsAndWakePermissionBothRequiredForLibraryTools()=runBlocking{
        val (store,initial)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowLibrary=true,allowLibraryWrite=true,includeCustom=true,tokenBudget=22000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        val book=LocalSideRecords(store).importBook(buildJsonObject{put("title","wake book");put("format","txt");putJsonArray("chapters"){add(buildJsonObject{put("title","one");put("body","real page fixture")})}}).getValue("id").jsonPrimitive.content
        val claim=ledger.claim(cid,settings.revision!!,model,at)!!
        assertNull(LocalToolSession.openAutonomous(store,claim))
        cache.putHistory(cid,cache.history(cid)!!.copy(contextSettings=com.elementeracoast.app.core.model.ConversationContextSettings(localModelToolsEnabled=true,localModelWriteEnabled=true)))
        val session=LocalToolSession.openAutonomous(store,claim)!!
        val names=session.completionsTools.map{it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content}
        assertTrue("library_read" in names&&"library_write" in names)
        assertFalse("memory_search" in names||"read_mystic_dogtalk" in names)
        val page=session.execute("read","library_read",buildJsonObject{put("book_id",book);put("chapter_index",0)}.toString())
        assertTrue(page.output,page.ok);assertTrue(page.output.contains("real page fixture"))
        val args=buildJsonObject{put("action","create");put("book_id",book);put("kind","reflection");put("body","independent note")}.toString()
        // Even a permitted tool cannot bypass the actual activity reservation.
        assertFalse(session.execute("unreserved-write","library_write",args).ok)
        assertTrue(ledger.reserveStep(claim,0,at))
        assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
        assertTrue(ledger.reserveTool(claim,0,0,"library_write",at))
        val write=session.execute(claim.id+":0:0","library_write",args)
        assertTrue(write.output,write.ok)
        assertTrue(ledger.recordTool(claim,0,0,true,null,at,Json.parseToJsonElement(write.output).jsonObject.getValue("id").jsonPrimitive.content))
        assertTrue(ledger.reserveTool(claim,0,1,"library_write",at))
        assertFalse(session.execute(claim.id+":0:1","library_write",buildJsonObject{put("action","delete");put("id",Json.parseToJsonElement(write.output).jsonObject.getValue("id"))}.toString()).ok)
        assertTrue(ledger.recordTool(claim,0,1,false,null,at))
        val note=LocalSideRecords(store).rows().single{it.kind=="library_notes"}.data
        assertEquals(JsonNull,note["source_turn_id"]);assertEquals("assistant",note["author"]!!.jsonPrimitive.content)
        assertTrue(LocalToolAudit(store).read().isEmpty())
        ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"disarm"),false,at)
        assertFalse(session.execute("revoked","library_read",buildJsonObject{put("book_id",book);put("chapter_index",0)}.toString()).ok)
        ledger.finish(claim,"cancelled",buildJsonObject{},null,at)
        assertEquals(1,cache.history(cid)!!.turns.size)
    }

    @Test fun ordinaryReadbackIsSeparatelyAuthorizedAndDoesNotWake()=runBlocking{
        val (store,a)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val claim=ledger.claim(cid,a.revision!!,model,at)!!
        assertTrue(ledger.note(claim,"private readback fixture",at))
        assertTrue(ledger.finish(claim,"completed",buildJsonObject{},null,at))
        ledger.configure(cid,DreamSettingsRequest(cid,a.revision,"disarm"),false,at)
        val history=cache.history(cid)!!
        val base=history.contextSettings.copy(localModelToolsEnabled=true)
        cache.putHistory(cid,history.copy(contextSettings=base))
        val desk=RemoteDeskSlip(contextBudget=RemoteDeskContextBudget(comfortCeiling=10000))
        fun session()=LocalToolSession.open(store,cid,"t",null,desk,model)!!
        assertFalse(session().completionsTools.toString().contains("dream_readback"))
        cache.putHistory(cid,history.copy(contextSettings=base.copy(dreamReadbackEnabled=true)))
        val reader=session()
        val result=reader.execute("readback","dream_readback","""{"kind":"notes"}""")
        assertTrue(result.output,result.ok);assertTrue(result.output.contains("private readback fixture"))
        assertFalse(reader.execute("cross","dream_readback","""{"kind":"notes","conversation_id":"other"}""").ok)
        cache.putHistory(cid,history.copy(contextSettings=base))
        assertFalse(reader.execute("revoked","dream_readback","""{"kind":"notes"}""").ok)
        assertFalse(ledger.settings(cid).enabled)
        assertEquals(1,LocalDreamArchive(store).export(cid)!!.getValue("runs").jsonArray.size)
    }

    @Test fun canonicalDailyAndCalendarCreationHasWakeProvenance()=runBlocking{
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowDailyWrite=true,allowCalendarWrite=true,tokenBudget=24000,toolBudget=5))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,tools,_->
            requests++
            if(requests==1){
                assertTrue(tools.toString().contains("create_diary"))
                assertFalse(tools.toString().contains("moment_comment"))
                response(call("d","create_diary",buildJsonObject{put("date","2026-11-27");put("text","fixture diary")}),
                    call("m","create_moment",buildJsonObject{put("date","2026-11-27");put("text","fixture moment")}),
                    call("c","calendar_create",buildJsonObject{put("date","2026-11-28");put("title","fixture calendar");put("kind","note")}))
            }else response()
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        assertEquals(2,requests)
        val diary=cache.dailyDiaries().single();val moment=cache.dailyMoments().single()
        val calendar=LocalCalendarStore(store).snapshot().single().jsonObject
        assertNull(diary.sourceTurnId);assertNull(moment.sourceTurnId)
        assertEquals(JsonNull,calendar["source_turn_id"])
        assertEquals(cid,calendar.getValue("conversation_id").jsonPrimitive.content)
        assertTrue(calendar.getValue("tool_call_id").jsonPrimitive.content.startsWith("dream:"))
        val restoredCalendar=LocalCalendarStore(MemoryLocalPersistence())
        restoredCalendar.restoreSnapshot(JsonArray(listOf(calendar)))
        assertEquals(calendar,restoredCalendar.snapshot().single())
        val invalid=JsonObject(calendar+("tool_call_id" to JsonPrimitive("ordinary-tool")))
        assertTrue(runCatching{restoredCalendar.restoreSnapshot(JsonArray(listOf(invalid)))}.isFailure)
        assertEquals(calendar,restoredCalendar.snapshot().single())
        assertEquals("myri",diary.author)
        assertEquals("myri",moment.author)
        assertTrue(diary.toolCallId.orEmpty().startsWith("dream:"))
        val events=LocalDreamArchive(store).export(cid)!!.getValue("tools").jsonArray
        assertEquals(listOf(diary.id,moment.id,calendar.getValue("id").jsonPrimitive.content),events.map{
            Json.parseToJsonElement(it.jsonObject.getValue("receipt_json").jsonPrimitive.content).jsonObject.getValue("record_id").jsonPrimitive.content
        })
        assertEquals(1,cache.history(cid)!!.turns.size)
    }


    @Test fun autonomousVoiceUsesRealOriginalAndClaimWithoutChatTurn()=runBlocking{
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowVoice=true,voiceLimit=2,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        val bytes=byteArrayOf(73,68,51,4,0,0,0,0,0,0,1,2)
        var generated=0
        val http=OkHttpClient.Builder().addInterceptor{chain->
            generated++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("ok").body(bytes.toResponseBody()).build()
        }.build()
        val speech=LocalVoiceProvider({LocalProviderConfig("elevenlabs","ElevenLabs","https://api.elevenlabs.io","fixture-only",listOf("fixtureVoice"))},http)
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,tools,_->
            requests++
            if(requests==1){
                assertTrue(tools.toString().contains("voice_create"))
                response(call("v","voice_create",buildJsonObject{put("text","independent speech")}))
            }else response()
        },clock={at},voice=speech)
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        assertEquals(2,requests);assertEquals(1,generated)
        val side=LocalSideRecords(store);val row=side.clips(cid).single()
        assertEquals("dream_tide",row.getValue("source").jsonPrimitive.content)
        assertEquals(JsonNull,row["turn_id"]);assertEquals(JsonNull,row["message_id"])
        val id=row.getValue("id").jsonPrimitive.content
        assertTrue(id.startsWith("voice_dream_"))
        assertArrayEquals(bytes,side.bytes(side.audio(id)!!))
        assertEquals(1,cache.history(cid)!!.turns.size)
        val event=LocalDreamArchive(store).export(cid)!!.getValue("tools").jsonArray.single().jsonObject
        assertEquals(id,Json.parseToJsonElement(event.getValue("receipt_json").jsonPrimitive.content).jsonObject.getValue("record_id").jsonPrimitive.content)
    }

    @Test fun autonomousVoiceRequiresClaimAndRefusesSaveAfterRevocation(){
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowVoice=true,voiceLimit=2,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,settings.revision!!,model,at)!!
        var generated=0
        val bytes=byteArrayOf(73,68,51,4,0,0,0,0,0,0,1,2)
        val http=OkHttpClient.Builder().addInterceptor{chain->
            generated++
            assertNotNull(ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"disarm"),false,at))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("ok").body(bytes.toResponseBody()).build()
        }.build()
        val speech=LocalVoiceProvider({LocalProviderConfig("elevenlabs","ElevenLabs","https://api.elevenlabs.io","fixture-only",listOf("fixtureVoice"))},http)
        val session=LocalToolSession.openAutonomous(store,claim,voice=speech)!!
        val args=buildJsonObject{put("text","revoked speech")}.toString()
        assertFalse(session.execute("unclaimed","voice_create",args).ok);assertEquals(0,generated)
        assertTrue(ledger.reserveStep(claim,0,at))
        assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
        assertTrue(ledger.reserveTool(claim,0,0,"voice_create",at))
        assertFalse(session.execute(claim.id+":0:0","voice_create",args).ok)
        assertEquals(1,generated);assertTrue(LocalSideRecords(store).clips(cid).isEmpty())
    }


    @Test fun independentImageOriginalKeepsActivityIdentityAndCannotBeReboundByOrdinaryChat(){
        val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowImage=true,allowMessage=true,imageLimit=2,toolBudget=10,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
        val imageConfig=LocalProviderConfig("image-fixture","fixture","https://openrouter.ai/api/v1","fixture-only",listOf("fixture/paid"))
        val selected=imageConfig.modelId("fixture/paid")
        cache.putProfile(RemoteProfile(currentImageModel=selected))
        val claim=ledger.claim(cid,settings.revision!!,model,at)!!
        assertTrue(ledger.reserveStep(claim,0,at))
        assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
        assertTrue(ledger.reserveTool(claim,0,0,"image_generate",at))
        val bytes=ByteArray(24);byteArrayOf(137.toByte(),80,78,71,13,10,26,10).copyInto(bytes)
        var calls=0
        val generator=LocalDreamImage(store,claim,selected,{imageConfig},
            discover={listOf(LocalImageProvider.Model("fixture/paid","fixture",false))},
            generate={_,_->calls++;LocalImageProvider.Generated(bytes,"image/png","openrouter","fixture/paid",null,null,"fixture")})
        val result=generator.execute(claim.id+":0:0",buildJsonObject{put("prompt","fixture")})
        val original=LocalAttachmentStore(store).entries().single()
        assertEquals("att_dream_"+claim.id+"_0_0",result.getValue("id").jsonPrimitive.content)
        assertEquals(JsonNull,original.metadata["turn_id"])
        assertArrayEquals(bytes,LocalAttachmentStore(store).bytes(original))
        assertEquals(1,cache.history(cid)!!.turns.size)
        assertTrue(runCatching{LocalAttachmentStore.validate(JsonObject(original.metadata+("turn_id" to JsonPrimitive("t"))))}.isFailure)
        assertTrue(runCatching{generator.execute(claim.id+":0:0",buildJsonObject{put("prompt","again")})}.isFailure)
        assertEquals(1,calls)
        val imageId=result.getValue("id").jsonPrimitive.content
        assertTrue(ledger.recordTool(claim,0,0,true,null,at,imageId))
        assertTrue(ledger.reserveTool(claim,0,1,"dream_send_message",at))
        assertNull(ledger.sendMessage(claim,0,1,"foreign",at,listOf("att_dream_foreign_0_0")))
        assertNull(ledger.sendMessage(claim,0,1,"",at))
        val sent=ledger.sendMessage(claim,0,1,"",at,listOf(imageId))
        assertNotNull(sent)
        assertTrue(ledger.recordTool(claim,0,1,true,null,at,sent))
        val turn=cache.history(cid)!!.turns.last()
        assertEquals("",turn.assistant.variantsByUserVariant.getValue("0").single().content)
        assertEquals(imageId,turn.assistant.variantsByUserVariant.getValue("0").single().attachments.single().id)
        val bound=LocalAttachmentStore(store).entries().single()
        assertEquals(turn.id,bound.metadata.getValue("turn_id").jsonPrimitive.content)
        assertArrayEquals(bytes,LocalAttachmentStore(store).bytes(bound))
        assertTrue(ledger.reserveTool(claim,0,2,"dream_send_message",at))
        assertNull(ledger.sendMessage(claim,0,2,"duplicate",at,listOf(imageId)))
        assertTrue(ledger.recordTool(claim,0,2,false,null,at))
        assertTrue(ledger.reserveTool(claim,0,3,"image_generate",at))
        val revoked=LocalDreamImage(store,claim,selected,{imageConfig},
            discover={listOf(LocalImageProvider.Model("fixture/paid","fixture",false))},
            generate={_,_->assertNotNull(ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"disarm"),false,at));LocalImageProvider.Generated(bytes,"image/png","openrouter","fixture/paid",null,null,"fixture")})
        assertTrue(runCatching{revoked.execute(claim.id+":0:3",buildJsonObject{put("prompt","revoked")})}.isFailure)
        assertEquals(1,LocalAttachmentStore(store).entries().size)
    }


    @Test fun autonomousMediaAndFavoritesSurviveCompleteNativeArchiveWithoutRearming(){
        val dir=java.nio.file.Files.createTempDirectory("dream-media-v2").toFile()
        try{
            val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
            val history=cache.history(cid)!!
            cache.putHistory(cid,history.copy(updatedAt=Instant.ofEpochMilli(at).toString(),contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
            val cfg2=cfg.copy(execution=cfg.execution!!.copy(allowImage=true,allowVoice=true,allowMemoryCandidate=true,tokenBudget=24000))
            val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",cfg2,model,true),true,at-86400000)!!
            val claim=ledger.claim(cid,settings.revision!!,model,at)!!
            assertTrue(ledger.reserveStep(claim,0,at));assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
            val pictureId="att_dream_"+claim.id+"_0_0";val voiceId="voice_dream_"+claim.id+"_0_1"
            val png=ByteArray(24);byteArrayOf(137.toByte(),80,78,71,13,10,26,10).copyInto(png)
            val mp3=byteArrayOf(73,68,51,4,0,0,0,0,0,0,1,2)
            val meta=buildJsonObject{put("id",pictureId);put("conversation_id",cid);put("turn_id",JsonNull);put("kind","image");put("name","自主活动图像.png");put("mime","image/png");put("size",png.size);put("created_at",at);put("updated_at",at)}
            assertTrue(ledger.reserveTool(claim,0,0,"image_generate",at))
            store.atomic{s->LocalAttachmentStore(s).publish(LocalAttachmentStore.Original(meta,ArchiveV2Chunks.digest(png)),png)}
            assertTrue(ledger.recordTool(claim,0,0,true,null,at,pictureId))
            assertTrue(ledger.reserveTool(claim,0,1,"voice_create",at))
            store.atomic{s->saveLocalVoice(s,cid,null,null,"dream_tide",model,LocalVoiceProvider.Generated("fixtureVoice","fixture speech",mp3),voiceId)}
            assertTrue(ledger.recordTool(claim,0,1,true,null,at,voiceId))
            assertTrue(ledger.reserveTool(claim,0,2,"dream_memory_candidate",at))
            val memory=LocalMemoryStore(store)
            val proposal=memory.proposeDreamCandidate(claim,0,2,buildJsonObject{put("title","archive candidate");put("content","candidate body");put("destination","seed")},at)
            val pocketId=proposal.getValue("id").jsonPrimitive.content
            assertTrue(ledger.recordTool(claim,0,2,true,null,at,pocketId))
            assertTrue(ledger.reserveTool(claim,0,3,"dream_write_note",at))
            assertTrue(ledger.note(claim,"archive note",at))
            assertTrue(ledger.recordTool(claim,0,3,true,null,at))
            assertTrue(ledger.finish(claim,"completed",buildJsonObject{},null,at))
            assertTrue(LocalDreamArchive(store).reply(cid,claim.id,"reply-archive","archive reply"))
            val candidate=memory.snapshot().pockets.single()
            assertTrue(runCatching{LocalDreamArchive(store).validateCandidateOrigin(null,candidate)}.isFailure)
            val side=LocalSideRecords(store)
            for((kind,id) in listOf("image" to pictureId,"audio" to voiceId)){
                val favorite=side.saveTreasure(buildJsonObject{put("kind",kind);put("conversation_id",cid);put("source_id",id);put("notes","fixture favorite")})
                assertEquals("assistant",favorite.getValue("role").jsonPrimitive.content)
                assertEquals(JsonNull,favorite["message_id"])
            }
            val app="com.elementeracoast.source";val zip=java.io.File(dir,"complete.zip")
            val manifest=ArchiveV2Local.export(store,app,zip)
            val stage=ArchiveV2Stage(dir,"verified");ArchiveV2ZipReader.stage(zip,stage)
            val dst=MemoryLocalPersistence();val selected=setOf("chat","attachments","voice","vault","memory_pockets")
            ArchiveV2Local.restore(manifest,stage,selected,app,dst,false)
            assertTrue(RemoteCacheStore(dst).conversations().isEmpty())
            ArchiveV2Local.restore(manifest,stage,selected,app,dst,true)
            assertEquals(LocalDreamArchive(store).export(cid),LocalDreamArchive(dst).export(cid))
            assertFalse(LocalDreamLedger(dst).settings(cid).enabled)
            assertEquals(1,RemoteCacheStore(dst).history(cid)!!.turns.size)
            val originals=LocalAttachmentStore(dst);assertArrayEquals(png,originals.bytes(originals.entries().single()))
            val restored=LocalSideRecords(dst);assertArrayEquals(mp3,restored.bytes(restored.audio(voiceId)!!))
            assertEquals(side.rows(),restored.rows())
            assertEquals(listOf(candidate),LocalMemoryStore(dst).snapshot().pockets)
            assertTrue(LocalMemoryStore(dst).snapshot().entries.isEmpty())
            // Exercise backend-shaped input through the real cloud converter.
            val desc=manifest.objects.associateBy{it.objectSha256}
            val replacements=mutableMapOf<String,Pair<ArchiveV2Object,ByteArray>>()
            val cloudRecords=manifest.records.map{record->
                if(record.module!="chat")record else{
                    val raw=Json.parseToJsonElement(stage.verify(desc.getValue(record.recordObject),1800000).readBytes().decodeToString()).jsonObject.getValue("data").jsonObject
                    val c=raw.getValue("conversation").jsonObject
                    fun millis(key:String):JsonElement=c[key]?.takeIf{it!=JsonNull}?.let{JsonPrimitive(Instant.parse(it.jsonPrimitive.content).toEpochMilli())}?:JsonNull
                    val cloudC=buildJsonObject{
                        for(key in listOf("id","title","room_type"))put(key,c.getValue(key))
                        for(key in listOf("created_at","updated_at","title_generated_at"))put(key,millis(key))
                        put("deleted_at",JsonNull);put("title_manual",if(c["title_manual"]?.jsonPrimitive?.booleanOrNull==true)1 else 0)
                        put("title_model_id",c["title_model_id"]?:JsonNull);put("source",c["source"]?:JsonPrimitive("coast"));put("source_window_id",c["source_window_id"]?:JsonNull)
                    }
                    val h=raw.getValue("history").jsonObject
                    val storage=h["archive_storage_updated_at"]?.jsonPrimitive?.longOrNull?:Instant.parse(h.getValue("updated_at").jsonPrimitive.content).toEpochMilli()
                    val cloudRaw=buildJsonObject{put("kind","chat");putJsonObject("data"){
                        put("conversation",cloudC);put("history_json",JsonObject(h.filterKeys{it!="archive_storage_updated_at"}).toString());put("history_updated_at",storage)
                        raw["dream_tide"]?.let{put("dream_tide",it)}
                    }}.toString().encodeToByteArray()
                    val hashes=(cloudRaw.indices step ArchiveV2Chunks.CHUNK_BYTES).map{ArchiveV2Chunks.digest(cloudRaw.copyOfRange(it,minOf(it+ArchiveV2Chunks.CHUNK_BYTES,cloudRaw.size)))}
                    val d=ArchiveV2Object(cloudRaw.size.toLong(),hashes,ArchiveV2Chunks.fingerprint(cloudRaw.size.toLong(),hashes))
                    replacements[record.recordObject]=d to cloudRaw
                    record.copy(recordObject=d.objectSha256)
                }
            }
            val cloudManifest=manifest.copy(workspace="backend",records=cloudRecords,objects=manifest.objects.map{replacements[it.objectSha256]?.first?:it})
            val cloudZip=java.io.File(dir,"backend.zip")
            ArchiveV2ZipWriter(cloudZip.outputStream()).use{writer->
                for(d in manifest.objects){
                    val changed=replacements[d.objectSha256]
                    if(changed!=null)java.io.ByteArrayInputStream(changed.second).use{writer.objectEntry(changed.first,it)}
                    else stage.verify(d,CoastTransferCodec.MAX_BYTES.toLong()).inputStream().use{writer.objectEntry(d,it)}
                }
                writer.finish(cloudManifest)
            }
            val cloudStage=ArchiveV2Stage(dir,"cloud");ArchiveV2ZipReader.stage(cloudZip,cloudStage)
            val fromCloud=MemoryLocalPersistence()
            ArchiveV2Local.restore(cloudManifest,cloudStage,selected,app,fromCloud,false)
            assertTrue(RemoteCacheStore(fromCloud).conversations().isEmpty())
            ArchiveV2Local.restore(cloudManifest,cloudStage,selected,app,fromCloud,true)
            assertEquals(LocalDreamArchive(store).export(cid),LocalDreamArchive(fromCloud).export(cid))
            assertFalse(LocalDreamLedger(fromCloud).settings(cid).enabled)
            assertEquals(listOf(candidate),LocalMemoryStore(fromCloud).snapshot().pockets)
            assertEquals(side.rows(),LocalSideRecords(fromCloud).rows())
            val cloudOriginals=LocalAttachmentStore(fromCloud)
            assertArrayEquals(png,cloudOriginals.bytes(cloudOriginals.entries().single()))
            assertArrayEquals(mp3,LocalSideRecords(fromCloud).bytes(LocalSideRecords(fromCloud).audio(voiceId)!!))
            assertEquals(1,RemoteCacheStore(fromCloud).history(cid)!!.turns.size)
            val archive=LocalDreamArchive(dst)
            for(id in listOf(pictureId,voiceId))assertEquals(JsonPrimitive(claim.id),archive.activity(cid,0,id).getValue("items").jsonArray.single().jsonObject["claim_id"])
            assertTrue(archive.activity(cid,0,pictureId+"_forged").getValue("items").jsonArray.isEmpty())
            assertTrue(archive.activity("other",0,voiceId).getValue("items").jsonArray.isEmpty())
        }finally{dir.deleteRecursively()}
    }


    @Test fun mediaReservationBudgetsAreIndependentAndZeroDisables(){
        val (store,initial)=fixture();val ledger=LocalDreamLedger(store)
        val config=cfg.copy(execution=cfg.execution!!.copy(allowVoice=true,allowImage=true,voiceLimit=2,imageLimit=0))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",config,model,true),true,at-86400000)!!
        val claim=ledger.claim(cid,settings.revision!!,model,at)!!
        assertTrue(ledger.reserveStep(claim,0,at));assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},at))
        assertFalse(ledger.reserveTool(claim,0,0,"image_generate",at))
        assertTrue(ledger.reserveTool(claim,0,1,"voice_create",at))
        assertTrue(ledger.reserveTool(claim,0,2,"voice_create",at))
        assertFalse(ledger.reserveTool(claim,0,3,"voice_create",at))
        assertTrue(ledger.reserveTool(claim,0,4,"dream_write_note",at))
    }


    @Test fun dreamMemoryCandidateStaysPendingAndDoesNotReviveDiscardedCandidate()=runBlocking{
        val (store,initial)=fixture();val ledger=LocalDreamLedger(store);val cache=RemoteCacheStore(store)
        val history=cache.history(cid)!!
        cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
        val config=cfg.copy(execution=cfg.execution!!.copy(allowMemoryCandidate=true,tokenBudget=24000))
        val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",config,model,true),true,at-86400000)!!
        val input=buildJsonObject{put("title","一颗候选");put("content","fixture memory candidate");put("destination","memory")}
        var requests=0
        val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,_,tools,_->
            requests++
            if(requests==1){
                assertTrue(tools.toString().contains("dream_memory_candidate"))
                response(call("m1","dream_memory_candidate",input),call("m2","dream_memory_candidate",input))
            }else response()
        },clock={at})
        assertEquals("completed",engine.run(cid,settings.revision!!,model,at))
        val memory=LocalMemoryStore(store);val candidate=memory.snapshot().pockets.single()
        assertEquals(JsonPrimitive("dream_tide"),candidate["source_type"])
        assertEquals(JsonPrimitive("pending"),candidate["status"])
        assertTrue(memory.snapshot().entries.isEmpty())
        assertEquals(1,cache.history(cid)!!.turns.size)
        val id=candidate.getValue("id").jsonPrimitive.content
        memory.resolve(id,RemoteMemoryPocketResolveRequest(action="discard"))
        val later=at+86400000
        val claim=ledger.claim(cid,settings.revision!!,model,later)!!
        assertTrue(ledger.reserveStep(claim,0,later));assertTrue(ledger.recordStep(claim,0,true,buildJsonObject{},later))
        assertTrue(ledger.reserveTool(claim,0,0,"dream_memory_candidate",later))
        assertEquals(JsonPrimitive("duplicate_suppressed"),memory.proposeDreamCandidate(claim,0,0,input,later)["status"])
        assertTrue(memory.pockets(cid).isEmpty())
        assertNotNull(ledger.configure(cid,DreamSettingsRequest(cid,settings.revision,"disarm"),false,later))
        assertTrue(runCatching{memory.proposeDreamCandidate(claim,0,0,JsonObject(input+("content" to JsonPrimitive("revoked"))),later)}.isFailure)
        assertEquals(1,memory.snapshot().pockets.size)
    }


    @Test fun autonomousImagePixelsAreOneShotOptInAndBudgeted()=runBlocking{
        for((permission,vision,budget) in listOf(Triple(true,true,24000),Triple(false,true,24000),Triple(true,false,24000),Triple(true,null,24000),Triple(true,true,1800))){
            val (store,initial)=fixture();val cache=RemoteCacheStore(store);val ledger=LocalDreamLedger(store)
            val history=cache.history(cid)!!
            cache.putHistory(cid,history.copy(contextSettings=history.contextSettings.copy(localModelWriteEnabled=true)))
            val config=cfg.copy(execution=cfg.execution!!.copy(allowImage=true,allowImageReadback=permission,tokenBudget=budget))
            val settings=ledger.configure(cid,DreamSettingsRequest(cid,initial.revision,"arm",config,model,true),true,at-86400000)!!
            val imageConfig=LocalProviderConfig("image-fixture","fixture","https://openrouter.ai/api/v1","fixture-only",listOf("fixture/paid"))
            cache.putProfile(RemoteProfile(currentImageModel=imageConfig.modelId("fixture/paid")))
            val bytes=ByteArray(24);byteArrayOf(137.toByte(),80,78,71,13,10,26,10).copyInto(bytes)
            var requests=0;var seen=0
            val engine=LocalDreamExecutor(store,{provider to "fixture/model"},{_,_,messages,_,_->
                requests++
                val parts=messages.flatMap{m->(m.jsonObject["content"] as? JsonArray).orEmpty()}.filter{it.jsonObject["type"]==JsonPrimitive("image_url")}
                if(parts.isNotEmpty()){
                    seen++;assertEquals(1,parts.size)
                    val url=parts.single().jsonObject.getValue("image_url").jsonObject.getValue("url").jsonPrimitive.content
                    assertArrayEquals(bytes,java.util.Base64.getDecoder().decode(url.substringAfter(',')))
                }
                when(requests){
                    1->response(call("i","image_generate",buildJsonObject{put("prompt","fixture")}))
                    2->response(call("n","dream_write_note",buildJsonObject{put("body","fixture note")}))
                    else->response()
                }
            },clock={at},imageProvider={imageConfig},modelBudget={_,_,_->LocalInputBudget(18000,null,100000,"fixture",vision)},
                imageFactory={s,c,id,resolver->LocalDreamImage(s,c,id,resolver,
                    discover={listOf(LocalImageProvider.Model("fixture/paid","fixture",false))},
                    generate={_,_->LocalImageProvider.Generated(bytes,"image/png","openrouter","fixture/paid",null,null,"fixture")})})
            val status=engine.run(cid,settings.revision!!,model,at)
            if(budget==24000)assertEquals("completed",status)
            val expected=permission&&vision==true&&budget==24000
            assertEquals(if(expected)1 else 0,seen)
            val archive=LocalDreamArchive(store).export(cid)!!.toString()
            assertEquals(expected,archive.contains("image_readback_accepted"))
            assertFalse(archive.contains("data:image/"));assertEquals(1,cache.history(cid)!!.turns.size)
        }
    }


    @Test fun planRouteRunsSameDurableToolsWithNoApiFallbackOrHumanTurn()=runBlocking{
        val (store,initial)=fixture();val ledger=LocalDreamLedger(store)
        val m="chatgpt-plan:fixture-model";val rev="plan-revision"
        val request=DreamSettingsRequest(cid,initial.revision,"arm",cfg,m,true,confirmPlanSoftBudget=true,planRequestLimit=3)
        assertNull(ledger.configure(cid,request.copy(confirmPlanSoftBudget=false),true,at-86400000,planRevision=rev))
        assertNull(ledger.configure(cid,request,true,at-86400000))
        val armed=ledger.configure(cid,request,true,at-86400000,planRevision=rev)!!
        assertEquals(rev,armed.revision)
        val grant=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanProtocol.Grant(cid,rev,"account","client","fixture-model",true,3)
        var calls=0
        val binding=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanBinding(grant,{true}){body,tools,authorize->
            assertTrue(authorize());calls++
            assertEquals(JsonPrimitive(false),body["store"]);assertFalse("max_output_tokens" in body)
            assertTrue(ledger.active(cid))
            val history=LocalDreamArchive(store).runtimeHistory(cid)
            assertTrue(history.getValue("steps").jsonArray.any{it.jsonObject["step_index"]==JsonPrimitive(calls-1)&&it.jsonObject["status"]==JsonPrimitive("claimed")})
            val result=if(calls==1)"""{"status":"completed","model":"fixture-model","usage":{"total_tokens":120},"output":[{"type":"function_call","namespace":"coast","name":"dream_write_note","call_id":"n","arguments":"{\"body\":\"套餐手记\"}"}]}"""
            else{
                assertTrue(body.getValue("input").jsonArray.any{it.jsonObject["type"]==JsonPrimitive("function_call_output")&&it.jsonObject["call_id"]==JsonPrimitive("n")})
                """{"status":"completed","model":"fixture-model","usage":{"total_tokens":120},"output":[]}"""
            }
            com.elementeracoast.app.feature.chatgpt.NativeDreamPlanProtocol.complete(Json.parseToJsonElement(result).jsonObject,emptyMap(),tools)
        }
        val engine=LocalDreamExecutor(store,{error("API fallback prohibited")},{_,_,_,_,_->error("API fallback prohibited")},clock={at},preparePlan={w,r,id->assertEquals(cid,w);assertEquals(rev,r);assertEquals(m,id);binding})
        assertEquals("completed",engine.run(cid,rev,m,at));assertEquals(2,calls)
        val archive=LocalDreamArchive(store)
        assertEquals("套餐手记",archive.notes(cid,0).getValue("items").jsonArray.single().jsonObject["body"]!!.jsonPrimitive.content)
        val row=archive.activity(cid,0).getValue("items").jsonArray.single().jsonObject
        assertEquals(JsonPrimitive(240),row.getValue("result").jsonObject["total_tokens"])
        assertEquals(JsonNull,row.getValue("result").jsonObject["reported_cost"])
        assertEquals(1,RemoteCacheStore(store).history(cid)!!.turns.size)
        assertEquals("not_due",engine.run(cid,rev,m,at));assertEquals(2,calls)
    }
    @Test fun planWakeSoilConsumesOnlyRemainingGrantRequest()=runBlocking{
        for(limit in listOf(2,3)){
            val (store,initial)=fixture();val ledger=LocalDreamLedger(store)
            LocalSoilStore(store).patch(cid,buildJsonObject{put("auto_refresh_enabled",true)})
            val m="chatgpt-plan:fixture-model";val rev="plan-soil-"+limit
            val updated=cfg.copy(execution=cfg.execution!!.copy(allowMessage=true,tokenBudget=22000,toolBudget=5))
            val request=DreamSettingsRequest(cid,initial.revision,"arm",updated,m,true,confirmPlanSoftBudget=true,planRequestLimit=limit)
            assertNotNull(ledger.configure(cid,request,true,at-86400000,planRevision=rev))
            val grant=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanProtocol.Grant(cid,rev,"account","client","fixture-model",true,limit)
            var calls=0
            val binding=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanBinding(grant,{true}){body,tools,authorize->
                assertTrue(authorize());calls++
                val output=when(calls){
                    1->buildJsonObject{put("type","function_call");put("namespace","coast");put("name","dream_send_message");put("call_id","sent");put("arguments",buildJsonObject{put("body","real plan wake")}.toString())}
                    2->null
                    else->{
                        assertTrue(tools.isEmpty());assertTrue(body.toString().contains("real plan wake"))
                        buildJsonObject{put("type","message");put("role","assistant");putJsonArray("content"){add(buildJsonObject{put("type","output_text");put("text","""{"current_text":"plan soil","afterthoughts":[]}""")})}}
                    }
                }
                val raw=buildJsonObject{put("status","completed");put("model","fixture-model");putJsonObject("usage"){put("total_tokens",120)};put("output",JsonArray(listOfNotNull(output)))}
                com.elementeracoast.app.feature.chatgpt.NativeDreamPlanProtocol.complete(raw,emptyMap(),tools)
            }
            val engine=LocalDreamExecutor(store,{error("API fallback prohibited")},{_,_,_,_,_->error("API fallback prohibited")},clock={at},preparePlan={_,_,_->binding})
            assertEquals("completed",engine.run(cid,rev,m,at));assertEquals(limit,calls)
            val message=RemoteCacheStore(store).history(cid)!!.turns.last().assistant.variantsByUserVariant.getValue("0").single()
            assertEquals("real plan wake",message.content)
            val result=LocalDreamArchive(store).activity(cid,0).getValue("items").jsonArray.single().jsonObject.getValue("result").jsonObject
            assertEquals(JsonPrimitive(limit*120),result["total_tokens"])
            val soil=LocalSoilStore(store).read()
            if(limit==3){assertEquals(message.id,soil.history.last().getValue("assistant_message_id").jsonPrimitive.content);assertEquals("plan soil",soil.current.single().getValue("current_text").jsonPrimitive.content)}
            else assertTrue(soil.history.isEmpty())
        }
    }
    @Test fun planQuotaFailureIsTerminalAndCannotSwitchToApiKey()=runBlocking{
        val (store,initial)=fixture();val ledger=LocalDreamLedger(store)
        val m="chatgpt-plan:fixture-model";val rev="plan-quota"
        val request=DreamSettingsRequest(cid,initial.revision,"arm",cfg,m,true,confirmPlanSoftBudget=true,planRequestLimit=2)
        assertNotNull(ledger.configure(cid,request,true,at-86400000,planRevision=rev))
        val grant=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanProtocol.Grant(cid,rev,"account","client","fixture-model",true,2)
        var calls=0;var authorized=true
        val binding=com.elementeracoast.app.feature.chatgpt.NativeDreamPlanBinding(grant,{authorized}){_,_,_->calls++;authorized=false;throw com.elementeracoast.app.feature.chatgpt.NativeDreamPlanFailure("plan_usage_limit")}
        val engine=LocalDreamExecutor(store,{error("must not fallback")},{_,_,_,_,_->error("must not fallback")},clock={at},preparePlan={_,_,_->binding.takeIf{authorized}})
        assertEquals("failed",engine.run(cid,rev,m,at));assertEquals(1,calls)
        assertEquals("model_unavailable",engine.run(cid,rev,m,at+86400000));assertEquals(1,calls)
        val row=LocalDreamArchive(store).activity(cid,0).getValue("items").jsonArray.single().jsonObject
        assertEquals(JsonPrimitive("plan_usage_limit"),row.getValue("result").jsonObject["reason"])
        assertEquals(JsonNull,row.getValue("result").jsonObject["total_tokens"])
        assertEquals(1,RemoteCacheStore(store).history(cid)!!.turns.size)
    }

}
