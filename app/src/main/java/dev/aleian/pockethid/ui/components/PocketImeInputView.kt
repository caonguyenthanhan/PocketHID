package dev.aleian.pockethid.ui.components

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImeDiagnostics(
    val lastEvent: String = "IDLE",
    val committedText: String = "",
    val resolvedKeys: String = "None",
    val status: String = "Ready",
    val timestamp: Long = System.currentTimeMillis()
)

object ImeDiagnosticsHub {
    private val _diagnostics = MutableStateFlow(ImeDiagnostics())
    val diagnostics: StateFlow<ImeDiagnostics> = _diagnostics.asStateFlow()

    fun record(event: String, text: String, keys: String, status: String = "Sent") {
        _diagnostics.value = ImeDiagnostics(
            lastEvent = event,
            committedText = text,
            resolvedKeys = keys,
            status = status,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * A dedicated non-text-corrupting Android View that acts as the target for the Android IME.
 * Directly intercepts commitText, deleteSurroundingText, sendKeyEvent, and performEditorAction
 * without relying on EditText buffer clearing, avoiding Gboard / Samsung keyboard desync.
 */
class PocketImeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onCommitText: ((String) -> Unit)? = null
    var onDeleteBack: ((Int) -> Unit)? = null
    var onDeleteForward: ((Int) -> Unit)? = null
    var onEditorAction: ((Int) -> Unit)? = null
    var onKeyEvent: ((KeyEvent) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE or
                EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI

        return PocketInputConnection(this, false)
    }

    override fun onCheckIsTextEditor(): Boolean = true

    fun requestKeyboard() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    private inner class PocketInputConnection(
        targetView: View,
        fullEditor: Boolean
    ) : BaseInputConnection(targetView, fullEditor) {

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (!text.isNullOrEmpty()) {
                val str = text.toString()
                ImeDiagnosticsHub.record("commitText", str, "resolving...")
                onCommitText?.invoke(str)
            }
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (beforeLength > 0) {
                ImeDiagnosticsHub.record("deleteSurroundingText", "-$beforeLength chars", "KEY_BACKSPACE x$beforeLength")
                onDeleteBack?.invoke(beforeLength)
            }
            if (afterLength > 0) {
                ImeDiagnosticsHub.record("deleteSurroundingText", "+$afterLength forward", "KEY_DELETE x$afterLength")
                onDeleteForward?.invoke(afterLength)
            }
            return true
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
            return deleteSurroundingText(beforeLength, afterLength)
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) {
                ImeDiagnosticsHub.record("sendKeyEvent", "keyCode=${event.keyCode}", "resolving...")
                onKeyEvent?.invoke(event)
            }
            return true
        }

        override fun performEditorAction(actionCode: Int): Boolean {
            ImeDiagnosticsHub.record("performEditorAction", "actionCode=$actionCode", "KEY_ENTER")
            onEditorAction?.invoke(actionCode)
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            // Acknowledge composing text without premature emission to prevent duplicate strokes
            return true
        }

        override fun finishComposingText(): Boolean {
            return true
        }
    }
}
