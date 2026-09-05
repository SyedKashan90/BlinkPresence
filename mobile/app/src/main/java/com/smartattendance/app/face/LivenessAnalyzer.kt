package com.smartattendance.app.face

/**
 * Passive liveness check (FR-07): waits for a blink — both eyes' open
 * probability dropping below [closedThreshold] then rising back above
 * [openThreshold] within [maxFramesBetween] frames. A printed photo or a
 * static replayed image can't produce this transition, which is the
 * cheapest signal that defeats the risks the TRD calls out (printed
 * photos, screen replay) without needing a depth sensor.
 */
class LivenessAnalyzer(
    private val openThreshold: Float = 0.6f,
    private val closedThreshold: Float = 0.25f,
    private val maxFramesBetween: Int = 30, // ~2s at 15fps analysis rate
) {
    private enum class State { WAITING_OPEN, WAITING_CLOSE, WAITING_REOPEN }

    private var state = State.WAITING_OPEN
    private var framesSinceClose = 0
    var blinkDetected: Boolean = false
        private set

    fun reset() {
        state = State.WAITING_OPEN
        framesSinceClose = 0
        blinkDetected = false
    }

    /** Feed one frame's eye-open probabilities; returns true the instant a blink completes. */
    fun onFrame(leftOpen: Float?, rightOpen: Float?): Boolean {
        if (blinkDetected) return true
        val left = leftOpen ?: return false
        val right = rightOpen ?: return false
        val avgOpen = (left + right) / 2f

        when (state) {
            State.WAITING_OPEN -> {
                if (avgOpen >= openThreshold) state = State.WAITING_CLOSE
            }
            State.WAITING_CLOSE -> {
                if (avgOpen <= closedThreshold) {
                    state = State.WAITING_REOPEN
                    framesSinceClose = 0
                }
            }
            State.WAITING_REOPEN -> {
                framesSinceClose++
                if (avgOpen >= openThreshold) {
                    blinkDetected = true
                    return true
                }
                if (framesSinceClose > maxFramesBetween) {
                    // Took too long — restart the wait rather than accept a stale attempt.
                    state = State.WAITING_CLOSE
                }
            }
        }
        return false
    }
}
