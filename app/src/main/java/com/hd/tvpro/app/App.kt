package com.hd.tvpro.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.multidex.MultiDex
import com.hd.tvpro.constants.TimeConstants
import com.hd.tvpro.util.CrashHandler
import com.hd.tvpro.webserver.WebServer
import com.lzy.okgo.OkGo
import com.lzy.okgo.https.HttpsUtils
import com.lzy.okgo.model.HttpHeaders
import com.pngcui.skyworth.dlna.device.DeviceInfo
import com.pngcui.skyworth.dlna.device.DeviceUpdateBrocastFactory
import com.pngcui.skyworth.dlna.device.ItatisticsEvent
import com.pngcui.skyworth.dlna.util.CommonLog
import com.pngcui.skyworth.dlna.util.LogFactory
import com.umeng.commonsdk.UMConfigure
import com.wanjian.cockroach.Cockroach
import com.wanjian.cockroach.ExceptionHandler
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 作者：By 15968
 * 日期：On 2021/10/26
 * 时间：At 13:36
 */
open class App : Application(), ItatisticsEvent {
    private val log: CommonLog = LogFactory.createLog()

    companion object {
        lateinit var INSTANCE: App
        var webServer: WebServer? = null
    }

    private var mDeviceInfo: DeviceInfo = DeviceInfo()

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        //OKGO配置
        val builder = OkHttpClient.Builder()
        builder.readTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
        builder.writeTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
        builder.connectTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
        //方法一：信任所有证书,不安全有风险
        val sslParams1 = HttpsUtils.getSslSocketFactory()
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, HttpsUtils.UnSafeTrustManager)
            .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
        val headers = HttpHeaders()
        headers.put("charset", "UTF-8")
        OkGo.getInstance().init(this).setOkHttpClient(builder.build())
            .setRetryCount(1)
            .addCommonHeaders(headers)
        installCrashHandler()
        //crash监测
        try {
            UMConfigure.preInit(this, "616f8498e014255fcb521858", "tvpro")
            UMConfigure.init(
                this,
                "616f8498e014255fcb521858",
                "tvpro",
                UMConfigure.DEVICE_TYPE_PHONE,
                null
            )
        } catch (e: Throwable) {
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }


    open fun updateDevInfo(name: String?, uuid: String?) {
        mDeviceInfo.dev_name = name
        mDeviceInfo.uuid = uuid
    }

    open fun setDevStatus(flag: Boolean) {
        mDeviceInfo.status = flag
        DeviceUpdateBrocastFactory.sendDevUpdateBrocast(this)
    }

    fun hasDlanConnect(): Boolean {
        return mDeviceInfo.status
    }

    open fun getDevInfo(): DeviceInfo? {
        return mDeviceInfo
    }

    override fun onEvent(eventID: String?) {
        log.e("eventID = $eventID")
    }

    override fun onEvent(eventID: String?, map: HashMap<String, String>?) {
        log.e("eventID = $eventID")
    }

    private fun getContext(): Context {
        return this
    }

    private fun installCrashHandler() {
        CrashHandler.getInstance()
            .initDefaultHandler(getContext())
        Cockroach.install(this, object : ExceptionHandler() {
            override fun onUncaughtExceptionHappened(thread: Thread, throwable: Throwable?) {
                Timber.e(throwable, "--->onUncaughtExceptionHappened:$thread<---")
                val fileName: String = CrashHandler.getInstance().saveCatchInfo2File(throwable)
                Handler(Looper.getMainLooper()).post {
//                    ToastMgr.shortBottomCenter(
//                        applicationContext,
//                        "检测到异常崩溃信息，已记录崩溃日志"
//                    )
                }
            }

            override fun onBandageExceptionHappened(throwable: Throwable?) {
                val fileName: String = CrashHandler.getInstance().saveCatchInfo2File(throwable)
                Handler(Looper.getMainLooper()).post {
//                    ToastMgr.shortBottomCenter(
//                        applicationContext,
//                        "检测到异常崩溃信息，已记录崩溃日志"
//                    )
                }
            }

            override fun onEnterSafeMode() {}
            override fun onMayBeBlackScreen(e: Throwable?) {
                val thread = Looper.getMainLooper().thread
                CrashHandler.getInstance().crashMySelf(thread, e)
            }
        })
    }
}