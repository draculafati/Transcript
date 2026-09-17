package com.transcript.core.overlay

interface IOverlayPresenter {
    fun showCapsule()
    fun hideCapsule()
    fun updateSubtitle(token: String)
    fun setListeningState(isListening: Boolean)
}
