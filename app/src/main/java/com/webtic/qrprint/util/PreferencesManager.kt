package com.webtic.qrprint.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import com.brother.ptouch.sdk.LabelInfo
import com.brother.ptouch.sdk.NetPrinter
import com.brother.ptouch.sdk.Printer
import com.brother.ptouch.sdk.PrinterInfo
import com.brother.ptouch.sdk.PrinterInfo.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        private const val PREFERENCE_KEY = "preferences"
        private const val MAC_ADDRESS_KEY = "macAddress"
        private const val TYPE_KEY = "type"
        private const val CAMERA_KEY = "camera"
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)

    var macAddr: String = loadMacAddress()
        set(value) {
            updateMacAddress(value)
            typeMap[value]?.let {
                type = it
            }
            field = value
        }
    private var type: PrinterType = loadType()
        set(value) {
            updateType(value)
            field = value
        }

    private val availablePrinters: MutableSet<NetPrinter> = mutableSetOf()
    private val typeMap: MutableMap<String, PrinterType> = mutableMapOf()

    var cameraBack: Boolean = loadCamera()
        set(value) {
            updateCamera(value)
            field = value
        }

    fun availablePrinters(listener: PrinterResultListener) {
        CoroutineScope(Dispatchers.IO).async {
            var list = Printer().getNetPrinters("QL-720NW")
            availablePrinters.addAll(list)
            typeMap.putAll(list.map { it.macAddress to PrinterType.QL_720NW })
            list = Printer().getNetPrinters("QL-810W")
            availablePrinters.addAll(list)
            typeMap.putAll(list.map { it.macAddress to PrinterType.QL_810W })
            list.forEach {
                if (it != null)
                    availablePrinters.add(it)
            }
            CoroutineScope(Dispatchers.Main).async {
                listener.onPrinterListReady(availablePrinters.toList())
            }
        }
    }

    fun printImage(listener: PrinterResultListener, vararg image: Pair<Bitmap, Int>) {
        if (macAddr.isBlank())
            throw IllegalStateException("Mac Address cannot be empty!")
        CoroutineScope(Dispatchers.IO).launch {

            image.forEach {
                val printer = Printer()
                printer.printerInfo = PrinterInfo().apply {
                    printerModel = when (type) {
                        PrinterType.QL_720NW -> Model.QL_720NW
                        PrinterType.QL_810W -> Model.QL_810W
                        PrinterType.NONE -> throw IllegalStateException("Printer type not specified!")
                    }
                    port = Port.NET
                    macAddress = macAddr
                    labelNameIndex = LabelInfo.QL700.W62.ordinal
                    numberOfCopies = it.second
                    workPath = context.cacheDir.path
                }
                if (printer.startCommunication()) {
                    val result = printer.printImage(it.first)
                    CoroutineScope(Dispatchers.Main).launch {
                        if (result.errorCode != ErrorCode.ERROR_NONE)
                            listener.onPrintResult(Error(result.errorCode))
                    }
                    printer.endCommunication()
                }
            }
        }
    }

    private fun updateMacAddress(value: String) {
        with(preferences.edit()) {
            putString(MAC_ADDRESS_KEY, value)
            commit()
        }
    }

    private fun updateType(type: PrinterType) {
        with(preferences.edit()) {
            putInt(TYPE_KEY, type.ordinal)
            commit()
        }
    }

    private fun loadMacAddress(): String {
        return preferences.getString(MAC_ADDRESS_KEY, "") ?: ""
    }

    private fun loadType(): PrinterType {
        val ordinal = preferences.getInt(TYPE_KEY, 0)
        return PrinterType.values()[ordinal]
    }

    private fun updateCamera(value: Boolean) {
        with(preferences.edit()) {
            putBoolean(CAMERA_KEY, value)
            commit()
        }
    }

    private fun loadCamera(): Boolean {
        return preferences.getBoolean(CAMERA_KEY, true)
    }

    interface PrinterResultListener {
        fun onPrinterListReady(list: List<NetPrinter>)
        fun onPrintResult(result: PrinterResult)
    }
}

sealed class PrinterResult
object Success : PrinterResult()
class Error(val errorCode: ErrorCode) : PrinterResult()

enum class PrinterType {
    NONE, QL_720NW, QL_810W
}