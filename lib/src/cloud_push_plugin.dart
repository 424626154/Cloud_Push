import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'cloud_push_message.dart';
import 'cloud_push_service_enums.dart';
import 'common_callback_result.dart';

/// 由于SDK特性，请在调用 `init` 方法之前监听调用 `Stream.listen`，
/// 如果在`init`方法之后调用，有可能出现消息漏收现象

final MethodChannel _channel = const MethodChannel('cloud_push')
  ..setMethodCallHandler(_handler);

StreamController<CommonCallbackResult> _initCloudChannelResultController =
    StreamController.broadcast();

StreamController<CloudPushMessage> _onMessageArrivedController =
    StreamController.broadcast();

StreamController<OnNotification> _onNotificationController =
    StreamController.broadcast();

StreamController<OnNotificationOpened> _onNotificationOpenedController =
    StreamController.broadcast();

StreamController<String> _onNotificationRemovedController =
    StreamController.broadcast();

StreamController<OnNotificationClickedWithNoAction>
    _onNotificationClickedWithNoActionController = StreamController.broadcast();

StreamController<OnNotificationReceivedInApp>
    _onNotificationReceivedInAppController = StreamController.broadcast();

Future<dynamic> _handler(MethodCall methodCall) {
  if ("initCloudChannelResult" == methodCall.method) {
    _initCloudChannelResultController.add(CommonCallbackResult(
      isSuccessful: methodCall.arguments["isSuccessful"],
      response: methodCall.arguments["response"],
      errorCode: methodCall.arguments["errorCode"],
      errorMessage: methodCall.arguments["errorMessage"],
    ));
  } else if ("onMessageArrived" == methodCall.method) {
    _onMessageArrivedController.add(CloudPushMessage(
      messageId: methodCall.arguments["messageId"],
      appId: methodCall.arguments["appId"],
      title: methodCall.arguments["title"],
      content: methodCall.arguments["content"],
      traceInfo: methodCall.arguments["traceInfo"],
    ));
  } else if ("onNotification" == methodCall.method) {
    _onNotificationController.add(OnNotification(methodCall.arguments["title"],
        methodCall.arguments["summary"], methodCall.arguments["extras"]));
  } else if ("onNotificationOpened" == methodCall.method) {
    _onNotificationOpenedController.add(OnNotificationOpened(
        methodCall.arguments["title"],
        methodCall.arguments["summary"],
        methodCall.arguments["extras"],
        methodCall.arguments["subtitle"],
        methodCall.arguments["badge"]));
  } else if ("onNotificationRemoved" == methodCall.method) {
    _onNotificationRemovedController.add(methodCall.arguments);
  } else if ("onNotificationClickedWithNoAction" == methodCall.method) {
    _onNotificationClickedWithNoActionController.add(
        OnNotificationClickedWithNoAction(methodCall.arguments["title"],
            methodCall.arguments["summary"], methodCall.arguments["extras"]));
  } else if ("onNotificationReceivedInApp" == methodCall.method) {
    _onNotificationReceivedInAppController.add(OnNotificationReceivedInApp(
        methodCall.arguments["title"],
        methodCall.arguments["summary"],
        methodCall.arguments["extras"],
        methodCall.arguments["openType"],
        methodCall.arguments["openActivity"],
        methodCall.arguments["openUrl"]));
  }

  return Future.value(true);
}

class CloudPush {
  /// 设备成功注册时通过 [Steam] 广播，此时可以调用`deviceId`方法获取推送设备ID
  static Stream<CommonCallbackResult> get initCloudChannelResult =>
      _initCloudChannelResultController.stream;

  /// 服务端推送的透传消息到达时通过 [Steam] 广播。不会弹出通知栏
  static Stream<CloudPushMessage> get onMessageArrived =>
      _onMessageArrivedController.stream;

  /// 服务端推送的通知消息到达时通过 [Steam] 广播并弹出通知栏
  static Stream<OnNotification> get onNotification =>
      _onNotificationController.stream;

  /// 通知消息被点击打开时通过 [Steam] 广播
  static Stream<OnNotificationOpened> get onNotificationOpened =>
      _onNotificationOpenedController.stream;

  /// 通知消息从通知栏被移除时通过 [Steam] 广播
  static Stream<String> get onNotificationRemoved =>
      _onNotificationRemovedController.stream;

