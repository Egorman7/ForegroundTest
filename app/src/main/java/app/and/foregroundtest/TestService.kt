package app.and.foregroundtest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class TestService : Service(){
    companion object{
        fun getIntent(context: Context, count: Int): Intent{
            return Intent(context, TestService::class.java)
                .putExtra(KEY_COUNT, count)
        }

        private const val KEY_COUNT = "count";
        private const val NOTIFICATION_ID = 14124;
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    private val builder by lazy {notificationBuilder()}
//    private val wakeLock by lazy {
//        (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName.WAKELOCK")
//    }
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName.wakelock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE", "flags: $flags, startId: $startId")
        intent?.also { safeIntent ->
            val count = safeIntent.getIntExtra(KEY_COUNT, 10)
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE)) as NotificationManager
            wakeLock.acquire((count+5) * 1000L)
            startForeground(NOTIFICATION_ID, builder.build())
            manager.notify(NOTIFICATION_ID, builder.setProgress(count, 0, false).build())
            TaskRunner(count).run { i ->
                Log.d("SERVICE", "tag : $i")
                if(i == -1){
                    manager.notify(NOTIFICATION_ID, builder.setProgress(0, 0, false).build())
                    stopForeground(false)
                    wakeLock.release()
//                    stopSelf()
                } else {
                    manager.notify(NOTIFICATION_ID, builder.setContentText("Count: $i").setProgress(count, i, false).build())
                }
            }
        }
        return START_STICKY
    }

    private fun notificationBuilder(): NotificationCompat.Builder{
        return NotificationCompat.Builder(this, getChannelId())
            .setContentTitle("Counter!")
            .setContentText("Count: -")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setTicker(resources.getString(R.string.app_name))
            .setLargeIcon(Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round), 128, 128, false))
    }

    private fun getChannelId(): String{
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel("mychannel", "testName", NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                enableVibration(true)
                lightColor = Color.RED
            })
        }
        return "mychannel"
    }

    private class TaskRunner(val count: Int){
        fun run(listener: ((Int) -> Unit)){
            Thread(Runnable {
                var i = 0;
                postCount(i, listener)
                while(i < count){
                    Thread.sleep(1000)
                    i++;
                    postCount(i, listener)
                }
                Thread.sleep(1000)
                postCount(-1, listener)
            }).start()
        }

        private fun postCount(counter: Int, listener: (Int) -> Unit){
            Handler(Looper.getMainLooper()).post {
                listener(counter);
            }
        }
    }
}