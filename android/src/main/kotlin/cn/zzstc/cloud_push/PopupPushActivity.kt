package cn.zzstc.cloud_push

import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import com.alibaba.sdk.android.push.AndroidPopupActivity
import org.json.JSONObject


class PopupPushActivity : AndroidPopupActivity() {

    override fun onSysNoticeOpened(title: String, summary: String, extras: MutableMap<String, String>) {
        Log.d("PopupPushActivity", "onSysNoticeOpened, title: $title, content: $summary, extMap: $extras")
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
        var jsonExtras = JSONObject()
        for (key in extras.keys) {
            jsonExtras.putOpt(key, extras[key])
        }
        Log.d("PopupPushActivity", "onSysNoticeOpened extras: ${jsonExtras.toString()}")
        MessageHandler.handleMessage("onNotificationOpened", mapOf(
                "title" to title,
                "summary" to summary,
                "extras" to jsonExtras.toString()
        ))
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }


}