  static Stream<OnNotificationClickedWithNoAction>
      get onNotificationClickedWithNoAction =>
          _onNotificationClickedWithNoActionController.stream;

  static dispose() {
    _initCloudChannelResultController.close();
    _onMessageArrivedController.close();
    _onNotificationController.close();
    _onNotificationRemovedController.close();
    _onNotificationClickedWithNoActionController.close();
    _onNotificationReceivedInAppController.close();
  }

  static Stream<OnNotificationReceivedInApp> get onNotificationReceivedInApp =>
      _onNotificationReceivedInAppController.stream;

  ///获取推送SDK生成的设备ID，确保 `initCloudChannelResult` 回调之后调用
  static Future<String> get deviceId async {
    return _channel.invokeMethod("deviceId");
  }

  /// 推送SDK初始化
  static Future<dynamic> init() {
    return _channel.invokeMethod("init");
  }

  static Future<CommonCallbackResult> get pushChannelStatus async {
    var result = await _channel.invokeMethod("checkPushChannelStatus");

    return CommonCallbackResult(
      isSuccessful: result["isSuccessful"],
      response: result["response"],
      errorCode: result["errorCode"],
      errorMessage: result["errorMessage"],
    );
  }

  /// 开启推送通道，默认状态为开启，与 `turnOffPushChannel` 方法配套使用
  static Future<CommonCallbackResult> turnOnPushChannel() async {
    var result = await _channel.invokeMethod("turnOnPushChannel");

    return CommonCallbackResult(
      isSuccessful: result["isSuccessful"],
      response: result["response"],
      errorCode: result["errorCode"],
      errorMessage: result["errorMessage"],
    );
  }

  /// 关闭推送通道，默认状态为开启
  static Future<CommonCallbackResult> turnOffPushChannel() async {
    var result = await _channel.invokeMethod("turnOffPushChannel");
    return CommonCallbackResult(
      isSuccessful: result["isSuccessful"],
      response: result["response"],
      errorCode: result["errorCode"],
      errorMessage: result["errorMessage"],
    );
  }

