package dev.aleian.pockethid.drawing

import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingState
import dev.aleian.pockethid.drawing.model.DrawingStroke
import dev.aleian.pockethid.drawing.model.DrawingTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Controller and state machine for Electronic Drawing Board.
 *
 * Owns stroke lifecycle, bounded Undo/Redo history, eraser hit-testing,
 * tool configurations, and multi-touch single-finger protection.
 * Completely decoupled from Android UI/Compose.
 */
class DrawingController(
    maxHistorySize: Int = DEFAULT_MAX_HISTORY
) {
    companion object {
        const val MAX_HISTORY = 50
        const val DEFAULT_MAX_HISTORY = MAX_HISTORY
        const val DEFAULT_STROKE_WIDTH = 4f
        const val DEFAULT_ERASER_RADIUS = 24f
    }

    private val maxHistory: Int = maxHistorySize

    private val _state = MutableStateFlow(
        DrawingState(
            strokes = emptyList(),
            redoStack = emptyList(),
            activeTool = DrawingTool.PEN,
            selectedColor = DrawingColor.WHITE,
            strokeWidth = DEFAULT_STROKE_WIDTH,
            isClearConfirmationPending = false,
            currentDraftStroke = null
        )
    )
    val state: StateFlow<DrawingState> = _state.asStateFlow()

    /**
     * Begins a drawing stroke with 1 finger.
     * If more than 1 finger is active, the touch is ignored to prevent accidental marks.
     */
    fun startStroke(point: DrawingPoint, pointerCount: Int = 1) {
        if (pointerCount > 1) {
            cancelStroke()
            return
        }

        val currentState = _state.value
        when (currentState.activeTool) {
            DrawingTool.PEN -> {
                val newStroke = DrawingStroke(
                    points = listOf(point),
                    color = currentState.selectedColor,
                    width = currentState.strokeWidth,
                    tool = DrawingTool.PEN
                )
                _state.update { it.copy(currentDraftStroke = newStroke) }
            }
            DrawingTool.ERASER -> {
                eraseAt(point, DEFAULT_ERASER_RADIUS)
            }
        }
    }

    /**
     * Appends a coordinate point to the active stroke.
     * If a second finger touches the screen, the stroke is finished/canceled immediately.
     */
    fun appendPoint(point: DrawingPoint, pointerCount: Int = 1) {
        if (pointerCount > 1) {
            cancelStroke()
            return
        }

        val currentDraft = _state.value.currentDraftStroke
        val currentTool = _state.value.activeTool

        if (currentTool == DrawingTool.PEN && currentDraft != null) {
            // Avoid inserting redundant duplicate points
            val lastPoint = currentDraft.points.lastOrNull()
            if (lastPoint == null || lastPoint.distanceTo(point) >= 1.0f) {
                val updatedPoints = currentDraft.points + point
                _state.update { it.copy(currentDraftStroke = currentDraft.copy(points = updatedPoints)) }
            }
        } else if (currentTool == DrawingTool.ERASER) {
            eraseAt(point, DEFAULT_ERASER_RADIUS)
        }
    }

    /**
     * Finishes and commits the current draft stroke to stroke history.
     */
    fun finishStroke() {
        val draft = _state.value.currentDraftStroke ?: return
        if (draft.points.isNotEmpty()) {
            _state.update { current ->
                val newStrokes = current.strokes + draft
                // Enforce bounded history limit
                val boundedStrokes = if (newStrokes.size > maxHistory) {
                    newStrokes.takeLast(maxHistory)
                } else {
                    newStrokes
                }
                current.copy(
                    strokes = boundedStrokes,
                    redoStack = emptyList(), // Standard drawing behavior: new stroke invalidates redo stack
                    currentDraftStroke = null
                )
            }
        } else {
            _state.update { it.copy(currentDraftStroke = null) }
        }
    }

    /**
     * Cancels the active draft stroke without committing.
     */
    fun cancelStroke() {
        _state.update { it.copy(currentDraftStroke = null) }
    }

    /**
     * Erases strokes intersecting a circular zone centered at [point] with [radius].
     */
    fun eraseAt(point: DrawingPoint, radius: Float = DEFAULT_ERASER_RADIUS) {
        _state.update { current ->
            val remainingStrokes = mutableListOf<DrawingStroke>()
            var anyErased = false

            for (stroke in current.strokes) {
                if (stroke.intersects(point, radius)) {
                    anyErased = true
                } else {
                    remainingStrokes.add(stroke)
                }
            }

            if (anyErased) {
                current.copy(strokes = remainingStrokes, redoStack = emptyList())
            } else {
                current
            }
        }
    }

    /**
     * Undoes the last committed stroke.
     */
    fun undo(): Boolean {
        val current = _state.value
        if (current.strokes.isEmpty()) return false

        val strokeToUndo = current.strokes.last()
        val remainingStrokes = current.strokes.dropLast(1)
        val updatedRedo = (current.redoStack + strokeToUndo).takeLast(maxHistory)

        _state.update {
            it.copy(
                strokes = remainingStrokes,
                redoStack = updatedRedo
            )
        }
        return true
    }

    /**
     * Redoes the last undone stroke.
     */
    fun redo(): Boolean {
        val current = _state.value
        if (current.redoStack.isEmpty()) return false

        val strokeToRestore = current.redoStack.last()
        val remainingRedo = current.redoStack.dropLast(1)
        val updatedStrokes = (current.strokes + strokeToRestore).takeLast(maxHistory)

        _state.update {
            it.copy(
                strokes = updatedStrokes,
                redoStack = remainingRedo
            )
        }
        return true
    }

    /**
     * Requests canvas clear. Sets confirmation pending flag.
     * Does not erase until confirmed.
     */
    fun requestClear() {
        if (_state.value.strokes.isNotEmpty()) {
            _state.update { it.copy(isClearConfirmationPending = true) }
        }
    }

    /**
     * Confirms canvas clear. Empties all strokes.
     */
    fun confirmClear() {
        _state.update {
            it.copy(
                strokes = emptyList(),
                redoStack = emptyList(),
                isClearConfirmationPending = false,
                currentDraftStroke = null
            )
        }
    }

    /**
     * Cancels canvas clear request.
     */
    fun cancelClear() {
        _state.update { it.copy(isClearConfirmationPending = false) }
    }

    /**
     * Sets active drawing tool (Pen or Eraser).
     */
    fun setTool(tool: DrawingTool) {
        _state.update { it.copy(activeTool = tool) }
    }

    /**
     * Sets current stroke color.
     */
    fun setColor(color: DrawingColor) {
        _state.update { it.copy(selectedColor = color) }
    }

    /**
     * Sets stroke width.
     */
    fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width) }
    }
}
