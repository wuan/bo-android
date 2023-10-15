/*

   Copyright 2015, 2016 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app.controller

import android.content.Context
import android.view.View
import android.widget.ImageButton
import org.blitzortung.android.app.ButtonGroup
import org.blitzortung.android.app.databinding.MainBinding
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.Mode
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.protocol.Event

class HistoryController(
    private val context: Context,
    private val binding: MainBinding,
    private val buttonHandler: ButtonColumnHandler<ImageButton, ButtonGroup>,
    private val dataHandler: MainDataHandler
) {

    private val buttons: MutableCollection<ImageButton> = arrayListOf()

    private var animationRunning = false

    val dataConsumer = { event: Event ->
        if (event is ResultEvent) {
            setRealtimeData(event.containsRealtimeData())
        }
    }

    init {
        setupStartStopAnimationButton()
        setupGoRealtimeButton()
        setRealtimeData(true)
    }

    private fun setRealtimeData(realtimeData: Boolean) {
        val historyButtonsVisibility = if (realtimeData || animationRunning) View.INVISIBLE else View.VISIBLE
        binding.goRealtime.visibility = historyButtonsVisibility
        if (realtimeData) {
            binding.timeSlider.progress = binding.timeSlider.max
        }
        updateButtonColumn()
    }

    private fun setupStartStopAnimationButton() {
        addButtonWithOnClickAction(binding.startStopAnimation) {
            if (animationRunning) {
                dataHandler.stop()
                binding.startStopAnimation.setImageResource(android.R.drawable.ic_media_play)
                dataHandler.goRealtime()
                configureForRealtimeOperation()

                animationRunning = false
                setRealtimeData(true)
                binding.timeSlider.isEnabled = true
            } else {
                dataHandler.stop()
                binding.startStopAnimation.setImageResource(android.R.drawable.ic_media_pause)
                animationRunning = true
                binding.timeSlider.isEnabled = false
                dataHandler.startAnimation()
            }
        }

        binding.startStopAnimation.visibility = View.VISIBLE
    }

    private fun setupGoRealtimeButton() {
        addButtonWithOnClickAction(binding.goRealtime) {
            if (dataHandler.goRealtime()) {
                configureForRealtimeOperation()
            }
        }
    }

    private fun addButtonWithOnClickAction(button: ImageButton, action: (View) -> Unit): ImageButton {
        return button.apply {
            buttons.add(this)
            visibility = View.INVISIBLE
            setOnClickListener(action)
        }
    }

    private fun configureForRealtimeOperation() {
        binding.goRealtime.visibility = View.INVISIBLE
        updateButtonColumn()

        dataHandler.restart()
        val historySteps = dataHandler.historySteps()
        binding.timeSlider.max = historySteps
        binding.timeSlider.progress = historySteps
    }

    private fun updateButtonColumn() {
        buttonHandler.updateButtonColumn()
    }

    fun getButtons(): Collection<ImageButton> {
        return buttons.toList()
    }

    private fun updateData() {
        dataHandler.updateData(setOf(DataChannel.STRIKES))
    }

    fun onResume() {
        animationRunning = dataHandler.mode == Mode.ANIMATION
        if (animationRunning) {
            binding.startStopAnimation.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.timeSlider.isEnabled = false
        }
    }
}
