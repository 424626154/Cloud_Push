package cn.zzstc.cloud_push

import android.content.Context
import android.os.Handler
import android.util.Log
import com.alibaba.sdk.android.push.AliyunMessageIntentService
import com.alibaba.sdk.android.push.notification.CPushMessage

class CloudPushIntentService : AliyunMessageIntentService() {

    override fun onNotificationRemoved(context: Context, messageId: String?) {
        Log.d("CloudPushIntentService", "onNotificationRemoved messageId is $messageId")
        MessageHandler.handleMessage("onNotificationRemoved", messageId ?: "")
    }

    override fun onNotification(context: Context, title: String?, summary: String?, extras: MutableMap<String, String>?) {
        Log.d("CloudPushIntentService", "onNotification title is $title, summary is $summary, extras: $extras")
        MessageHandler.handleMessage("onNotification", mapOf(
                "title" to title,
                "summary" to summary,
                "extras" to extras
        ))
    }

    override fun onMessage(context: Context, message: CPushMessage) {
        Log.d("CloudPushIntentService", "onMessage title is ${message.title}, messageId is ${message.messageId}, content is ${message.content}")
        MessageHandler.handleMessage("onMessageArrived", mapOf(
                "appId" to message.appId,
                "content" to message.content,
                "messageId" to message.messageId,
                "title" to message.title,
                "traceInfo" to message.traceInfo
        ))
    }

    override fun onNotificationOpened(p0: Context?, title: String?, summary: String?, extras: String?) {

        Log.d("CloudPushIntentService", "onNotificationOpened title is $title, summary is $summary, extras: $extras")
        MessageHandler.handleMessage("onNotificationOpened", mapOf(
                "title" to title,
                "summary" to summary,
                "extras" to extras
        ))
    }

    override fun onNotificationReceivedInApp(p0: Context?, title: String?, summary: String?, extras: MutableMap<String, String>?, openType: Int, openActivity: String?, openUrl: String?) {
        Log.d("CloudPushIntentService", "onNotificationReceivedInApp title is $title, summary is $summary, extras: $extras")
        MessageHandler.handleMessage("onNotificationReceivedInApp", mapOf(
                "title" to title,
                "summary" to summary,
                "extras" to extras,
                "openType" to openType,
                "openActivity" to openActivity,
                "openUrl" to openUrl
        ))
    }

    override fun onNotificationClickedWithNoAction(context: Context, title: String?, summary: String?, extras: String?) {
        Log.d("CloudPushIntentService", "onNotificationClickedWithNoAction title is $title, summary is $summary, extras: $extras")
        MessageHandler.handleMessage("onNotificationClickedWithNoAction", mapOf(
                "title" to title,
                "summary" to summary,
                "extras" to extras
        ))
    }
}