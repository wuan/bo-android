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
import android.widget.Toast
import org.blitzortung.android.app.ButtonGroup
import org.blitzortung.android.app.R
import org.blitzortung.android.app.databinding.MainBinding
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.protocol.Event

class HistoryController(
        private val context: Context,
        private val binding: MainBinding,
        private val buttonHandler: ButtonColumnHandler<View, ButtonGroup>,
        private val dataHandler: MainDataHandler
) {

    private val buttons: MutableCollection<ImageButton> = arrayListOf()

    val dataConsumer = { event: Event ->
        if (event is ResultEvent) {
            setRealtimeData(event.containsRealtimeData())
        }
    }

    init {
        setupHistoryRewindButton()
        setupHistoryForwardButton()
        setupGoRealtimeButton()

        setRealtimeData(true)
    }

    private fun setRealtimeData(realtimeData: Boolean) {
        binding.historyRew.visibility = View.VISIBLE
        val historyButtonsVisibility = if (realtimeData) View.INVISIBLE else View.VISIBLE
        binding.historyFfwd.visibility = historyButtonsVisibility
        binding.goRealtime.visibility = historyButtonsVisibility
        updateButtonColumn()
    }

    private fun setupHistoryRewindButton() {
        addButtonWithOnClickAction(binding.historyRew) {
            if (dataHandler.rewInterval()) {
                binding.historyFfwd.visibility = View.VISIBLE
                binding.goRealtime.visibility = View.VISIBLE
                updateButtonColumn()
                updateData()
            } else {
                val toast = Toast.makeText(context, context.resources.getText(R.string.historic_timestep_limit_reached), Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun setupHistoryForwardButton() {
        addButtonWithOnClickAction(binding.historyFfwd) {
            if (dataHandler.ffwdInterval()) {
                if (dataHandler.isRealtime) {
                    configureForRealtimeOperation()
                } else {
                    dataHandler.updateData()
                }
            }
        }
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
        binding.historyFfwd.visibility = View.INVISIBLE
        binding.goRealtime.visibility = View.INVISIBLE
        updateButtonColumn()

        dataHandler.restart()
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
}
