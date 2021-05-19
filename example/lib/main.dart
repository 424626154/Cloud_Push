import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:cloud_push/cloud_push.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
    CloudPush.initCloudChannelResult.listen((data) async {
      print(
          "----------->init successful ${data.isSuccessful} ${data.errorCode} ${data.errorMessage}");
      final deviceId = await CloudPush.deviceId;
      print("----------->device id $deviceId");
    });

    CloudPush.setupNotificationManager(
        [NotificationChannel("id", "name", "description")]);

    CloudPush.onNotification.listen((data) {
      print("----------->notification here ${data.summary}");
      setState(() {
        _platformVersion = data.summary;
      });
    });
    CloudPush.onNotificationOpened.listen((data) {
      print("-----------> ${data.summary} 被点了");
      setState(() {
        _platformVersion = "${data.summary} 被点了";
      });
    });

    CloudPush.onNotificationRemoved.listen((data) {
      print("-----------> $data 被删除了");
    });

    CloudPush.onNotificationReceivedInApp.listen((data) {
      print("-----------> ${data.summary} In app");
    });

    CloudPush.onNotificationClickedWithNoAction.listen((data) {
      print("${data.summary} no action");
    });

    CloudPush.onMessageArrived.listen((data) {
      print("received data -> ${data.content}");
      setState(() {
        _platformVersion = data.content;
      });
    });
//    CloudPush.initCloudChannel( );
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
//    try {
//      platformVersion = await CloudPush.platformVersion;
//    } on PlatformException {
//      platformVersion = 'Failed to get platform version.';
//    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
