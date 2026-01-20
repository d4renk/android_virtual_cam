package com.example.vcam.location.xposed.cellar

import com.example.vcam.location.xposed.helpers.LocationLogger

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.*
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookMethod
import com.github.kyuubiran.ezxhelper.utils.isPublic
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.example.vcam.location.xposed.cellar.identity.Lte
import com.example.vcam.location.xposed.cellar.identity.Nr
import com.example.vcam.location.xposed.helpers.ConfigGateway

class PhoneInterfaceManagerHooker {
    @ExperimentalStdlibApi
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("PrivateApi")
    fun hookCellLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz: Class<*> =
            lpparam.classLoader.loadClass("com.android.phone.PhoneInterfaceManager")

        LocationLogger.log("FL: [Cellar] Finding method in PhoneInterfaceManager")

        findAllMethods(clazz) {
            name == "getImeiForSlot" && isPublic
        }.hookMethod {
            after { param ->
                val packageName = param.args[1] as String
                val customIMEI = "1234567891011120" // TODO: Support custom IMEI information

                LocationLogger.log("FL: [Cellar] in getImeiForSlot! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    param.result = customIMEI
                    LocationLogger.log("FL: [Cellar] In whiteList! Return custom value for testing purpose: $customIMEI")
                }
            }
        }

        findAllMethods(clazz) {
            name == "getMeidForSlot" && isPublic
        }.hookMethod {
            after { param ->
                val packageName = param.args[1] as String
                val customMEID = "1234567891011120" // TODO: Support custom MEID information

                LocationLogger.log("FL: [Cellar] in getMeidForSlot! Caller package name: $packageName")
                if (ConfigGateway.get().inWhitelist(packageName)) {
                    param.result = customMEID
                    ConfigGateway.get().inWhitelist(param.args[1] as String)
                    LocationLogger.log("FL: [Cellar] In whiteList! Return custom value for testing purpose: $customMEID")
                }
            }
        }

        findAllMethods(clazz) {
            name == "getCellLocation" && isPublic
        }.hookMethod {
            after { param ->
                val packageName = param.args[0] as String
                LocationLogger.log("FL: [Cellar] in getCellLocation! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    LocationLogger.log("FL: [Cellar] in whiteList! Return custom cell data information")

                    when (param.result) {
                        is CellIdentityLte -> {
                            LocationLogger.log("FL: [Cellar] Using LTE Network...")
                            param.result = Lte().alterCellIdentity(param.result as CellIdentityLte)
                        }
                        is CellIdentityNr -> {
                            LocationLogger.log("FL: [Cellar] Using Nr Network...")
                            param.result = Nr().alterCellIdentity(param.result as CellIdentityNr)
                        }
                        else -> {
                            LocationLogger.log("FL: [Cellar] Unsupported network type. Return null as fallback")
                            param.result = null
                        }
                    }

                    // Android 9 does not have this network type
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && param.result is CellIdentityNr) {
                        LocationLogger.log("FL: [Cellar] Using NR Network...")
                        param.result = Nr().alterCellIdentity(param.result as CellIdentityNr)
                    }
                }
            }
        }

        findAllMethods(clazz) {
            name == "getAllCellInfo" && isPublic
        }.hookMethod {
            before { param ->
                val packageName = param.args[0] as String
                LocationLogger.log("FL: [Cellar] in getAllCellInfo! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    LocationLogger.log("FL: [Cellar] in whiteList! Return empty AllCellInfo for testing purpose.")
                    val customAllCellInfo = ArrayList<CellInfo>()
                    param.result = customAllCellInfo
                }
            }
        }

        findAllMethods(clazz) {
            name == "getNeighboringCellInfo" && isPublic
        }.hookMethod {
            before { param ->
                val packageName = param.args[0] as String
                LocationLogger.log("FL: [Cellar] in getNeighboringCellInfo! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    LocationLogger.log("FL: [Cellar] in whiteList! Return empty NeighboringCellInfo for testing purpose.")
                    val customNeighboringCellInfo = ArrayList<NeighboringCellInfo>()
                    param.result = customNeighboringCellInfo
                }
            }
        }

        findAllMethods(clazz) {
            name == "requestCellInfoUpdateInternal" && isPublic
        }.hookBefore { param ->
            val packageName = param.args[2] as String
            LocationLogger.log("FL: [Cellar] in requestCellInfoUpdateInternal! Caller package name: $packageName")

            if (ConfigGateway.get().inWhitelist(packageName)) {
                LocationLogger.log("FL: in whiteList! Dropping register request...")
                param.result = null
                return@hookBefore
            }
        }
    }
}