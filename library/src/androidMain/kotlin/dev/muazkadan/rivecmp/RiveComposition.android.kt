package dev.muazkadan.rivecmp

import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.PlayableInstance
import app.rive.runtime.kotlin.core.ViewModelInstance

actual class RiveComposition internal actual constructor(
    spec: RiveCompositionSpec
) {
    internal actual val spec: RiveCompositionSpec = spec
    private var animationViewRef: RiveAnimationView? = null
    private var boundViewModelInstance: ViewModelInstance? = null
    private var followUpFrameScheduled = false
    private var pendingApplyScheduled = false
    private var pendingApplyAttempts = 0
    private val pendingNumberProperties = mutableMapOf<String, Float>()
    private val listener = object : RiveFileController.Listener {
        override fun notifyPlay(animation: PlayableInstance) {
            applyPendingNumberPropertiesWhenReady()
        }
        override fun notifyPause(animation: PlayableInstance) = Unit
        override fun notifyStop(animation: PlayableInstance) = Unit
        override fun notifyLoop(animation: PlayableInstance) = Unit
        override fun notifyStateChanged(stateMachineName: String, stateName: String) = Unit
    }

    actual fun setNumberInput(stateMachineName: String, name: String, value: Float) {
        animationViewRef?.setNumberState(
            stateMachineName = stateMachineName,
            inputName = name,
            value = value
        )
    }

    actual fun setNumberProperty(name: String, value: Float) {
        if (pendingNumberProperties[name] == value && boundViewModelInstance != null) return
        pendingNumberProperties[name] = value
        applyPendingNumberPropertiesWhenReady()
    }

    private fun applyPendingNumberPropertiesWhenReady() {
        if (pendingNumberProperties.isEmpty()) return
        if (applyPendingNumberProperties()) {
            pendingApplyAttempts = 0
            return
        }

        val view = animationViewRef ?: return
        if (pendingApplyScheduled || pendingApplyAttempts >= MaxPendingApplyAttempts) return
        pendingApplyScheduled = true
        view.postOnAnimation {
            pendingApplyScheduled = false
            if (animationViewRef === view) {
                pendingApplyAttempts++
                applyPendingNumberPropertiesWhenReady()
            }
        }
    }

    private fun applyPendingNumberProperties(): Boolean {
        val view = animationViewRef ?: return false
        return try {
            val controller = view.controller
            val artboard = controller.activeArtboard ?: return false
            val instance = boundViewModelInstance
                ?: artboard.viewModelInstance
                ?: controller.file
                    ?.defaultViewModelForArtboard(artboard)
                    ?.createDefaultInstance()
                    ?.also { created ->
                        artboard.viewModelInstance = created
                        controller.stateMachines.forEach { it.viewModelInstance = created }
                        boundViewModelInstance = created
                    }
                ?: return false

            pendingNumberProperties.forEach { (name, value) ->
                instance.getNumberProperty(name).value = value
            }
            // Rive polls Data Binding after advancing the state machine. Schedule
            // two frames: one to poll the new value and one to evaluate it. Slow
            // gestures may otherwise provide only the first frame.
            view.invalidate()
            if (!followUpFrameScheduled) {
                followUpFrameScheduled = true
                view.postOnAnimation {
                    followUpFrameScheduled = false
                    if (animationViewRef === view) view.invalidate()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun setBooleanInput(stateMachineName: String, name: String, value: Boolean) {
        animationViewRef?.setBooleanState(
            stateMachineName = stateMachineName,
            inputName = name,
            value = value
        )
    }

    actual fun setTriggerInput(stateMachineName: String, name: String) {
        animationViewRef?.fireState(stateMachineName = stateMachineName, inputName = name)
    }

    actual fun pause() {
        animationViewRef?.pause()
    }

    actual fun reset() {
        animationViewRef?.reset()
    }

    actual fun stop() {
        animationViewRef?.stop()
    }

    internal actual fun connectToAnimationView(animationView: Any?) {
        val nextView = animationView as? RiveAnimationView
        if (animationViewRef === nextView) {
            applyPendingNumberPropertiesWhenReady()
            return
        }
        animationViewRef?.unregisterListener(listener)
        animationViewRef = nextView
        boundViewModelInstance = null
        followUpFrameScheduled = false
        pendingApplyScheduled = false
        pendingApplyAttempts = 0
        animationViewRef?.registerListener(listener)
        applyPendingNumberPropertiesWhenReady()
    }


    private companion object {
        const val MaxPendingApplyAttempts = 120
    }
}
