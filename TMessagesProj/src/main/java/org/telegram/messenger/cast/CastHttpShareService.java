package org.telegram.messenger.cast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CastHttpShareService extends Service {

    private static final int SERVICE_ID = 654;
    private static final String CHANNEL_ID = "CastHttpService_Notifications";
    private static final String CHANNEL_NAME = "Telegram Cast Foreground Service";
    private static final int PORT = 4747;
    private String wifiIP = null;

    private ExecutorService executorService;
    private static CastShareServer castShareServer;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Objects.equals(intent.getAction(), START)) {
                executorService.submit(this::start);
                startForegroundWithNotification();
                return START_STICKY;
            } else if (Objects.equals(intent.getAction(), STOP)) {
                executorService.submit(this::stop);
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    private void startForegroundWithNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(LocaleController.getString(R.string.CastAndroidServiceName)).setContentText(LocaleController.getString(R.string.CastAndroidServiceRunning)).setSmallIcon(R.drawable.ic_cast);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(SERVICE_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(SERVICE_ID, builder.build());
            }
        } catch (Exception e) {
            FileLog.e("[CastService] startForegroundWithNotification: Unable to display persistent notification", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.submit(this::stop);
        executorService.shutdown();
        executorService = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        if (wifiIP == null) {
            wifiIP = getWiFiIP();
        }
        if (wifiIP == null) {
            FileLog.w("[CastHttpService] Service not started: Null Wi-Fi IP");
            return;
        }
        try {
            CastShareServer.stopServer();
        } catch (Exception ignored) {
        }
        try {
            castShareServer = new CastShareServer(wifiIP, PORT);
            castShareServer.runServer();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void stop() {
        CastShareServer.stopServer();
    }

    private String getWiFiIP() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(wifiInfo.getIpAddress()).array()).getHostAddress();
        } catch (UnknownHostException e) {
            FileLog.e(e);
            return null;
        }
    }

    private static final String START = "CastHTTPService::start";
    private static final String STOP = "CastHTTPService::stop";

    public static void startService() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        context.startService(new Intent(context, CastHttpShareService.class).setAction(START));
    }

    public static void stopService() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        context.startService(new Intent(context, CastHttpShareService.class).setAction(STOP));
    }

    public static boolean isStarted() {
        return castShareServer != null && castShareServer.isAlive();
    }
}
