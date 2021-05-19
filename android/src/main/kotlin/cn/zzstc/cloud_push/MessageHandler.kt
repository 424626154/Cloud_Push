package cn.zzstc.cloud_push

import android.os.Handler
import android.os.Looper
import java.util.*

class MessageHandler {
    companion object {

        private val handler: Handler = Handler(Looper.getMainLooper())

        /**
         * 推送消息队列
         */
        private val messageQueue: MutableList<Map<String, Any?>> = Collections.synchronizedList(LinkedList())

        fun handleMessage(method: String, message: Any) {
            synchronized(messageQueue) {
                // 此时插件初始化完成：直接通过插件渠道发送推送消息
                // 此时插件未初始化完成：存入消息队列，等待初始化完成调用
                if (CloudPushPlugin.initialized) {
                    handler.post {
                        CloudPushPlugin.methodChannel?.invokeMethod(
                                method, message
                        )
                    }
                } else {
                    messageQueue.add(
                            mapOf(
                                    "method" to method,
                                    "message" to message
                            )
                    )
                }
            }
        }

        fun onInit() {
            synchronized(messageQueue) {
                val i: MutableIterator<Map<String, Any?>> = messageQueue.iterator()
                while (i.hasNext()) {
                    val msg = i.next()
                    handler.post {
                        CloudPushPlugin.methodChannel?.invokeMethod(
                                msg["method"] as String, msg["message"]
                        )
                    }
                }
                messageQueue.clear()
            }
        }
    }
}