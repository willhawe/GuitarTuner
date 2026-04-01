package com.whawe.guitartuner.scale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.whawe.guitartuner.R

class ScaleStaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private data class StaffLayout(
        val staffLeft: Float,
        val staffRight: Float,
        val stepSize: Float,
        val bottomReferenceY: Float
    ) {
        fun yForStep(step: Int): Float {
            return bottomReferenceY - step * stepSize
        }
    }

    private val staffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.panel_stroke)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }
    private val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = sp(18f)
        textAlign = Paint.Align.CENTER
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.panel_stroke)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }

    private val noteHeadWidth = dp(22f)
    private val noteHeadHeight = dp(15f)
    private val stemLength = dp(34f)
    private val sidePadding = dp(28f)
    private val topPadding = dp(28f)
    private val bottomPadding = dp(28f)
    private val ledgerWidth = dp(28f)

    private var scale: MusicalScale = ScaleLibrary.buildScale(RootNote.A, ScaleType.MINOR_PENTATONIC)
    private var playedPitchClasses: Set<Int> = emptySet()
    private var currentPitchClass: Int? = null

    fun setScale(scale: MusicalScale) {
        this.scale = scale
        invalidate()
    }

    fun setHighlightState(playedPitchClasses: Set<Int>, currentPitchClass: Int?) {
        this.playedPitchClasses = playedPitchClasses.mapTo(mutableSetOf()) { normalizePitchClass(it) }
        this.currentPitchClass = currentPitchClass?.let(::normalizePitchClass)
        invalidate()
    }

    fun clearHighlights() {
        playedPitchClasses = emptySet()
        currentPitchClass = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scale.notes.isEmpty()) {
            return
        }

        val layout = buildStaffLayout()

        for (line in 0..4) {
            val y = layout.yForStep(line * 2)
            canvas.drawLine(layout.staffLeft, y, layout.staffRight, y, staffPaint)
        }

        val notes = scale.notes
        val spacing = if (notes.size > 1) {
            (layout.staffRight - layout.staffLeft) / (notes.size - 1)
        } else {
            0f
        }

        notes.forEachIndexed { index, note ->
            val centerX = if (notes.size == 1) {
                (layout.staffLeft + layout.staffRight) / 2f
            } else {
                layout.staffLeft + index * spacing
            }
            val step = staffStepFor(note.name)
            val centerY = layout.yForStep(step)
            drawLedgerLines(canvas, centerX, step, layout)
            drawNote(canvas, centerX, centerY, step, note)
        }
    }

    private fun buildStaffLayout(): StaffLayout {
        val staffLeft = paddingLeft + sidePadding
        val staffRight = width - paddingRight - sidePadding
        val topY = paddingTop + topPadding
        val bottomY = height - paddingBottom - bottomPadding
        val usableHeight = (bottomY - topY).coerceAtLeast(noteHeadHeight * 6f)
        val stepRange = (VISIBLE_MAX_STEP - VISIBLE_MIN_STEP).coerceAtLeast(1)
        val stepSize = usableHeight / stepRange
        val bottomReferenceY = bottomY + VISIBLE_MIN_STEP * stepSize

        return StaffLayout(
            staffLeft = staffLeft,
            staffRight = staffRight,
            stepSize = stepSize,
            bottomReferenceY = bottomReferenceY
        )
    }

    private fun drawNote(canvas: Canvas, centerX: Float, centerY: Float, step: Int, note: ScaleNote) {
        val color = when {
            currentPitchClass == note.pitchClass -> ContextCompat.getColor(context, R.color.tuner_yellow)
            playedPitchClasses.contains(note.pitchClass) -> ContextCompat.getColor(context, R.color.tuner_green)
            else -> ContextCompat.getColor(context, R.color.text_secondary)
        }

        notePaint.color = color
        stemPaint.color = color

        canvas.save()
        canvas.rotate(-20f, centerX, centerY)
        canvas.drawOval(
            RectF(
                centerX - noteHeadWidth / 2f,
                centerY - noteHeadHeight / 2f,
                centerX + noteHeadWidth / 2f,
                centerY + noteHeadHeight / 2f
            ),
            notePaint
        )
        canvas.restore()

        val stemUp = step < 6
        if (stemUp) {
            canvas.drawLine(
                centerX + noteHeadWidth / 2.8f,
                centerY,
                centerX + noteHeadWidth / 2.8f,
                centerY - stemLength,
                stemPaint
            )
        } else {
            canvas.drawLine(
                centerX - noteHeadWidth / 2.8f,
                centerY,
                centerX - noteHeadWidth / 2.8f,
                centerY + stemLength,
                stemPaint
            )
        }

        if (note.name.contains('#')) {
            canvas.drawText(
                "#",
                centerX - noteHeadWidth * 1.1f,
                centerY + accidentalPaint.textSize / 3f,
                accidentalPaint
            )
        }
    }

    private fun drawLedgerLines(
        canvas: Canvas,
        centerX: Float,
        step: Int,
        layout: StaffLayout
    ) {
        if (step < 0) {
            var ledgerStep = -2
            while (ledgerStep >= step) {
                val y = layout.yForStep(ledgerStep)
                canvas.drawLine(centerX - ledgerWidth / 2f, y, centerX + ledgerWidth / 2f, y, ledgerPaint)
                ledgerStep -= 2
            }
        } else if (step > 8) {
            var ledgerStep = 10
            while (ledgerStep <= step) {
                val y = layout.yForStep(ledgerStep)
                canvas.drawLine(centerX - ledgerWidth / 2f, y, centerX + ledgerWidth / 2f, y, ledgerPaint)
                ledgerStep += 2
            }
        }
    }

    private fun staffStepFor(noteName: String): Int {
        val letter = noteName.firstOrNull()?.uppercaseChar() ?: 'C'
        val octave = noteName.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 4
        return (octave - 4) * 7 + naturalLetterOffset(letter) - naturalLetterOffset('E')
    }

    private fun naturalLetterOffset(letter: Char): Int {
        return when (letter) {
            'C' -> 0
            'D' -> 1
            'E' -> 2
            'F' -> 3
            'G' -> 4
            'A' -> 5
            'B' -> 6
            else -> 0
        }
    }

    private fun normalizePitchClass(value: Int): Int {
        return ((value % 12) + 12) % 12
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }

    private companion object {
        // Fixed treble-staff viewport so note names always map to the same lines/spaces.
        const val VISIBLE_MIN_STEP = -4
        const val VISIBLE_MAX_STEP = 12
    }
}