  /// 仅iOS调用有效
  /// 同步设备当前角标数到推送服务端，配合角标自增功能（参考 OpenAPI 2.0 高级推送接口，搜索iOSBadgeAutoIncrement）使用；
  ///
  static Future<CommonCallbackResult> syncBadgeNum(int num) async {
    assert(num != null);
    var result = await _channel.invokeMethod("syncBadgeNum", num);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///将应用内账号和推送通道相关联，可以实现按账号的定点消息推送；
  ///设备只能绑定一个账号，同一账号可以绑定到多个设备；
  ///同一设备更换绑定账号时无需进行解绑，重新调用绑定账号接口即可生效；
  ///若业务场景需要先解绑后绑定，在解绑账号成功回调中进行绑定绑定操作，以此保证执行的顺序性；
  ///账户名设置支持64字节。
  static Future<CommonCallbackResult> bindAccount(String account) async {
    assert(account != null);

    var result = await _channel.invokeMethod("bindAccount", account);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  static Future<CommonCallbackResult> unbindAccount() async {
    var result = await _channel.invokeMethod("unbindAccount");
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  /// 将应用内账号和手机号码相关联，可以实现当推送无法到达的时候使用短信通知
  static Future<CommonCallbackResult> bindPhoneNumber(
      String phoneNumber) async {
    var result = await _channel.invokeMethod("bindPhoneNumber", phoneNumber);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  static Future<CommonCallbackResult> unbindPhoneNumber() async {
    var result = await _channel.invokeMethod("unbindPhoneNumber");
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///将应用内账号和 `TAG` 相关联，可以实现按 `TAG` 批量推送
  ///支持向设备、账号和别名绑定标签，绑定类型由参数target指定；
  ///绑定标签在10分钟内生效；
  ///App最多支持绑定1万个标签，单个标签最大支持128字符。
  ///target 目标类型，1：本设备； 2：本设备绑定账号； 3：别名
  ///target(V2.3.5及以上版本) 目标类型，CloudPushService.DEVICE_TARGET：本设备； CloudPushService.ACCOUNT_TARGET：本账号； CloudPushService.ALIAS_TARGET：别名
  ///tags 标签（数组输入）
  ///alias 别名（仅当target = 3时生效）
  ///callback 回调
  static Future<CommonCallbackResult> bindTag(
      {@required CloudPushServiceTarget target,
      List<String> tags,
      String alias}) async {
    var result = await _channel.invokeMethod("bindTag", {
      "target": target.index + 1,
      "tags": tags ?? List<String>(),
      "alias": alias
    });

    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///解绑指定目标标签；
  ///支持解绑设备、账号和别名标签，解绑类型由参数target指定；
  ///解绑标签在10分钟内生效；
  ///解绑标签只是解除设备和标签的绑定关系，不等同于删除标签，即该APP下标签仍然存在，系统目前不支持标签的删除。
  ///target 目标类型，1：本设备； 2：本设备绑定账号； 3：别名
  ///target(V2.3.5及以上版本) 目标类型，CloudPushService.DEVICE_TARGET：本设备； CloudPushService.ACCOUNT_TARGET：本账号； CloudPushService.ALIAS_TARGET：别名
  ///tags 标签（数组输入）
  ///alias 别名（仅当target = 3时生效）
  ///callback 回调
  static Future<CommonCallbackResult> unbindTag(
      {@required CloudPushServiceTarget target,
      List<String> tags,
      String alias}) async {
    var result = await _channel.invokeMethod("unbindTag", {
      "target": target.index + 1,
      "tags": tags ?? List<String>(),
      "alias": alias
    });

    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///查询目标绑定标签，当前仅支持查询设备标签；
  ///查询结果可从回调onSuccess(response)的response获取；
  ///标签绑定成功且生效（10分钟内）后即可查询。
  static Future<CommonCallbackResult> listTags(
      CloudPushServiceTarget target) async {
    var result = await _channel.invokeMethod("listTags", target.index + 1);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///添加别名
  ///设备添加别名；
  ///单个设备最多添加128个别名，且同一别名最多添加到128个设备；
  ///别名支持128字节。
  static Future<CommonCallbackResult> addAlias(String alias) async {
    var result = await _channel.invokeMethod("addAlias", alias);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///删除别名
  ///删除设备别名；
  ///支持删除指定别名和删除全部别名（alias = null || alias.length = 0）。
  static Future<CommonCallbackResult> removeAlias(String alias) async {
    var result = await _channel.invokeMethod("removeAlias", alias);
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///查询设备别名；
  ///查询结果可从回调onSuccess(response)的response中获取；
  ///从V3.0.9及以上版本开始，接口内部有5s短缓存，5s内多次调用只会请求服务端一次。
  static Future<CommonCallbackResult> listAliases() async {
    var result = await _channel.invokeMethod("listAliases");
    return CommonCallbackResult(
        isSuccessful: result["isSuccessful"],
        response: result["response"],
        errorCode: result["errorCode"],
        errorMessage: result["errorMessage"],
        iosError: result["iosError"]);
  }

  ///这个方法只对android有效
  ///最好调用这个方法以保证在Android 8以上推送通知好用。
  ///如果不调用这个方法，请确认你自己创建了NotificationChannel。
  ///为了更好的用户体验，一些参数请不要用传[null]。
  ///[id]一定要和后台推送时候设置的通知通道一样，否则Android8.0以上无法完成通知推送。
  static Future setupNotificationManager(
      List<NotificationChannel> channels) async {
    return _channel.invokeMethod(
        "setupNotificationManager", channels.map((e) => e.toJson()).toList());
  }

  ///这个方法仅针对iOS
  ///设置推送通知显示方式
  ///    completionHandler(UNNotificationPresentationOptionSound | UNNotificationPresentationOptionAlert | UNNotificationPresentationOptionBadge);
  static Future configureNotificationPresentationOption(
      {bool none: false,
      bool sound: true,
      bool alert: true,
      bool badge: true}) async {
    return _channel.invokeMethod("configureNotificationPresentationOption",
        {"none": none, "sound": sound, "alert": alert, "badge": badge});
  }
}

class NotificationChannel {
  const NotificationChannel(this.id, this.name, this.description,
      {this.importance = AndroidNotificationImportance.DEFAULT});
  final String id;
  final String name;
  final String description;
  final AndroidNotificationImportance importance;

  Map<String, dynamic> toJson() {
    return {
      "id": id,
      "name": name,
      "description": description,
      "importance": importance.index + 1
    };
  }
}
