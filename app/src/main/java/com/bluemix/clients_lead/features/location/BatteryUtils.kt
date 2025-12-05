package com.bluemix.clients_lead.features.location

import android.content.Context
import android.os.BatteryManager


object BatteryUtils {
    fun getBatteryPercentage(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
