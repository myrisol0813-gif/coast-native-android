package com.elementeracoast.app.core.local

import android.content.Context
import androidx.work.*
import com.elementeracoast.app.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

/** Device scheduling for the online workspace, without a cloud cron or
 * cloud-held ChatGPT credential. A server claim is always required. */
class OnlineDreamWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        val context=applicationContext
        if(!BuildConfig.LOCAL_DREAM_PLAN_EXECUTION_ENABLED||NativeWorkspace.localSelected(context))return@withContext Result.success()
        val lease=LocalDreamRuntime.acquire()?:return@withContext Result.success()
        try{
            val device=OnlineDreamDevice.get(context)
            val api=device.api()
            for(r in device.registrations()){
                ensureActive()
                if(NativeWorkspace.localSelected(context))break
                try{if(device.execute(api,r))break}
                catch(e:CancellationException){throw e}
                catch(e:Exception){device.recordCheckFailure(r,e);continue}
            }
            Result.success()
        }catch(e:CancellationException){throw e}
        catch(_:Exception){Result.failure()}
        finally{lease.close()}
    }
    companion object{
        private const val JOB="coast-online-device-dream"
        fun sync(context:Context){
            val app=context.applicationContext
            val manager=WorkManager.getInstance(app)
            if(!BuildConfig.LOCAL_DREAM_PLAN_EXECUTION_ENABLED||NativeWorkspace.localSelected(app)){manager.cancelUniqueWork(JOB);return}
            // A storage read failure is not evidence that the owner removed
            // all registrations. Keep the existing worker instead of cancelling
            // an admitted activity from an unrelated UI refresh.
            val enabled=try{OnlineDreamDevice.get(app).registrations().isNotEmpty()}catch(_:Exception){return}
            if(!enabled){manager.cancelUniqueWork(JOB);return}
            val request=PeriodicWorkRequestBuilder<OnlineDreamWorker>(15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            manager.enqueueUniquePeriodicWork(JOB,ExistingPeriodicWorkPolicy.KEEP,request)
        }
    }
}
