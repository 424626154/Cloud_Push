# cloud_push

阿里云推送Flutter插件

[阿里云推送官方文档](https://help.aliyun.com/document_detail/51056.html)

## Android 配置

### 配置appKey与appSecret

在`/android/app/build.gradle`设置appKey,appSecret

```gradle
android: {
  ....
  defaultConfig {
    applicationId ""
    manifestPlaceholders = [
        PUSH_APP_KEY   : "",
        PUSH_APP_SECRET: "",
    ]
  }
}
```

> 也可以动态设置，具体方式看官方文档

### 添加调用

阿里云推送SDK限制必须在channel进程也进行SDK初始化调用

所以必须在[Application.onCreate]方法上调用

```java
  CloudPushPlugin.initPushService(this)
```

### 第三方推送通道配置

在`/android/app/build.gradle`设置第三方推送的相关信息

```gradle
android: {
  ....
  defaultConfig {
    applicationId ""
    manifestPlaceholders = [
        // 下面是多厂商配置
        // 如果不需要使用，预留空字段即可
        XIAOMI_APP_ID  : "",
        XIAOMI_APP_KEY : "",
        // 华为APPID不需要添加appid=前缀，内部已经处理
        HUAWEI_APP_ID  : "",
        OPPO_APP_KEY   : "",
        OPPO_APP_SECRET: "",
        VIVO_APP_ID    : "",
        VIVO_APP_KEY   : "",
        MEIZU_APP_ID   : "",
        MEIZU_APP_KEY  : "",
        GCM_SEND_ID    : "",
        GCM_APP_ID     : "",
    ]
  }
}
```

## iOS 配置

### 推送证书配置

参考 [iOS 配置推送证书指南](https://help.aliyun.com/document_detail/30071.html)

### 添加阿里云源

在项目中的`PodFile`前面加上下面的两句话

```ruby
source 'https://github.com/CocoaPods/Specs.git'
source 'https://github.com/aliyun/aliyun-specs.git'
```

### 添加info.plist

请参考 [阿里云推送iOS集成文档](https://help.aliyun.com/document_detail/30072.html) 将 `info.plist` 添加到你的项目中。

### iOS通知栏

如果你想推送通知的时候在通知栏上有显示请确保调用了下面的代码

```dart
  CloudPush.configureNotificationPresentationOption();
```

## 接口

由于SDK特性，请在调用 `init` 方法之前监听调用 `Stream.listen`，
如果在`init`方法之后调用，有可能出现消息漏收现象

### init `→` Future\<dynamic\>

推送SDK初始化

### deviceId `→` Future\<String\>

获取推送SDK生成的设备ID，确保 `initCloudChannelResult` 回调之后调用

### turnOffPushChannel `→` Future\<CommonCallbackResult\>

关闭推送通道，默认状态为开启

### turnOnPushChannel `→` Future\<CommonCallbackResult\>

开启推送通道，默认状态为开启，与 `turnOffPushChannel` 方法配套使用

### bindAccount `→` Future\<CommonCallbackResult\>

|参数名|类型|是否必需|
|-|-|-|
|account|String|是|

将应用内账号和推送通道相关联，可以实现按账号的定点消息推送

### bindPhoneNumber `→` Future\<CommonCallbackResult\>

|参数名|类型|是否必需|
|-|-|-|
|phone|String|是|

将应用内账号和手机号码相关联，可以实现当推送无法到达的时候使用短信通知

### syncBadgeNum `→` Future\<CommonCallbackResult\>

|参数名|类型|是否必需|
|-|-|-|
|num|int|是|

仅iOS调用有效，同步设备当前角标数到推送服务端，配合角标自增功能（参考 OpenAPI 2.0 高级推送接口，搜索iOSBadgeAutoIncrement）使用；

### bindTag `→` Future\<CommonCallbackResult\>

|参数名|类型|是否必需|
|-|-|-|
|tag|String|是|

将应用内账号和 `TAG` 相关联，可以实现按 `TAG` 批量推送

### initCloudChannelResult `→` Stream\<CommonCallbackResult\>

设备成功注册时通过 [Steam] 广播，此时可以调用`deviceId`方法获取推送设备ID

### onMessageArrived `→` Stream\<CloudPushMessage\>

服务端推送的透传消息到达时通过 [Steam] 广播。不会弹出通知栏

### OnNotification `→` Stream\<OnNotification\>

服务端推送的通知消息到达时通过 [Steam] 广播并弹出通知栏

### OnNotificationOpened `→` Stream\<OnNotificationOpened\>

通知消息被点击打开时通过 [Steam] 广播

### onNotificationRemoved `→` Stream\<String\>

通知消息从通知栏被移除时通过 [Steam] 广播
