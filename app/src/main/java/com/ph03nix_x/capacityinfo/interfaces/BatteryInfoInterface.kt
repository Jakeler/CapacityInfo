package com.ph03nix_x.capacityinfo.interfaces

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.preference.PreferenceManager
import com.ph03nix_x.capacityinfo.MainApp.Companion.batteryIntent
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.helpers.TimeHelper
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.CAPACITY_ADDED
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.DESIGN_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.LAST_CHARGE_TIME
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.PERCENT_ADDED
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.RESIDUAL_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.TEMPERATURE_IN_FAHRENHEIT
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.UNIT_OF_CHARGE_DISCHARGE_CURRENT
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.VOLTAGE_IN_MV
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.VOLTAGE_UNIT
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.pow

@SuppressWarnings("PrivateApi")
interface BatteryInfoInterface {

    companion object {

        var tempCurrentCapacity = 0.0
        var capacityAdded = 0.0
        var tempBatteryLevelWith = 0
        var percentAdded = 0
        var residualCapacity = 0.0
        var batteryLevel = 0
        var maxChargeCurrent = 0
        var averageChargeCurrent = 0
        var minChargeCurrent = 0
        var maxDischargeCurrent = 0
        var averageDischargeCurrent = 0
        var minDischargeCurrent = 0
    }

    fun getOnDesignCapacity(context: Context): Int {

        val powerProfileClass = "com.android.internal.os.PowerProfile"

        val mPowerProfile = Class.forName(powerProfileClass).getConstructor(
            Context::class.java
        ).newInstance(context)

        val designCapacity = (Class.forName(powerProfileClass).getMethod(
            "getBatteryCapacity"
        ).invoke(mPowerProfile) as Double).toInt()

        return when {

            designCapacity == 0 || designCapacity < context.resources.getInteger(
                R.integer.min_design_capacity) -> context.resources.getInteger(
                R.integer.min_design_capacity)
            designCapacity < 0 -> designCapacity / -1
            else -> designCapacity
        }
    }

    fun getOnBatteryLevel(context: Context) = try {

        (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)?.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }

    catch (e: RuntimeException) {

        val batteryIntent = context.registerReceiver(
            null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )

        batteryIntent?.getStringExtra(BatteryManager.EXTRA_LEVEL)?.toInt() ?: 0
    }

    fun getOnChargeDischargeCurrent(context: Context): Int {

        return try {

            val pref = PreferenceManager.getDefaultSharedPreferences(context)

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                    as BatteryManager

            var chargeCurrent = batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
            )
            
            val status = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )

            if(chargeCurrent < 0) chargeCurrent /= -1

            if(pref.getString(UNIT_OF_CHARGE_DISCHARGE_CURRENT, "μA") == "μA")
                chargeCurrent /= 1000

            getOnMaxAverageMinChargeDischargeCurrent(status, chargeCurrent)
            
