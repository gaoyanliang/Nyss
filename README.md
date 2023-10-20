# 南石医院 Android app

Nsyy App 需要提供的功能

> 需求功能：
> 1. app 中内嵌 南石OA系统
> 2. 提供获取系统信息的接口，目前实现：获取当前位置，推送系统通知
> 3. 要求 app 能够常驻后台，并实现自启动

1. app 中内嵌浏览器（访问固定内容，南石OA系统？）
2. 提供系统功能
   1. 获取当前位置
   2. 发送消息通知
   3. 支持连接蓝牙秤
   4. 支持扫码 （直接扫码 / 从相册扫码）
3. 要求 app 能够常驻后台，并实现自启动


## 熟悉 Android 开发

通过下面文章中的案例可以简单了解 Android 开发的工具使用，开发流程等。并且快速开发一个Android App Demo

[开发你的第一个 Android 应用](https://mp.weixin.qq.com/s/FIHUdAl2Wl80_BLZ2EB4Jw)

## 功能一： 内嵌浏览器，访问 OA 系统

Android 中可以通过 WebView 来加载指定网站，使用 WebView 可以实现功能一

下面是一个使用 web view 加载指定网站的 Demo

[安卓 WebView 套壳应用 Demo](https://gitee.com/ichenpipi/android-webview-demo/blob/master/app/src/main/java/com/example/webviewdemo/MainActivity.java)

当网站内容更新时，可以通过下拉操作来进行刷新，具体实现可参考：

[下拉刷新Webview](https://www.cnblogs.com/felixwan/p/17292415.html)

**遇到的问题：**

使用 web view 时可以遇到 http 地址访问报错（net::ERR_CLEARTEXT_NOT_PERMITTED）

**解决方案：**[https://blog.csdn.net/geofferysun/article/details/88575504](https://blog.csdn.net/geofferysun/article/details/88575504)


## 功能二：提供系统功能

### 2.0 引入 AndServer 提供 web server 能力

这个需求需要提供的几种能力，需要 App 像 Web 应用一样提供接口供 OA 系统调用

要想实现预期的想过，需要在 App 中启动 web server 暴露指定端口才行，通过调研 [AndServer](https://yanzhenjie.com/AndServer/) 可以满足要求

当然还有其他第三方工具，也可以实现，但是都比较老，在 Github 上不太活跃，并没有采用。

[在 Android 项目中引入 AndServer:](https://blog.csdn.net/Deep_rooted/article/details/124764731)

```
# Project build.gradle
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath 'com.yanzhenjie.andserver:plugin:2.1.12'
    }
}

# Module build.gradle
apply plugin: 'com.yanzhenjie.andserver'
        
// AndServer： https://github.com/yanzhenjie/AndServer
implementation 'com.yanzhenjie.andserver:api:2.1.12'
annotationProcessor 'com.yanzhenjie.andserver:processor:2.1.12'
```


1. 在项目中引入依赖

https://blog.csdn.net/Deep_rooted/article/details/124764731

```xml
# Project build.gradle
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath 'com.yanzhenjie.andserver:plugin:2.1.12'
    }
}


# Module build.gradle
apply plugin: 'com.yanzhenjie.andserver'


    // AndServer： https://github.com/yanzhenjie/AndServer
    implementation 'com.yanzhenjie.andserver:api:2.1.12'
    annotationProcessor 'com.yanzhenjie.andserver:processor:2.1.12'

```

2. 提供 services.NsServerService 用于启动 AndServer 服务
3. 将 NsServerService 注册

```xml AndroidManifest.xml
        <service
            android:name=".service.NsServerService"
            android:enabled="true"
            android:exported="false" />
```

4. 在 MainActivity 中启动 NsServerService

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 启动 AndServer
        startService(new Intent(this, NsServerService.class));
        
    }
```

这样就可以在 Android 中启动一个 webserver。 之后就可以通过 controller 暴露接口

```java
@RestController
public class NsyyController {

    @GetMapping("/test")
    public String ping() {
        return "SERVER OK";
    }

    @GetMapping("/location")
    public String location() {
        try {
            return LocationServices.getInstance().location();
        } catch (Exception e) {
            return "Failed to get location: Please enable location service first";
        }
    }

    @GetMapping("/notification")
    public void notification() {
        try {
            NotificationServices.getInstance().notification();
        } catch (Exception e) {

        }
    }
}
```

> 该功能中添加 NsyyServerBroadcastReceiver 用于监控 web server 服务状态。 该功能可选，也可以不加


使用 AndServer 过程中遇到的一些问题

locationManager.requestLocationUpdates()报Can't create handler inside thread that has not called Looper.prepare()

[https://www.jianshu.com/p/c9a6c73ed5ce](https://www.jianshu.com/p/c9a6c73ed5ce)

[https://stackoverflow.com/questions/9033337/requestlocationupdates-gives-error-cant-create-handler-inside-thread-that-has](https://stackoverflow.com/questions/9033337/requestlocationupdates-gives-error-cant-create-handler-inside-thread-that-has)


[Android中一个类的方法调用Activity中的方法](https://www.cnblogs.com/changyiqiang/p/14486015.html)

该功能主要实现在 [web Server](./app/src/main/java/com/example/nsyy/service)

对 OA 暴露的接口在 [Controller](./app/src/main/java/com/example/nsyy/server)

**消息转换器 MessageConverter**

在使用 Andserver 提供的 web server 服务时，需要提供一个 MessageConverter 用来实现 "服务端 -> 客户端" & "客户端 -> 服务端" 消息的转换，否则服务端接收不到客户端发送的内容。

- https://yanzhenjie.com/AndServer/annotation/RequestBody.html
- https://yanzhenjie.com/AndServer/class/MessageConverter.html

### 2.1 Android 权限申请

要使用 Android 系统级能力，如位置，消息通知，蓝牙，相册等，需要申请权限。通过调研发现 [XXPermissions](https://github.com/getActivity/XXPermissions) 比较合适。

以上集中系统能力，统一通过 [XXPermissions](https://github.com/getActivity/XXPermissions) 来申请权限。

接入文档： https://github.com/getActivity/XXPermissions


### 2.2 获取位置信息

该功能主要实现在 [LocationUtil](./app/src/main/java/com/example/nsyy/utils/LocationUtil.java)

功能实现参考以下文章：

- [https://www.jianshu.com/p/87e0dec25071](https://www.jianshu.com/p/87e0dec25071)
- [https://juejin.cn/post/7108726185550413831](https://juejin.cn/post/7108726185550413831)
- [https://blog.51cto.com/u_15880918/5859937](https://blog.51cto.com/u_15880918/5859937)
- [https://www.runoob.com/w3cnote/android-tutorial-gps.html](https://www.runoob.com/w3cnote/android-tutorial-gps.html)
- [https://medium.com/@grudransh1/best-way-to-get-users-location-in-android-app-using-location-listener-from-java-in-android-studio-77882f8b87fd](https://medium.com/@grudransh1/best-way-to-get-users-location-in-android-app-using-location-listener-from-java-in-android-studio-77882f8b87fd)

**遇到的问题**

通过如下方式获取位置（Location）时，有可能返回的地址为空

```java
Location location = locationManager.getLastKnownLocation(bestProvider);
```

**解决方案：**

[https://medium.com/@grudransh1/best-way-to-get-users-location-in-android-app-using-location-listener-from-java-in-android-studio-77882f8b87fd](https://medium.com/@grudransh1/best-way-to-get-users-location-in-android-app-using-location-listener-from-java-in-android-studio-77882f8b87fd)

[https://blog.51cto.com/u_14125/6537964](https://blog.51cto.com/u_14125/6537964)

### 2.3 消息通知

功能实现参考：

- https://bbs.huaweicloud.com/blogs/362305
- https://developer.android.com/guide/topics/ui/notifiers/notifications?hl=zh_cn#icon-badge
- https://developer.android.com/training/notify-user/build-notification?hl=zh-cn#add_the_support_library

获取通知栏权限时，需要进行版本适配，具体可参考：

- [Android获取应用通知栏权限，并跳转通知设置页面（全版本适配）](https://blog.csdn.net/aiynmimi/article/details/102740139)

该功能主要实现在 [notification](./app/src/main/java/com/example/nsyy/notification)

### 2.4 使用蓝牙连接蓝牙秤

Android 实现的蓝牙连接电子秤的功能比较简单，通过蓝牙秤的 Mac 地址来寻找电子秤并进行连接，连接成功之后通过向电子秤发送 “R” 来获取电子秤的重量。

在 IOS 中已实现通过配置来实现 app 启动自动连接指定蓝牙秤。

Android 中暂时没有做到和 IOS 同样的想过，主要因为目前蓝牙秤功能主要供 医废 使用，而 医废 主要使用 IOS 设备。 Android 中暂时用不到，暂时不用修改。

参考文章：

- [https://github.com/kellysong/DeviceConnector](https://github.com/kellysong/DeviceConnector)
- [https://blog.csdn.net/weixin_41101173/article/details/116308853](https://blog.csdn.net/weixin_41101173/article/details/116308853)
- [https://zhuanlan.zhihu.com/p/608566777](https://zhuanlan.zhihu.com/p/608566777)
- [https://juejin.cn/post/6955012421522030623?searchId=20230916145931BF8960487BC6E980452A](https://juejin.cn/post/6955012421522030623?searchId=20230916145931BF8960487BC6E980452A)
- [https://www.an.rustfisher.com/android/connectivity/bluetooth/Bluetooth2_use_sample/#discovering-devices](https://www.an.rustfisher.com/android/connectivity/bluetooth/Bluetooth2_use_sample/#discovering-devices)
- [https://juejin.cn/post/7225552757607153721](https://juejin.cn/post/7225552757607153721)

该功能主要实现在 [buletooth](./app/src/main/java/com/example/nsyy/utils/BlueToothUtil.java)

### 2.5 扫码

扫码功能的实现主要通过接入 华为-统一扫码服务 来实现的。

接入华为统一扫码服务：https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-dev-process-0000001050043953

该功能主要实现在 [code_scan](./app/src/main/java/com/example/nsyy/code_scan)

华为提供4种调用方式，可以根据需求选择相应的调用方式构建扫码功能。

| 调用方式 | 扫码流程 | 扫码界面 | 功能  |
| --- | --- | --- | --- |
| Default View Mode | Scan Kit处理 | Scan Kit提供 | 相机扫码、导入图片扫码。 |
| Customized View Mode | Scan Kit处理 | 您自行定义 | 相机扫码（可以叠加Bitmap Mode增加导入图片扫码功能）。 |
| Bitmap Mode | 您的应用处理 | 您自行定义 | 相机扫码、导入图片扫码，支持同时检测多个码。 |
| MultiProcessor Mode | 您的应用处理 | 您自行定义 | 相机扫码、导入图片扫码，支持同时检测多个码。 |

当前项目中实现的第二种：Customized View Mode

```java
    @JavascriptInterface
    public void scanCode(){
        // 多种模式可选： https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-overview-0000001050282308
        //loadScanKitBtnClick();
        newViewBtnClick();
        //multiProcessorSynBtnClick();
        //multiProcessorAsynBtnClick();
    }
```


## APP 保活 & 自启动

Android 中由于太过开放，并没有提供统一的，可靠的保活&自启动实现，只能通过不同的方式来尽力实现，并不保证在所有设备上的效果相同。

参考文章：

- [https://github.com/LiuWeiQiu/AutoStartAndKeepAlive](https://github.com/LiuWeiQiu/AutoStartAndKeepAlive)
- [https://blog.51cto.com/u_16099299/6448541](https://blog.51cto.com/u_16099299/6448541)
- [https://www.955code.com/23122.html](https://www.955code.com/23122.html)
- [https://juejin.cn/post/7205508989160734775](https://juejin.cn/post/7205508989160734775)


## 定时任务

https://developer.aliyun.com/article/684728


## Android 打包

[https://blog.csdn.net/qq_38436214/article/details/112288954](https://blog.csdn.net/qq_38436214/article/details/112288954)
