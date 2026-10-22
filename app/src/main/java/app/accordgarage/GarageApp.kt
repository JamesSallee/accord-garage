package app.accordgarage

import android.app.Application
import app.accordgarage.data.Container
import app.accordgarage.work.Reminders

class GarageApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        Reminders.schedule(this)
    }
}
