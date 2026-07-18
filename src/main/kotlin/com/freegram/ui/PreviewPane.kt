package com.freegram.ui

import com.freegram.converter.*
import com.freegram.format.fdml.FdmlWriter
import com.freegram.model.FdmlDocument
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView

class PreviewPane : TabPane() {

    private val mermaidTab = Tab("Mermaid Preview")
    private val mermaidWebView = WebView()
    private val mermaidTextArea = TextArea()

    private val fdmlTab = Tab("FDML Source")
    private val fdmlTextArea = TextArea()

    private val conversionLogTab = Tab("Conversion Log")
    private val conversionLogArea = TextArea()

    private val conversionManager = ConversionManager()

    init {
        tabMinWidth = 100.0

        val mermaidContent = StackPane()
        mermaidWebView.prefWidth = 600.0
        mermaidWebView.prefHeight = 400.0
        mermaidTextArea.prefRowCount = 5
        mermaidContent.children.addAll(mermaidWebView)
        mermaidTab.content = mermaidContent
        tabs.add(mermaidTab)

        fdmlTab.content = fdmlTextArea
        tabs.add(fdmlTab)

        conversionLogArea.isEditable = false
        conversionLogTab.content = conversionLogArea
        tabs.add(conversionLogTab)
    }

    fun updatePreview(doc: FdmlDocument) {
        updateMermaidPreview(doc)
        updateFdmlPreview(doc)
    }

    private fun updateMermaidPreview(doc: FdmlDocument) {
        try {
            val result = conversionManager.convert(
                FdmlWriter.write(doc),
                Format.FDML,
                Format.MERMAID
            )
            val mermaidCode = result.output

            val html = buildMermaidHtml(mermaidCode)
            mermaidWebView.engine.loadContent(html)

            mermaidTextArea.text = mermaidCode

            val logSb = StringBuilder()
            logSb.appendLine("=== Conversion: FDML → Mermaid ===")
            logSb.appendLine("Compatibility: ${result.compatibility.level}")
            result.compatibility.reasons.forEach { logSb.appendLine("  ⚠ $it") }
            logSb.appendLine()
            logSb.appendLine("--- Mermaid Output ---")
            logSb.appendLine(mermaidCode)
            conversionLogArea.text = logSb.toString()
        } catch (e: Exception) {
            conversionLogArea.text = "Conversion error: ${e.message}"
        }
    }

    private fun updateFdmlPreview(doc: FdmlDocument) {
        try {
            fdmlTextArea.text = FdmlWriter.write(doc)
        } catch (e: Exception) {
            fdmlTextArea.text = "Error: ${e.message}"
        }
    }

    private fun buildMermaidHtml(mermaidCode: String): String {
        val escaped = mermaidCode
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

        return """
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        body { margin: 0; padding: 16px; background: white; }
        .mermaid { text-align: center; }
        .compat-warning {
            background: #fff3cd; border: 1px solid #ffc107;
            padding: 8px; margin: 8px 0; border-radius: 4px; font-family: sans-serif; font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="compat-warning">
        <strong>⚠ Mermaid Compatibility:</strong> This preview shows the Mermaid conversion.
        Extended FDML features (PDF embeds, position info, free text, non-native blocks)
        may not render fully in standard Mermaid renderers.
    </div>
    <pre class="mermaid">$escaped</pre>
    <script>mermaid.initialize({ startOnLoad: true, theme: 'default' });</script>
</body>
</html>
        """.trimIndent()
    }

    fun appendConversionLog(path: String, compatibility: String, reasons: List<String>) {
        val current = conversionLogArea.text
        val sb = StringBuilder(current)
        if (current.isNotEmpty()) sb.appendLine("\n---")
        sb.appendLine("=== $path ===")
        sb.appendLine("Compatibility: $compatibility")
        reasons.forEach { sb.appendLine("  ⚠ $it") }
        conversionLogArea.text = sb.toString()
    }

    fun showMermaidCode(code: String) {
        val html = buildMermaidHtml(code)
        mermaidWebView.engine.loadContent(html)
        mermaidTextArea.text = code
    }
}
