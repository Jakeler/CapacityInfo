package com.ph03nix_x.capacityinfo.helpers

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.preference.Preference
import com.ph03nix_x.capacityinfo.services.CapacityInfoService
import com.ph03nix_x.capacityinfo.services.OverlayService
import kotlinx.coroutines.*

object ServiceHelper {

    private var isStartedCapacityInfoService = false
    private var isStartedOverlayService = false

    fun startService(context: Context, serviceName: Class<*>,
                     isStartOverlayServiceFromSettings: Boolean = false) {

        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {

            try {

                if(serviceName == CapacityInfoService::class.java) {

                    isStartedCapacityInfoService = true

                    delay(2500L)
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startForegroundService(Intent(context, serviceName))
                    else context.startService(Intent(context, serviceName))

                    delay(1000L)
                    isStartedCapacityInfoService = false
                }

                else if(serviceName == OverlayService::class.java) {

                    isStartedOverlayService = true

                    if(!isStartOverlayServiceFromSettings) delay(3600L)

                    context.startService(Intent(context, serviceName))
                    isStartedCapacityInfoService = false
                }
            }
            catch(e: Exception) {

                Toast.makeText(context, e.message ?: e.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun isStartedCapacityInfoService() = isStartedCapacityInfoService

    fun isStartedOverlayService() = isStartedOverlayService

    fun stopService(context: Context, serviceName: Class<*>) =
        context.stopService(Intent(context, serviceName))

    fun restartService(context: Context, serviceName: Class<*>, preference: Preference? = null) {

        CoroutineScope(Dispatchers.Default).launch {

            withContext(Dispatchers.Main) {

                stopService(context, serviceName)

                if(serviceName == CapacityInfoService::class.java) delay(2500L)

                startService(context, serviceName)

                delay(1000L)
                preference?.isEnabled = true
            }
        }
    }

    fun jobSchedule(context: Context, jobName: Class<*>, jobId: Int, periodic: Long) {

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler

        val serviceComponent = ComponentName(context, jobName)

        val jobInfo = JobInfo.Builder(jobId, serviceComponent).apply {

            setPeriodic(periodic)

        }.build()

        if(!isJobSchedule(context, jobId)) jobScheduler?.schedule(jobInfo)
    }

    private fun isJobSchedule(context: Context, jobId: Int): Boolean {

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler

        jobScheduler?.allPendingJobs?.forEach {

            if(it.id == jobId) return true
        }

        return false
    }

    fun cancelJob(context: Context, jobId: Int) {

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler

        if(isJobSchedule(context, jobId)) jobScheduler?.cancel(jobId)
    }

    fun cancelAllJobs(context: Context) {

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler

        if(jobScheduler?.allPendingJobs?.isNotEmpty() == true) jobScheduler.cancelAll()
    }

    fun rescheduleJob(context: Context, jobName: Class<*>, jobId: Int, periodic: Long) {

        cancelJob(context, jobId)

        jobSchedule(context, jobName, jobId, periodic)
    }
}