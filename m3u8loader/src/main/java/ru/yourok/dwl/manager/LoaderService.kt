package ru.yourok.dwl.manager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.yourok.dwl.utils.Utils
import ru.yourok.m3u8loader.App
import kotlin.concurrent.thread


/**
 * Created by yourok on 19.11.17.
 */
class LoaderService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifyer.createNotification(this)
        startUpdate()
        return START_STICKY
    }

    override fun onDestroy() {
        isUpdates = false
        Notifyer.finishNotification()
        Manager.saveLists()
        super.onDestroy()
    }

    private fun startUpdate() {
        synchronized(isUpdates) {
            if (isUpdates)
                return
            isUpdates = true
        }
        thread {
            while (isUpdates && sendNotification()) {
                Thread.sleep(100)
                App.wakeLock(200)
            }
            stopSelf()
        }
    }

    private fun sendNotification(): Boolean {
        val index = Manager.getCurrentLoader()
        if (index == -1)
            return false
        val state = Manager.getLoaderStat(index) ?: return false


        var percent = 0
        if (state.size > 0)
            percent = (state.loadedFragments * 100 / state.fragments)

        val status = "%d/%d %s/sec %d%%".format(state.loadedFragments, state.fragments, Utils.byteFmt(state.speed), percent)
        return Notifyer.updateNotfication(state.name, status, percent)
    }

    companion object {
        private var isUpdates = false

        fun start() {
            if (isUpdates)
                return
            try {
                val intent = Intent(App.getContext(), LoaderService::class.java)
                App.getContext().startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop() {
            isUpdates = false
        }
    }
}