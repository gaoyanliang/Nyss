# 南石医院 Android app

> 需求功能：
> 1. app 中内嵌 南石OA系统
> 2. 提供获取系统信息的接口，目前实现：获取当前位置，推送系统通知
> 3. 要求 app 能够常驻后台，并实现自启动

## 功能一 内嵌 OA 系统

利用 Android WebView 实现内嵌南石OA系统的功能

### 开启权限

该功能需要开启以下权限，否则访问时会出现 net::ERR_CLEARTEXT_NOT_PERMITTED。

具体可参考：https://blog.csdn.net/geofferysun/article/details/88575504

```xml AndroidManifest.xml
<!-- 允许程序打开网络套接字 -->
<uses-permission android:name="android.permission.INTERNET" />
```

并且需要进行如下配置

res 下新建 xml 目录，创建文件：network_security_config.xml ，内容如下：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

在 AndroidManifest.xml 的 application 标签添加配置：

```xml
<manifest ...>
<application>
...
android:networkSecurityConfig="@xml/network_security_config"
...>
...
</application>
</manifest>
```

### 下拉刷新 webview

1.添加支持库依赖项：在 build.gradle 文件中添加以下行以添加 SwipeRefreshLayout 支持库：

```xml
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
```

2.在布局文件中添加 SwipeRefreshLayout 和 WebView：

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
xmlns:android="http://schemas.android.com/apk/res/android"
android:id="@+id/swipe_refresh_layout"
android:layout_width="match_parent"
android:layout_height="match_parent">

    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

3.缺少必要的权限：如果您的应用程序需要访问 Internet 权限，请确保在 AndroidManifest.xml 文件中添加以下行：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

完整案例可参考：https://www.cnblogs.com/felixwan/p/17292415.html

## 功能二 提供获取系统信息接口

### 通过 AndServer 在 Android 端启动 web server

AndServer： https://github.com/yanzhenjie/AndServer

该功能主要用于向前端提供功能接口（获取位置，推送消息通知）

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

### 获取位置

获取位置需要以下几种权限

```xml
    <!-- ================ 获取位置需要的权限 ================ -->
    <!-- 请求大致位置 -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <!-- 请求确切位置 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <!--后台位置信息 Android 10 (API level 29) 或更高版本 -->
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

通过 utils.PermissionUtil 获取相关权限

通过 utils.LocationUtil 获取位置

> 注意：通过 LocationUtil 获取位置时，值有可能为空（为空时返回的 unknown address）,这是正常的。需要多访问几次

### 推送消息通知

功能实现参考：

- https://bbs.huaweicloud.com/blogs/362305
- https://developer.android.com/guide/topics/ui/notifiers/notifications?hl=zh_cn#icon-badge
- https://developer.android.com/training/notify-user/build-notification?hl=zh-cn#add_the_support_library

获取通知栏权限时，需要进行版本适配，具体可参考：

- [Android获取应用通知栏权限，并跳转通知设置页面（全版本适配）](https://blog.csdn.net/aiynmimi/article/details/102740139)


### 消息转换器 MessageConverter

在使用 Andserver 提供的 web server 服务时，需要提供一个 MessageConverter 用来实现 "服务端 -> 客户端" & "客户端 -> 服务端" 消息的转换，否则服务端接收不到客户端发送的内容。

- https://yanzhenjie.com/AndServer/annotation/RequestBody.html
- https://yanzhenjie.com/AndServer/class/MessageConverter.html

## APP 保活



