package com.example.vcam.location.xposed

import com.example.vcam.location.xposed.helpers.LocationLogger

import android.annotation.SuppressLint
import android.os.Build
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.example.vcam.location.xposed.cellar.PhoneInterfaceManagerHooker
import com.example.vcam.location.xposed.cellar.TelephonyRegistryHooker
import com.example.vcam.location.xposed.helpers.workround.Miui
import com.example.vcam.location.xposed.helpers.workround.Oplus
import com.example.vcam.location.xposed.location.LocationHookerAfterS
import com.example.vcam.location.xposed.location.LocationHookerPreQ
import com.example.vcam.location.xposed.location.LocationHookerR
import com.example.vcam.location.xposed.location.WLANHooker
import com.example.vcam.location.xposed.location.gnss.GnssHookerPreQ
import com.example.vcam.location.xposed.location.gnss.GnssManagerServiceHookerR
import com.example.vcam.location.xposed.location.gnss.GnssManagerServiceHookerS
import com.example.vcam.location.xposed.location.miui.MiuiBlurLocationManagerHookerR
import com.example.vcam.location.xposed.location.miui.MiuiBlurLocationManagerHookerS
import com.example.vcam.location.xposed.location.oplus.NlpDLCS
import java.lang.Exception

@ExperimentalStdlibApi
class HookEntry : IXposedHookLoadPackage {
    @SuppressLint("PrivateApi", "ObsoleteSdkInt", "NewApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam != null) {
            when (lpparam.packageName) {
                "android" -> {
                    EzXHelperInit.initHandleLoadPackage(lpparam)
                    EzXHelperInit.setLogTag("VCAM Location")
                    EzXHelperInit.setToastTag("VCAM")

                    LocationLogger.log("VCAM: [location] init hooks")

                    try {
                        TelephonyRegistryHooker().hookListen(lpparam)

                        // For Android 12 and MIUI, run this hook
                        when (Build.VERSION.SDK_INT) {
                            Build.VERSION_CODES.S -> {
                                if (Miui().isMIUI()) {
                                    MiuiBlurLocationManagerHookerS().hookGetBlurryLocationS(lpparam)
                                } else if (Oplus().isOplus()) {
                                    NlpDLCS().hookColorOS(lpparam)
                                }
                                LocationHookerAfterS().hookLastLocation(lpparam)
                                LocationHookerAfterS().hookDLC(lpparam)

                                GnssManagerServiceHookerS().hookRegisterGnssNmeaCallback(lpparam)
                            }
                            Build.VERSION_CODES.R -> {  // Android 11 and MIUI
                                if (Miui().isMIUI()) {
                                    MiuiBlurLocationManagerHookerR().hookGetBlurryLocation(lpparam)
                                }

                                LocationHookerR().hookLastLocation(lpparam)
                                LocationHookerR().hookDLC(lpparam)

                                GnssManagerServiceHookerR().hookAddGnssBatchingCallback(lpparam)
                            }
                            else -> {    // For Android 10 and earlier, run this fallback version
                                LocationHookerPreQ().hookLastLocation(lpparam)

                                GnssHookerPreQ().hookAddGnssBatchingCallback(lpparam)
                            }
                        }

                        WLANHooker().hookWifiManager(lpparam)
                    } catch (e: Exception) {
                        LocationLogger.log("VCAM: [location] hook failed: $e")
                    }
                }

                "com.android.phone" -> {
                    try {
                        PhoneInterfaceManagerHooker().hookCellLocation(lpparam)
                    } catch (e: Exception) {
                        LocationLogger.log("VCAM: [location] phone hooks failed: $e")
                    }
                }
            }
        }

    }
}