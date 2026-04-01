package com.whawe.guitartuner.scale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.whawe.guitartuner.R

class ScaleStaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // All note/stem/ledger dimensions are derived from stepSize at draw time so they
    // remain proportional to the staff regardless of view height or screen density.
    //
    // The -20° rotation increases the visual height of a note head:
    //   effectiveH = H*(cos20° + aspectRatio*sin20°) = H*1.442
    // To fill ~80% of one staff space (= 2*stepSize):
    //   H = 2*stepSize*0.80 / 1.442 ≈ stepSize * 1.11
    // This guarantees a note on a line is visually cut by that line, and a note in
    // a space has clear clearance from the lines above and below it.
    private data class StaffLayout(
        val staffLeft: Float,
        val staffRight: Float,
        val stepSize: Float,
        val bottomReferenceY: Float
    ) {
        val noteHeadHeight: Float = stepSize * 1.11f
        val noteHeadWidth: Float = noteHeadHeight * (22f / 15f)
        val stemLength: Float = stepSize * 7.0f       // 3.5 staff spaces — standard
        val ledgerLineWidth: Float = noteHeadWidth * 1.3f

        fun yForStep(step: Int): Float = bottomReferenceY - step * stepSize
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
        textAlign = Paint.Align.CENTER
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.panel_stroke)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }

    private val sidePadding = dp(28f)
    private val topPadding = dp(28f)
    private val bottomPadding = dp(28f)

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
        if (scale.notes.isEmpty()) return

        val layout = buildStaffLayout()

        // Five staff lines at even steps 0..8 (E4=bottom, F5=top in treble clef)
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
            drawNote(canvas, centerX, centerY, step, note, layout)
        }
    }

    private fun buildStaffLayout(): StaffLayout {
        val staffLeft = paddingLeft + sidePadding
        val staffRight = width - paddingRight - sidePadding
        val topY = paddingTop + topPadding
        val bottomY = height - paddingBottom - bottomPadding
        val usableHeight = (bottomY - topY).coerceAtLeast(dp(90f))
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

    private fun drawNote(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        step: Int,
        note: ScaleNote,
        layout: StaffLayout
    ) {
        val color = when {
            currentPitchClass == note.pitchClass -> ContextCompat.getColor(context, R.color.tuner_yellow)
            playedPitchClasses.contains(note.pitchClass) -> ContextCompat.getColor(context, R.color.tuner_green)
            else -> ContextCompat.getColor(context, R.color.text_secondary)
        }
        notePaint.color = color
        stemPaint.color = color

        val nH = layout.noteHeadHeight
        val nW = layout.noteHeadWidth

        canvas.save()
        canvas.rotate(-20f, centerX, centerY)
        canvas.drawOval(
            RectF(
                centerX - nW / 2f,
                centerY - nH / 2f,
                centerX + nW / 2f,
                centerY + nH / 2f
            ),
            notePaint
        )
        canvas.restore()

        // Stems go up from the right side for lower notes, down from the left for higher notes.
        // The boundary is above the middle line (step 4 = B4) following standard convention.
        val stemUp = step < 6
        if (stemUp) {
            canvas.drawLine(
                centerX + nW / 2.8f, centerY,
                centerX + nW / 2.8f, centerY - layout.stemLength,
                stemPaint
            )
        } else {
            canvas.drawLine(
                centerX - nW / 2.8f, centerY,
                centerX - nW / 2.8f, centerY + layout.stemLength,
                stemPaint
            )
        }

        if (note.name.contains('#')) {
            accidentalPaint.textSize = nH * 1.5f
            canvas.drawText(
                "#",
                centerX - nW * 1.1f,
                centerY + nH * 0.5f,
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
        val half = layout.ledgerLineWidth / 2f
        if (step < 0) {
            var ledgerStep = -2
            while (ledgerStep >= step) {
                val y = layout.yForStep(ledgerStep)
                canvas.drawLine(centerX - half, y, centerX + half, y, ledgerPaint)
                ledgerStep -= 2
            }
        } else if (step > 8) {
            var ledgerStep = 10
            while (ledgerStep <= step) {
                val y = layout.yForStep(ledgerStep)
                canvas.drawLine(centerX - half, y, centerX + half, y, ledgerPaint)
                ledgerStep += 2
            }
        }
    }

    // Maps a note name (e.g. "E4", "F#5", "C#4") to a diatonic staff step.
    // Step 0 = E4 (bottom line of treble clef).  Even steps are lines; odd are spaces.
    // Each octave spans exactly 7 diatonic steps.
    private fun staffStepFor(noteName: String): Int {
        val letter = noteName.firstOrNull()?.uppercaseChar() ?: 'C'
        val octave = noteName.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 4
        return (octave - 4) * 7 + naturalLetterOffset(letter) - naturalLetterOffset('E')
    }

    private fun naturalLetterOffset(letter: Char): Int = when (letter) {
        'C' -> 0; 'D' -> 1; 'E' -> 2; 'F' -> 3; 'G' -> 4; 'A' -> 5; 'B' -> 6
        else -> 0
    }

    private fun normalizePitchClass(value: Int): Int = ((value % 12) + 12) % 12

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        // Treble-clef viewport: 4 steps of ledger space below and above the 5-line staff.
        // Step -4 = A3 (bottom of viewport), step 12 = C6 (top of viewport).
        const val VISIBLE_MIN_STEP = -4
        const val VISIBLE_MAX_STEP = 12
    }
}
