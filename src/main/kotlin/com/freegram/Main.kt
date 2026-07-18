package com.freegram

import com.freegram.ui.MainWindow
import javafx.application.Application
import javafx.scene.text.Font
import javafx.stage.Stage
import java.io.File

class Main : Application() {

    override fun init() {
        loadKoreanFonts()
    }

    override fun start(stage: Stage) {
        stage.title = "FreeGram v1.0"
        val mainWindow = MainWindow(stage)
        mainWindow.show()
    }

    private fun loadKoreanFonts() {
        val fontPaths = listOf(
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
            "/usr/share/fonts/truetype/d2coding/D2Coding.ttf"
        )
        for (path in fontPaths) {
            val file = File(path)
            if (file.exists()) {
                try {
                    Font.loadFont(file.toURI().toURL().toExternalForm(), 14.0)
                } catch (_: Exception) {}
            }
        }

        val fontDir = File("/usr/share/fonts")
        if (fontDir.exists()) {
            fontDir.walkTopDown()
                .filter { it.extension in setOf("ttf", "ttc", "otf") }
                .forEach { file ->
                    try {
                        Font.loadFont(file.toURI().toURL().toExternalForm(), 14.0)
                    } catch (_: Exception) {}
                }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java, *args)
        }
    }
}
