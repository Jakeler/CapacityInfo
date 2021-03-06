package com.ph03nix_x.capacityinfo.helpers

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import com.ph03nix_x.capacityinfo.R

object TextAppearanceHelper {

    fun setTextAppearance(context: Context, textView: AppCompatTextView, textStylePref: String?,
                          textFontPref: String?, textSizePref: String?) {

        setTextSize(context, textView, textSizePref)

        val fontFamily = setTextFont(textView.context, textFontPref)

        textView.typeface = setTextStyle(textView, textStylePref, fontFamily)
    }

    private fun setTextStyle(textView: AppCompatTextView, textStylePref: String?,
                             fontFamily: Typeface?): Typeface? {

        return when(textStylePref?.toInt()) {

            0 -> TypefaceCompat.create(textView.context, fontFamily, Typeface.NORMAL)

            1 -> TypefaceCompat.create(textView.context, fontFamily, Typeface.BOLD)

            2 -> TypefaceCompat.create(textView.context, fontFamily, Typeface.ITALIC)

            else -> null
        }
    }

    private fun setTextFont(context: Context, textFontPref: String?): Typeface? {

        return when(textFontPref?.toInt()) {

            0 -> Typeface.DEFAULT

            1 -> ResourcesCompat.getFont(context, R.font.roboto)

            2 -> Typeface.SERIF

            3 -> Typeface.SANS_SERIF

            4 -> Typeface.MONOSPACE

            5 -> ResourcesCompat.getFont(context, R.font.open_sans)

            6 -> ResourcesCompat.getFont(context, R.font.google_sans)

            7 -> ResourcesCompat.getFont(context, R.font.san_francisco)

            8 -> ResourcesCompat.getFont(context, R.font.times_new_roman)

            9 -> ResourcesCompat.getFont(context, R.font.ubuntu)

            10 -> ResourcesCompat.getFont(context, R.font.oxygen)

            11 -> ResourcesCompat.getFont(context, R.font.indie_flower)

            else -> null
        }
    }

    private fun setTextSize(context: Context, textView: AppCompatTextView, textSizePref: String?) {

        when(textSizePref?.toInt()) {

            0 -> textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.very_small_text_size))

            1 -> textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.small_text_size))

            2 -> textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.medium_text_size))

            3 -> textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.large_text_size))

            4 -> textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.very_large_text_size))
        }
    }
}