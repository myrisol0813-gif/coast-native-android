package com.elementeracoast.app.core.local

import android.content.Context
import androidx.work.*
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.auth.AndroidKeystoreAuthStore
import com.elementeracoast.app.core.network.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

/** Best-effort Android background delivery of already committed online
 * autonomous chat. No model call, wake claim, local workspace access or
 * generation lease. A fresh subscription never replays old messages. */
class OnlineArrivalWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        val c=applicationContext
        val auth=AndroidKeystoreAuthStore(c)
        val session=auth.load()?.takeUnless{it.isExpired()}?:return@withContext Result.success()
        fun active()=!NativeWorkspace.localSelected(c)&&subscribed(c)&&auth.load()?.cookieHeader==session.cookieHeader
        if(!active())return@withContext Result.success()
        val prefs=prefs(c)
        val cfg=CoastApiConfig(BuildConfig.COAST_API_BASE_URL)
        val api=CoastApiClient(cfg,CoastHttpClient(cfg,auth).client)
        try{
            var cursor=prefs.getLong(CURSOR,-1).takeIf{it>=0}
            // One page at a time; backlog is continued, never truncated.
            repeat(4){
                ensureActive()
                if(!active())return@withContext Result.success()
                val page=api.dreamArrivals(cursor)
                check(page.ok&&page.cursor>=0&&page.items.size<=50)
                check(page.reset||cursor==null||page.cursor>=cursor!!)
                if(!active())return@withContext Result.success()
                if(cursor!=null&&!page.reset){
                    var seen=cursor!!
                    val histories=mutableMapOf<String,com.elementeracoast.app.core.remote.RemoteHistory?>()
                    for(item in page.items){
                        ensureActive()
                        if(!active())return@withContext Result.success()
                        check(item.seq>seen&&item.seq<=page.cursor)
                        val cid=item.conversationId
                        when(item.kind){
                            "message"->if(CoastMessageNotifications.enabled(c)){
                                if(!histories.containsKey(cid))histories[cid]=try{api.getHistory(cid)}catch(e:CoastApiException){if(e.status==404)null else throw e}
                                val variant=histories[cid]?.turns?.firstOrNull{it.id==item.turnId&&it.turnType=="dream_tide"}?.assistant?.variantsByUserVariant?.values?.flatten()?.firstOrNull{it.id==item.messageId&&it.generationSource=="dream_tide"}
                                if(!active())return@withContext Result.success()
                                if(variant!=null)CoastMessageNotifications.postWake(c,cid,variant,false)
                            }
                            "note","start","failure"->if(LocalDreamNotifications.enabled(c,item.kind)){
                                val body=if(item.kind=="note"&&CoastMessageNotifications.preview(c)!="none")api.dreamNoticeBody(item.messageId) else null
                                if(!active())return@withContext Result.success()
                                LocalDreamNotifications.post(c,cid,item.turnId,item.kind,body,localMode=false)
                            }
                        }
                        check(prefs.edit().putLong(CURSOR,item.seq).commit())
                        seen=item.seq
                    }
                }
                if(!active())return@withContext Result.success()
                check(prefs.edit().putLong(CURSOR,page.cursor).commit())
                cursor=page.cursor
                if(!page.hasMore)return@withContext Result.success()
            }
            Result.success()
        }catch(e:CancellationException){throw e}
        catch(e:CoastApiException){if(e.status==401||e.status==403||e.status==404)Result.success() else Result.retry()}
        catch(_:Exception){Result.retry()}
    }
    companion object{
        private const val JOB="coast-online-arrival-notices"
        private const val CURSOR="cursor"
        private fun prefs(c:Context)=c.getSharedPreferences("coast_online_arrivals_v1",Context.MODE_PRIVATE)
        private fun subscribed(c:Context)=CoastMessageNotifications.enabled(c)||listOf(LocalDreamNotifications.NOTE,LocalDreamNotifications.START,LocalDreamNotifications.FAILURE).any{LocalDreamNotifications.enabled(c,it)}
        fun sync(context:Context){
            val c=context.applicationContext
            val manager=WorkManager.getInstance(c)
            val session=AndroidKeystoreAuthStore(c).load()?.takeUnless{it.isExpired()}
            if(NativeWorkspace.localSelected(c)||!subscribed(c)||session==null){
                manager.cancelUniqueWork(JOB)
                prefs(c).edit().remove(CURSOR).apply()
                return
            }
            val request=PeriodicWorkRequestBuilder<OnlineArrivalWorker>(15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            manager.enqueueUniquePeriodicWork(JOB,ExistingPeriodicWorkPolicy.KEEP,request)
        }
    }
}