            chargeCurrent
        }

        catch (e: RuntimeException) {

            val status = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )

            getOnMaxAverageMinChargeDischargeCurrent(status, 0)

            0
        }
    }
    
    fun getOnMaxAverageMinChargeDischargeCurrent(status: Int?, chargeCurrent: Int) {
        
        when(status) {

            BatteryManager.BATTERY_STATUS_CHARGING -> {

                maxDischargeCurrent = 0
                averageDischargeCurrent = 0
                minDischargeCurrent = 0

                if (chargeCurrent > maxChargeCurrent) maxChargeCurrent = chargeCurrent

                if (chargeCurrent < minChargeCurrent && chargeCurrent < maxChargeCurrent)
                    minChargeCurrent = chargeCurrent
                else if (minChargeCurrent == 0 && chargeCurrent < maxChargeCurrent)
                    minChargeCurrent = chargeCurrent

                if (maxChargeCurrent > 0 && minChargeCurrent > 0)
                    averageChargeCurrent = (maxChargeCurrent + minChargeCurrent) / 2
            }

            BatteryManager.BATTERY_STATUS_DISCHARGING -> {

                maxChargeCurrent = 0
                averageChargeCurrent = 0
                minChargeCurrent = 0

                if (chargeCurrent > maxDischargeCurrent) maxDischargeCurrent = chargeCurrent

                if (chargeCurrent < minDischargeCurrent && chargeCurrent < maxDischargeCurrent)
                    minDischargeCurrent = chargeCurrent
                else if (minDischargeCurrent == 0 && chargeCurrent < maxDischargeCurrent)
                    minDischargeCurrent = chargeCurrent

                if (maxDischargeCurrent > 0 && minDischargeCurrent > 0)
                    averageDischargeCurrent = (maxDischargeCurrent + minDischargeCurrent) / 2
            }

            BatteryManager.BATTERY_STATUS_UNKNOWN -> {

                maxChargeCurrent = 0
                averageChargeCurrent = 0
                minChargeCurrent = 0
                maxDischargeCurrent = 0
                averageDischargeCurrent = 0
                minDischargeCurrent = 0
            }
        }
        
    }

    fun getOnChargingCurrentLimit(context: Context): String? {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        val constantChargeCurrentMax = File(
            "/sys/class/power_supply/battery/constant_charge_current_max")

        var chargingCurrentLimit: String? = null

       return if(constantChargeCurrentMax.exists()) {

            try {

                val bufferReader = BufferedReader(FileReader(constantChargeCurrentMax))

                chargingCurrentLimit = bufferReader.readLine()

                bufferReader.close()

                if(pref.getString(UNIT_OF_CHARGE_DISCHARGE_CURRENT, "μA") == "μA")
                    chargingCurrentLimit = ((chargingCurrentLimit?.toInt() ?: 0) / 1000).toString()

                chargingCurrentLimit
            }

            catch (e: IOException) { chargingCurrentLimit }
        }

        else chargingCurrentLimit
    }

    fun getOnTemperature(context: Context): String {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        var temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            ?.toDouble() ?: 0.0

        temp /= 10.0

        return DecimalFormat("#.#").format(
            if (pref.getBoolean(TEMPERATURE_IN_FAHRENHEIT, context.resources.getBoolean(
                    R.bool.temperature_in_fahrenheit))) (temp * 1.8) + 32.0 else temp)
    }

    fun getOnTemperatureInDouble(context: Context): Double {

        batteryIntent = context.registerReceiver(
            null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )

        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            ?.toDouble() ?: 0.0

        return temp / 10.0
    }

    fun getOnCurrentCapacity(context: Context): Double {

      return try {

          val pref = PreferenceManager.getDefaultSharedPreferences(context)

          val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                  as BatteryManager

          val currentCapacity = batteryManager.getIntProperty(
              BatteryManager
                  .BATTERY_PROPERTY_CHARGE_COUNTER
          ).toDouble()

          when {

              currentCapacity < 0 -> 0.001

              pref.getString(UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY, "μAh") == "μAh" ->
                  currentCapacity / 1000

              else -> currentCapacity
          }
      }

      catch (e: RuntimeException) { 0.001 }
    }

    fun getOnCapacityAdded(context: Context, isOverlay: Boolean = false): String {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        batteryIntent = context.registerReceiver(
            null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )

            return when(batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )) {

                BatteryManager.BATTERY_STATUS_CHARGING -> {

                    percentAdded = (getOnBatteryLevel(context) ?: 0) - tempBatteryLevelWith

                    if (percentAdded < 0) percentAdded = 0

                    capacityAdded = getOnCurrentCapacity(context) - tempCurrentCapacity

                    if (capacityAdded < 0) capacityAdded /= -1

                    context.getString(
                        if (!isOverlay) R.string.capacity_added else
                            R.string.capacity_added_overlay_only_values, DecimalFormat("#.#")
                            .format(capacityAdded), "$percentAdded%"
                    )
                }

            else -> context.getString(
                if (!isOverlay) R.string.capacity_added
                else R.string.capacity_added_overlay_only_values, DecimalFormat("#.#").format(
                    pref.getFloat(CAPACITY_ADDED, 0f).toDouble()
                ), "${
                    pref.getInt(
                        PERCENT_ADDED,
                        0
                    )
                }%"
            )
        }
    }

    fun getOnVoltage(context: Context): Double {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        batteryIntent = context.registerReceiver(
            null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )

        var voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            ?.toDouble() ?: 0.0

        if(!pref.getBoolean(VOLTAGE_IN_MV, context.resources.getBoolean(R.bool.voltage_in_mv))) {

            if(pref.getString(VOLTAGE_UNIT, "mV") == "μV")
                voltage /= 1000.0.pow(2.0)
            else voltage /= 1000
        }

        else if(pref.getString(VOLTAGE_UNIT, "mV") == "μV") voltage /= 1000

        return voltage
    }

    fun getOnBatteryHealth(context: Context): String {

        batteryIntent = context.registerReceiver(
            null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )

        return when(batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        )) {

            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.battery_health_cold)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(
                R.string.battery_health_overheat
            )
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(
                R.string.battery_health_over_voltage
            )
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(
                R.string.battery_health_unspecified_failure
            )
            else -> context.getString(R.string.unknown)
        }
    }

    fun getOnResidualCapacity(context: Context, isOverlay: Boolean = false): String {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        residualCapacity = pref.getInt(RESIDUAL_CAPACITY, 0).toDouble()

        residualCapacity /= if(pref.getString(UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY,
                "μAh") == "μAh") 1000.0 else 100.0

        if(residualCapacity < 0.0) residualCapacity /= -1.0

        return context.getString(if(!isOverlay) R.string.residual_capacity else
                R.string.residual_capacity_overlay_only_values, DecimalFormat("#.#").format(
                residualCapacity), "${DecimalFormat("#.#").format((
                residualCapacity / pref.getInt(DESIGN_CAPACITY, context.resources.getInteger(
                    R.integer.min_design_capacity)).toDouble()) * 100.0)}%")
    }

    fun getOnStatus(context: Context, extraStatus: Int): String {

        return when(extraStatus) {

            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.not_charging)
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.charging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.full)
            else -> context.getString(R.string.unknown)
        }
    }

    fun getOnSourceOfPower(
        context: Context, extraPlugged: Int,
        isOverlay: Boolean = false
    ): String {

        return when(extraPlugged) {

            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(
                if (!isOverlay)
                    R.string.source_of_power else R.string.source_of_power_overlay_only_values,
                context.getString(R.string.source_of_power_ac)
            )
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(
                if (!isOverlay)
                    R.string.source_of_power else R.string.source_of_power_overlay_only_values,
                context.getString(R.string.source_of_power_usb)
            )
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(
                if (!isOverlay)
                    R.string.source_of_power else R.string.source_of_power_overlay_only_values,
                context.getString(R.string.source_of_power_wireless)
            )
            else -> "N/A"
        }
    }
    
    fun getOnBatteryWear(context: Context, isOverlay: Boolean = false): String {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        val designCapacity = pref.getInt(
            DESIGN_CAPACITY, context.resources.getInteger(
                R.integer.min_design_capacity
            )
        ).toDouble()

        return context.getString(
            if (!isOverlay) R.string.battery_wear else
                R.string.battery_wear_overlay_only_values, if (residualCapacity > 0
                && residualCapacity < designCapacity
            ) "${
                DecimalFormat("#.#").format(
                    100 - ((residualCapacity / designCapacity) * 100)
                )
            }%" else "0%",
            if (residualCapacity > 0 && residualCapacity < designCapacity) DecimalFormat(
                "#.#"
            ).format(designCapacity - residualCapacity) else "0"
        )
    }

    fun getOnChargingTime(context: Context, seconds: Int, isOverlay: Boolean = false): String {

        return context.getString(
            if (!isOverlay) R.string.charging_time else
                R.string.charging_time_overlay_only_values, TimeHelper.getTime(seconds.toLong())
        )
    }

    fun getOnChargingTimeRemaining(context: Context): String {

        var chargingTimeRemaining: Double

        val batteryLevel = getOnBatteryLevel(context) ?: 0

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        val currentCapacity = getOnCurrentCapacity(context)

        val residualCapacity = if(pref.getString(
                UNIT_OF_MEASUREMENT_OF_CURRENT_CAPACITY,
                "μAh"
            ) == "μAh") pref.getInt(RESIDUAL_CAPACITY, 0)
            .toDouble() / 1000.0 else pref.getInt(RESIDUAL_CAPACITY, 0).toDouble() / 100.0

        val chargeDischargeCurrent = getOnChargeDischargeCurrent(context).toDouble()

        return if(currentCapacity > 0.0 && currentCapacity < residualCapacity
            && residualCapacity > 0.0) {

            val capacity = if(batteryLevel < 100) residualCapacity - currentCapacity
            else ((residualCapacity - currentCapacity) - 150.0)

            if(chargeDischargeCurrent > 0.0) {

                chargingTimeRemaining = if(chargeDischargeCurrent > 500.0)
                    (capacity / chargeDischargeCurrent) * 1.1 else capacity / chargeDischargeCurrent

                chargingTimeRemaining *= 3600.0

                TimeHelper.getTime(chargingTimeRemaining.toLong())
            }

            else context.getString(R.string.unknown)
        }

        else if(currentCapacity > 0.0 && (currentCapacity >= residualCapacity
                    || currentCapacity >= pref.getInt(
                DESIGN_CAPACITY, context.resources.getInteger(
                    R.integer.min_design_capacity
                )
            ).toDouble()) && residualCapacity > 0.0)
        context.getString(R.string.unknown)

        else if(currentCapacity > 0.0 && residualCapacity == 0.0) {

            val capacity = pref.getInt(
                DESIGN_CAPACITY, context.resources.getInteger(
                    R.integer.min_design_capacity
                )
            ) - currentCapacity

            if(chargeDischargeCurrent > 0.0) {

                chargingTimeRemaining = if(chargeDischargeCurrent > 500.0)
                    (capacity / chargeDischargeCurrent) * 1.1 else capacity / chargeDischargeCurrent

                chargingTimeRemaining *= 3600.0

                TimeHelper.getTime(chargingTimeRemaining.toLong())
            }

            else context.getString(R.string.unknown)
        }

        else {

            val designCapacity = pref.getInt(
                DESIGN_CAPACITY, context.resources.getInteger(
                    R.integer.min_design_capacity
                )
            ).toDouble()

            val capacity = designCapacity - (designCapacity * (
                    batteryLevel.toDouble() / 100.0))

            if(chargeDischargeCurrent > 0.0) {

                chargingTimeRemaining = if(chargeDischargeCurrent > 500.0)
                    (capacity / chargeDischargeCurrent) * 1.1
                else capacity / chargeDischargeCurrent

                chargingTimeRemaining *= 3600.0

                TimeHelper.getTime(chargingTimeRemaining.toLong())
            }

            else context.getString(R.string.unknown)
        }
    }

    fun getOnRemainingBatteryTime(context: Context): String {

        val currentCapacity = getOnCurrentCapacity(context)

        return if(averageDischargeCurrent > 0.0) {

            val remainingBatteryTime =
                ((currentCapacity / averageDischargeCurrent) * 3600.0).toLong()

            TimeHelper.getTime(remainingBatteryTime)
        }

        else context.getString(R.string.unknown)
    }

    fun getOnLastChargeTime(context: Context): String {
        
        val seconds = PreferenceManager.getDefaultSharedPreferences(context).getInt(
            LAST_CHARGE_TIME, 0
        ).toLong()

        return TimeHelper.getTime(seconds)
    }
}