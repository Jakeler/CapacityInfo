package com.ph03nix_x.capacityinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ph03nix_x.capacityinfo.services.CapacityInfoService

class PluggedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        when(intent?.action) {

            Intent.ACTION_POWER_CONNECTED -> context?.stopService(Intent(context, CapacityInfoService::class.java))
        }
    }
}