package cn.zzstc.cloud_push

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.huawei.HuaWeiRegister
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.alibaba.sdk.android.push.register.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


/** CloudPushPlugin */
public class CloudPushPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var appContext: Context


    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    private fun onAttachedToEngine(context: Context, binaryMessenger: BinaryMessenger) {
        appContext = context
        val channel = MethodChannel(binaryMessenger, "cloud_push")
        methodChannel = channel
        channel.setMethodCallHandler(this)
//        val intentFilter = IntentFilter(METHOD_INVOKE_ACTION)
//        LocalBroadcastManager.getInstance(appContext).registerReceiver(this, intentFilter)

    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {

        private const val TAG = "CloudPushPlugin"
        var initialized = false
        private var registered = false

        var methodChannel: MethodChannel? = null


//        const val METHOD_INVOKE_ACTION = "CloudPushPlugin.METHOD_INVOKE"


        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = CloudPushPlugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }


        /**
         * 阿里云推送SDK限制必须在channel进程也进行SDK初始化调用
         * 所以必须在[Application.onCreate]方法上调用
         */
        @JvmStatic
        fun initPushService(application: Application) {
            PushServiceFactory.init(application.applicationContext)
            val pushService = PushServiceFactory.getCloudPushService()
            pushService.register(application.applicationContext, object : CommonCallback {
                override fun onSuccess(response: String?) {
                    registered = true
                    if (initialized && registered) {
                        methodChannel?.invokeMethod("initCloudChannelResult", mapOf(
                                "isSuccessful" to true,
                                "response" to "成功"
                        ))
                    }
                }

                override fun onFailed(errorCode: String?, errorMessage: String?) {
                    Log.e(TAG, "initPushService onFailed: $errorMessage")
                }
            })
            pushService.setPushIntentService(CloudPushIntentService::class.java)
            val appInfo = application.packageManager
                    .getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            registerXiaomiChannel(application, appInfo)
            registerHuaweiChannel(application, appInfo)
            registerOppoChannel(application, appInfo)
            registerMeizuChannel(application, appInfo)
            registerVivoChannel(application, appInfo)
            registerGcmChannel(application, appInfo)
        }

        private fun registerGcmChannel(application: Application, appInfo: ApplicationInfo?) {
            val gcmSendId = appInfo?.metaData?.getString("com.gcm.push.send_id")?.removePrefix("gcm_")
            val gcmApplicationId = appInfo?.metaData?.getString("com.gcm.push.app_id")?.removePrefix("gcm_")
            if (gcmSendId?.isNotBlank() == true && gcmApplicationId?.isNotBlank() == true) {
                Log.d(TAG, "正在注册Gcm推送服务...")
                GcmRegister.register(application.applicationContext, gcmSendId, gcmApplicationId)
            }
        }

        private fun registerVivoChannel(application: Application, appInfo: ApplicationInfo?) {
            VivoRegister.register(application.applicationContext)
        }

        private fun registerMeizuChannel(application: Application, appInfo: ApplicationInfo?) {
            val meizuAppId = appInfo?.metaData?.getString("com.meizu.push.client.app_id")?.removePrefix("meizu_")
            val meizuAppKey = appInfo?.metaData?.getString("com.meizu.push.client.app_key")?.removePrefix("meizu_")
            if (meizuAppId?.isNotBlank() == true && meizuAppKey?.isNotBlank() == true) {
                Log.d(TAG, "正在注册魅族推送服务...")
                MeizuRegister.register(application.applicationContext, meizuAppId, meizuAppKey)
            }
        }

        private fun registerOppoChannel(application: Application, appInfo: ApplicationInfo?) {
            val oppoAppKey = appInfo?.metaData?.getString("com.oppo.push.client.app_key")?.removePrefix("oppo_")
            val oppoAppSecret = appInfo?.metaData?.getString("com.oppo.push.client.app_secret")?.removePrefix("oppo_")
            if (oppoAppKey?.isNotBlank() == true && oppoAppSecret?.isNotBlank() == true) {
                Log.d(TAG, "正在注册Oppo推送服务...")
                OppoRegister.register(application.applicationContext, oppoAppKey, oppoAppSecret)
            }
        }

        private fun registerHuaweiChannel(application: Application, appInfo: ApplicationInfo?) {
            HuaWeiRegister.register(application)
        }

        private fun registerXiaomiChannel(application: Application, appInfo: ApplicationInfo?) {
            val xiaomiAppId = appInfo?.metaData?.getString("com.xiaomi.push.client.app_id")?.removePrefix("xiaomi_")
            val xiaomiAppKey = appInfo?.metaData?.getString("com.xiaomi.push.client.app_key")?.removePrefix("xiaomi_")
            if (xiaomiAppId?.isNotBlank() == true
                    && xiaomiAppKey?.isNotBlank() == true) {
                Log.d(TAG, "正在注册小米推送服务...")
                MiPushRegister.register(application.applicationContext, xiaomiAppId, xiaomiAppKey)
            }
        }


    }


    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "init" -> initPlugin(result)
            "deviceId" -> result.success(PushServiceFactory.getCloudPushService().deviceId)
            "turnOnPushChannel" -> turnOnPushChannel(result)
            "turnOffPushChannel" -> turnOffPushChannel(result)
            "checkPushChannelStatus" -> checkPushChannelStatus(result)
            "bindAccount" -> bindAccount(call, result)
            "unbindAccount" -> unbindAccount(result)
            "bindTag" -> bindTag(call, result)
            "unbindTag" -> unbindTag(call, result)
            "listTags" -> listTags(call, result)
            "addAlias" -> addAlias(call, result)
            "removeAlias" -> removeAlias(call, result)
            "listAliases" -> listAliases(result)
            "setupNotificationManager" -> setupNotificationManager(call, result)
            "bindPhoneNumber" -> bindPhoneNumber(call, result)
            "unbindPhoneNumber" -> unbindPhoneNumber(result)
            else -> result.notImplemented()
        }

    }

    private fun initPlugin(result: Result) {
        result.success(true)
        initialized = true
        if (initialized && registered) {
            methodChannel?.invokeMethod("initCloudChannelResult", mapOf(
                    "isSuccessful" to true,
                    "response" to "成功"
            ))
        }
        MessageHandler.onInit()
    }


    private fun turnOnPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOnPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun turnOffPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOffPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun checkPushChannelStatus(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.checkPushChannelStatus(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun bindAccount(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindAccount(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun unbindAccount(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindAccount(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    //bindPhoneNumber


    private fun bindPhoneNumber(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindPhoneNumber(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun unbindPhoneNumber(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindPhoneNumber(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun bindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.bindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun unbindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.unbindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun listTags(call: MethodCall, result: Result) {
        val target = call.arguments as Int? ?: 1
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listTags(target, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun addAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.addAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun removeAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.removeAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun listAliases(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listAliases(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }


    private fun setupNotificationManager(call: MethodCall, result: Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = call.arguments as List<Map<String, Any?>>
            val mNotificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannels = mutableListOf<NotificationChannel>()
            for (channel in channels) {
                // 通知渠道的id
                val id = channel["id"] ?: appContext.packageName
                // 用户可以看到的通知渠道的名字.
                val name = channel["name"] ?: appContext.packageName
                // 用户可以看到的通知渠道的描述
                val description = channel["description"] ?: appContext.packageName
                val importance = channel["importance"] ?: NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(id as String, name as String, importance as Int)
                // 配置通知渠道的属性
                mChannel.description = description as String
                mChannel.enableLights(true)
                mChannel.enableVibration(true)
                notificationChannels.add(mChannel)
            }
            if (notificationChannels.isNotEmpty()) {
                mNotificationManager.createNotificationChannels(notificationChannels)
            }
        }
        result.success(true)
    }
}
