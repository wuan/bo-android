package org.blitzortung.android.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import org.blitzortung.android.app.R

class CustomToast
    (context: Context?) : Toast(context) {
    companion object {
        fun makeText(context: Context, text: Int, duration: Int): Toast {
            val t = Toast.makeText(context, text, duration)
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout: View? = inflater.inflate(R.layout.toast, null)
            t.setView(layout)
            return t
        }
    }
}


