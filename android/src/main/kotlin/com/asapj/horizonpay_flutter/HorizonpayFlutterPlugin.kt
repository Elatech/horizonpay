package com.asapj.horizonpay_flutter

import android.app.Activity
import androidx.annotation.NonNull


import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast

import com.horizonpay.smartlink.ISmartLinkService
import com.horizonpay.smartlink.listener.ITransactionResultListener
import com.horizonpay.smartlink.model.Constant
import com.horizonpay.smartlink.model.TransactionRequestEntity
import com.horizonpay.smartlink.model.TransactionResultEntity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.text.DecimalFormat


class HorizonpayFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var activity: Activity

    private var iSmartLinkService: ISmartLinkService? = null
    private var requestEntity: TransactionRequestEntity? = null
    private var resultEntity: TransactionResultEntity? = null
    private var transtype = 0
    private var horizonAppInstalled = false

    private val eventChannelTag = "asapj.horizonpay_flutter.event_channel"

    private var messageChannel: EventChannel? = null
    private var txEventSink: EventChannel.EventSink? = null

    private val methodChannelTag = "asapj.horizonpay_flutter"


    override fun onDetachedFromActivity() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
        bindImplicitService()
    }
    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelTag)
        channel.setMethodCallHandler(this)
        this.context = flutterPluginBinding.applicationContext
        messageChannel = EventChannel(flutterPluginBinding.binaryMessenger, "eventChannelStream")
        messageChannel?.setStreamHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        txEventSink?.success("Hello")
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "bind") {
            bindImplicitService()
            result.success(1)
        }


        //If the called method is neither bind not getPlatformVersion, 
        //then ensure the sdk has been initialised.
        else if (!horizonAppInstalled) {
            result.error("00X20", "HorizonPay App Not Found", "HorizonPay App Not Found")
        } else if (call.method == "logon") {
            performLogon()
            result.success(1)
        } else if (call.method == "purchase") {
            performPurchase(call.argument("amount"))
            result.success(1)
        } else if (call.method == "print") {
            printTest(call.arguments)
            result.success(1)
        } else {
            result.notImplemented()
        }

    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        unbindService()
    }

    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        this.txEventSink = eventSink
    }

    override fun onCancel(arguments: Any?) {
        txEventSink = null
        messageChannel = null
    }

    companion object {
        private const val TAG = "HorizonpayActivity"
        private const val ACTION_BIND_SERVICE = "com.horizon.intent.action.SmartLink"
        fun createExplicitFromImplicitIntent(context: Context, implicitIntent: Intent): Intent {
            // Retrieve all services that can match the given intent


            val pm: PackageManager = context.getPackageManager() //This line throws a null error.
            val resolveInfo: List<ResolveInfo> = pm.queryIntentServices(implicitIntent, 0)
            Log.i(
                    "TTT", """resolveInfo=$resolveInfo
 resolveInfo.size()=${resolveInfo.size}"""
            )
            // Make sure only one match was found
            if (resolveInfo == null || resolveInfo.size != 1) {
                return Intent()
            }

            // Get component info and create ComponentName
            val serviceInfo: ResolveInfo = resolveInfo[0]
            val packageName: String = serviceInfo.serviceInfo.packageName
            val className: String = serviceInfo.serviceInfo.name
            val component = ComponentName(packageName, className)

            // Create a new intent. Use the old one for extras and such reuse
            val explicitIntent = Intent(implicitIntent)
            // Set the component to be explicit
            explicitIntent.setComponent(component)
            return explicitIntent
        }
    }



    private fun printTest(args: Any) {
        // Todo: Perform print job setup

        //test data
        requestEntity = TransactionRequestEntity()
        resultEntity = TransactionResultEntity()
        transtype = Constant.TRANS_TYPE_PRINT
        requestEntity!!.transType = transtype
        requestEntity!!.needPrint = 1
        setExtData()
        Log.d(TAG, "print data: " + requestEntity!!.extData)
        try {
            iSmartLinkService!!.transactionRequest(requestEntity, printResultListener)
            txEventSink?.success("'Printed data'")
            Log.d(TAG, "doPrint: 0000")
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun performPurchase(amount: Double?) {
        transtype = Constant.TRANS_TYPE_PURCHASE
        requestEntity = TransactionRequestEntity()
        resultEntity = TransactionResultEntity()
        val etAmount = amount
        val etReferenceno = "00001"
        val etdestpan = "te"
        val dec = DecimalFormat("#.##")
        Log.d(TAG, "doTrans: amount:" + dec.format(etAmount))
        Log.d(TAG, "doTrans: refno:" + etReferenceno.toString().trim())

        requestEntity!!.setAmount(dec.format(etAmount))
//      requestEntity!!.setCashBackAmt(etCashAmount.toString())
        requestEntity!!.setOrigRefNum(etReferenceno)
        requestEntity!!.setTransferPan(etdestpan)
        requestEntity!!.setTransType(transtype)
        requestEntity!!.setNeedPrint(1)
        setExtData()
        Log.d(TAG, "doTrans: " + requestEntity!!.getExtData())
        try {
            iSmartLinkService?.transactionRequest(requestEntity, transactionResultListener)
            Log.d(TAG, "doTrans: 0000")
            txEventSink?.success("txEventSink!!.success('Printed data')")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this.context, "Check payment App", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogon() {
        transtype = Constant.TRANS_TYPE_LOGON;
        requestEntity = TransactionRequestEntity()
        resultEntity = TransactionResultEntity()
        requestEntity!!.setTransType(transtype)
        setExtData()
        Log.d(TAG, "doTrans: " + requestEntity!!.getExtData())

        try {
            iSmartLinkService?.transactionRequest(requestEntity, transactionResultListener)
            Log.d(TAG, "doTrans: 0000")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this.context, "Check payment App", Toast.LENGTH_SHORT).show()
        }
    }


    private fun bindImplicitService() {
        var isOK = false
        val intent = Intent()
        intent.setAction(ACTION_BIND_SERVICE) // Use Action in intent to find service need to bind.
        val eintent = Intent(createExplicitFromImplicitIntent(this.context, intent))
        try {
            isOK = this.activity.bindService(eintent, mConnection, BIND_AUTO_CREATE)
            horizonAppInstalled = true;

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this.context, "Check payment App", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unbindService() {
        if (mConnection != null) {
            this.activity.unbindService(mConnection)
        }
//        showResult(tvResult, "bind service===" + false)
//        Log.d(MainActivity.TAG, "unbindService: success")
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        @Override
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder?) {
            Log.d(TAG, "Service connected\r\n")
            iSmartLinkService = ISmartLinkService.Stub.asInterface(iBinder)
//            showResult(tvResult, "ServiceConnected:$componentName")
        }

        @Override
        override fun onServiceDisconnected(componentName: ComponentName?) {
        }
    }


    private val transactionResultListener = object : ITransactionResultListener.Stub() {
        @Override
        @Throws(RemoteException::class)
        override fun onTransactionResult(transactionResultEntity: TransactionResultEntity) {
            txEventSink?.success("Transaction done and dusted")
            Log.d(
                    TAG, """
     ---------------------
     onTransactionResult: ${transactionResultEntity.getResultCode()}
     """.trimIndent()
            )
            val builder: StringBuilder = StringBuilder()
            builder.append(
                    """
                
                Result:${transactionResultEntity.getResultCode()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                TID:${transactionResultEntity.getTID()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                MID:${transactionResultEntity.getMID()}
                """.trimIndent()
            )
            var isNeedPntTrans = false
            if (transactionResultEntity.getResultCode() === Constant.SUCC) {
                when (requestEntity?.getTransType()) {
                    Constant.TRANS_TYPE_PURCHASE, Constant.TRANS_TYPE_CASHADVANCE, Constant.TRANS_TYPE_PURCHASEWITHCB, Constant.TRANS_TYPE_REFUND, Constant.TRANS_TYPE_REVERSAL -> {
                        isNeedPntTrans = true
                        builder.append(
                                """
                            
                            Response Code:${transactionResultEntity.getHostRespCode()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Response Msg:${transactionResultEntity.getHostRespMsg()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            MCC:${transactionResultEntity.getMCC()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Amount:${transactionResultEntity.getApprovedAmount()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            STAN:${transactionResultEntity.getTraceNum()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Batch NO:${transactionResultEntity.getBatchNo()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            RRN:${transactionResultEntity.getRefNum()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Card Number:${transactionResultEntity.getPAN()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Card Expr:${transactionResultEntity.getExpireDate()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Card holder:${transactionResultEntity.getHolderName()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Verify by:${transactionResultEntity.getVerifyType()}
                            """.trimIndent()
                        )
                    }
                    Constant.TRANS_TYPE_REPROT -> {
                        builder.append(
                                """
                            
                            Total Debit Num:${transactionResultEntity.getApprovedDebitNum()}
                            """.trimIndent()
                        )
                        builder.append(
                                """
                            
                            Total Debit Amount:${transactionResultEntity.getApprovedDebitAmt()}
                            """.trimIndent()
                        )
                    }
                    else -> {
                    }
                }
                builder.append(
                        """
                    
                    PTSP:${transactionResultEntity.getPtspName()}
                    """.trimIndent()
                )
                builder.append(
                        """
                    
                    Footer:${transactionResultEntity.getFooterMsg()}
                    """.trimIndent()
                )
            }
            builder.append(
                    """
                
                Time:${transactionResultEntity.getTimestamp()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                SN:${transactionResultEntity.getDeviceSN()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                App Version:${transactionResultEntity.getBaseAppVer()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                Ext:${transactionResultEntity.getExtData()}
                """.trimIndent()
            )
//            showResult(tvResult, builder.toString())
            resultEntity = transactionResultEntity
            if (transactionResultEntity.getResultCode() === Constant.SUCC && requestEntity?.getNeedPrint() === 0 && isNeedPntTrans) {
                Log.d(TAG, "print: 0000")
                print()
            }
        }
    }

    private val printResultListener: ITransactionResultListener = object : ITransactionResultListener.Stub() {
        @Override
        @Throws(RemoteException::class)
        override fun onTransactionResult(transactionResultEntity: TransactionResultEntity) {
            txEventSink?.success("Print success listener ")
            Log.d(
                    TAG, """
     ---------------------
     onTransactionResult: ${transactionResultEntity.getResultCode()}
     """.trimIndent()
            )
            val builder: StringBuilder = StringBuilder()
            builder.append(
                    """
                
                Result:${transactionResultEntity.getResultCode()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                TID:${transactionResultEntity.getTID()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                MID:${transactionResultEntity.getMID()}
                """.trimIndent()
            )
            if (transactionResultEntity.getResultCode() === Constant.SUCC) {
                builder.append(
                        """
                    
                    PTSP:${transactionResultEntity.getPtspName()}
                    """.trimIndent()
                )
                builder.append(
                        """
                    
                    Footer:${transactionResultEntity.getFooterMsg()}
                    """.trimIndent()
                )
            }
            builder.append(
                    """
                
                Time:${transactionResultEntity.getTimestamp()}
                """.trimIndent()
            )
            builder.append(
                    """
                
                SN:${transactionResultEntity.getDeviceSN()}
                """.trimIndent()
            )
//            showResult(tvResult, builder.toString())
            resultEntity = transactionResultEntity

            txEventSink?.success(
            "hello"
            )
        }
    }

    private fun print() {
        //test data
        requestEntity = TransactionRequestEntity()
        resultEntity = TransactionResultEntity()
        transtype = Constant.TRANS_TYPE_PRINT
        requestEntity!!.setTransType(transtype)
        requestEntity!!.setNeedPrint(1)
        setExtData()
        Log.d(TAG, "print data: " + requestEntity!!.getExtData())
        try {
            iSmartLinkService?.transactionRequest(requestEntity, printResultListener)
            Log.d(TAG, "doTrans: 0000")
            txEventSink?.success("Print Method")

        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    private fun setExtData() {
        var data = ""
        when (transtype) {
            Constant.TRANS_TYPE_PRINT -> data = resultEntity?.let { formReceipt(it).toString() }.toString()
            Constant.TRANS_TYPE_REPROT -> data = formReportRequest().toString()
            Constant.MODE_SETUP_PAGE -> data = forMerchantInfo().toString()
            else -> {
            }
        }
        requestEntity?.setExtData(data)
    }

    private fun formReceipt(resultEntity: TransactionResultEntity): JSONObject {
        val json = JSONObject()
        try {
            json.put(Constant.PNT_LOGO_PATH, "/sdcard/logo/printlogo/horizonpay.bmp")
            val array = JSONArray()
            val line1 = JSONObject()
            line1.put(Constant.PNT_CONTENT_LEFT, "Terminal ID:")
            line1.put(Constant.PNT_CONTENT_RIGHT, "123456")
            line1.put(Constant.PNT_TEXT_SIZE, 26)
            line1.put(Constant.PNT_TEXT_BOLD, true)
            line1.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line1)
            val line2 = JSONObject()
            line2.put(Constant.PNT_CONTENT_LEFT, "MID ID:")
            line2.put(Constant.PNT_CONTENT_RIGHT, "123456789089988")
            line2.put(Constant.PNT_TEXT_SIZE, 26)
            line2.put(Constant.PNT_TEXT_BOLD, true)
            line2.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line2)
            val line3 = JSONObject()
            line3.put(Constant.PNT_CONTENT, "--------------------------------------")
            line3.put(Constant.PNT_TEXT_ALIGN, "center")
            line3.put(Constant.PNT_TEXT_SIZE, 20)
            line3.put(Constant.PNT_TEXT_BOLD, true)
            line3.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line3)
            val line4 = JSONObject()
            line4.put(Constant.PNT_CONTENT, "Amount:")
            line4.put(Constant.PNT_TEXT_ALIGN, "left")
            line4.put(Constant.PNT_TEXT_SIZE, 26)
            line4.put(Constant.PNT_TEXT_BOLD, true)
            line4.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line4)
            val line5 = JSONObject()
            line5.put(Constant.PNT_CONTENT, " \u20a6 123456.89")
            line5.put(Constant.PNT_TEXT_ALIGN, "right")
            line5.put(Constant.PNT_TEXT_SIZE, 40)
            line5.put(Constant.PNT_TEXT_BOLD, true)
            line5.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line5)
            val line6 = JSONObject()
            line6.put(Constant.PNT_CONTENT, "--X--X--X--X--X--X--X--X--X--X--X--X")
            line6.put(Constant.PNT_TEXT_ALIGN, "center")
            line6.put(Constant.PNT_TEXT_SIZE, 20)
            line6.put(Constant.PNT_TEXT_BOLD, true)
            line6.put(Constant.PNT_TEXT_INVERT, false)
            array.put(line6)
            val line7 = JSONObject()
            line7.put(Constant.PNT_LOGO_PATH, "/sdcard/logo/printlogo/horizonpay.bmp")
            line7.put(Constant.PNT_TEXT_ALIGN, "center")
            array.put(line7)
            val line8 = JSONObject()
            line8.put("barcode", "112344556778")
            line8.put(Constant.PNT_TEXT_ALIGN, "center")
            line8.put("width", 260)
            line8.put("height", 50)
            array.put(line8)
            val line9 = JSONObject()
            line9.put("qrcode", "www.horizonpay.cn")
            line9.put(Constant.PNT_TEXT_ALIGN, "center")
            line9.put("width", 200)
            array.put(line9)
            val lin10 = JSONObject()
            lin10.put(Constant.PNT_LOGO_PATH, "/sdcard/logo/printlogo/horizonpay.bmp")
            lin10.put(Constant.PNT_TEXT_ALIGN, "center")
            array.put(lin10)
            val gap = JSONObject()
            gap.put(Constant.PNT_LINE_GAP, 60)
            json.put(Constant.PNT_STRING_BODY, array)
        } catch (e: JSONException) {
            Log.e(TAG, "", e)
        }
        return json
    }

    private fun formReportRequest(): JSONObject {
        val json = JSONObject()
        try {
            json.put(Constant.REPORT_START_DATE, "20210102") //yyyymmdd
            json.put(Constant.REPORT_END_DATE, "20210115") //yyyymmdd
        } catch (e: JSONException) {
            Log.e(TAG, "", e)
        }
        return json
    }

    private fun forMerchantInfo(): JSONObject {
        val json = JSONObject()
        try {
            json.put("username", "support@horizonpay.cn")
            json.put("userpwd", "123456")
        } catch (e: JSONException) {
            Log.e(TAG, "", e)
        }
        return json
    }

}
