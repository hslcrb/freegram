package com.freegram

import com.freegram.ui.MainWindow
import javafx.application.Application
import javafx.stage.Stage

class Main : Application() {

    override fun start(stage: Stage) {
        stage.title = "FreeGram v1.0"
        val mainWindow = MainWindow(stage)
        mainWindow.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java, *args)
        }
    }
}
