package com.example.barcodescanner.utils

import android.content.Context

class AndroidUtilities {
    fun dpToPx(dpValue: Int, context: Context?): Int {
        val d = context?.resources?.displayMetrics?.density ?: 0f
        return Math.round(dpValue * d)
    }
}