package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.google.android.material.progressindicator.CircularProgressIndicator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy

/**
 * A wrapper for [CircularProgressIndicator] that always rotates while being determinate.
 *
 * By always rotating we give the feedback to the user that the application isn't 'stuck',
 * and by making it determinate the user also approximately knows how much the operation will take.
 */
class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // TX-->
    private val preferences: PreferencesHelper by injectLazy()
    private val useAnimation: Boolean = preferences.useAnimatedLoader().get()

    private val holder: LinearLayout
    private val textIndicator: TextView
    private val extraTextIndicator: TextView
    private val extraIndicator: CircularProgressIndicator
    // TX<--

    private val indicator: CircularProgressIndicator

    private val rotateAnimation by lazy {
        RotateAnimation(
            0F,
            360F,
            Animation.RELATIVE_TO_SELF,
            0.5F,
            Animation.RELATIVE_TO_SELF,
            0.5F,
        ).apply {
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            duration = 4000
        }
    }

    init {
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        indicator = CircularProgressIndicator(context)
        // TX-->
        holder = LinearLayout(context)
        extraIndicator = CircularProgressIndicator(context)
        textIndicator = TextView(context)
        extraTextIndicator = TextView(context)
        if (useAnimation) {
            extraIndicator.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
            indicator.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
            indicator.max = 100
            indicator.isIndeterminate = true
            extraIndicator.max = 100
            extraIndicator.isIndeterminate = true
            extraIndicator.indicatorSize += indicator.trackThickness + 20
            addView(extraIndicator)
            addView(indicator)
            extraIndicator.hide()
        } else {
            holder.orientation = LinearLayout.VERTICAL
            holder.addView(textIndicator)
            holder.addView(extraTextIndicator)
            extraTextIndicator.visibility = View.INVISIBLE
            textIndicator.setText(R.string.inanimate_queued_text)
            extraTextIndicator.setText(R.string.inanimate_queued_text)
            addView(holder)
        }
        // TX<--
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateRotateAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateRotateAnimation()
    }

    // TX-->
    fun show() {
        if (useAnimation) {
            indicator.show()
            updateRotateAnimation()
        } else {
            textIndicator.visibility = View.VISIBLE
        }
    }

    fun hide() {
        if (useAnimation) {
            indicator.hide()
            updateRotateAnimation()
        } else {
            textIndicator.visibility = View.INVISIBLE
        }
    }

    fun showExtra() {
        if (useAnimation) {
            extraIndicator.show()
            updateRotateAnimation()
        } else {
            extraTextIndicator.visibility = View.VISIBLE
        }
    }

    fun hideExtra() {
        if (useAnimation) {
            extraIndicator.hide()
            updateRotateAnimation()
        } else {
            extraTextIndicator.visibility = View.INVISIBLE
        }
    }

    /**
     * Sets the current indicator progress to the specified value.
     */
    fun updateProgress(bytesRead: Long, totalBytes: Long, progress: Int, animated: Boolean = true, merge: Boolean = false) {
        if (progress > 0) {
            if (useAnimation) {
                if (progress > indicator.progress) {
                    indicator.setProgressCompat(progress, animated)
                    // updateRotateAnimation()
                }
            } else {
                if (merge) {
                    textIndicator.text = context.resources.getString(R.string.inanimate_merging_text, progress)
                } else {
                    textIndicator.text = if (bytesRead > 1048576 && totalBytes > 1048576) {
                        context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1048576.0, "MB", totalBytes / 1048576.0, "MB", progress)
                    } else if (bytesRead < 1048576 && totalBytes > 1048576) {
                        context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1024.0, "KB", totalBytes / 1048576.0, "MB", progress)
                    } else {
                        context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1024.0, "KB", totalBytes / 1024.0, "KB", progress)
                    }
                }
            }
        } else {
            if (useAnimation) {
                if (!indicator.isIndeterminate) {
                    indicator.hide()
                    indicator.isIndeterminate = true
                    indicator.show()
                    updateRotateAnimation()
                }
            } else {
                textIndicator.setText(R.string.inanimate_loading_text)
                updateLayoutParams<LayoutParams> { gravity = Gravity.CENTER }
            }
        }
    }

    /**
     * Sets extraIndicator progress to the specified value.
     */
    fun updateExtraProgress(bytesRead: Long, totalBytes: Long, progress: Int, animated: Boolean = true) {
        if (progress > 0) {
            if (useAnimation) {
                extraIndicator.setProgressCompat(progress, animated)
                // updateRotateAnimation()
            } else {
                extraTextIndicator.text = if (bytesRead > 1048576 && totalBytes > 1048576) {
                    context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1048576.0, "MB", totalBytes / 1048576.0, "MB", progress)
                } else if (bytesRead < 1048576 && totalBytes > 1048576) {
                    context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1024.0, "KB", totalBytes / 1048576.0, "MB", progress)
                } else {
                    context.resources.getString(R.string.inanimate_downloading_text, bytesRead / 1024.0, "KB", totalBytes / 1024.0, "KB", progress)
                }
            }
        } else {
            if (useAnimation) {
                if (!extraIndicator.isIndeterminate) {
                    extraIndicator.hide()
                    extraIndicator.isIndeterminate = true
                    extraIndicator.show()
                    updateRotateAnimation()
                }
            } else {
                textIndicator.setText(R.string.inanimate_loading_text)
                updateLayoutParams<LayoutParams> { gravity = Gravity.CENTER }
            }
        }
    }
    // TX<--

    /**
     * Sets the current indicator progress to the specified value.
     *
     * @param progress Indicator will be set indeterminate if this value is 0
     */
    /*fun setProgress(@IntRange(from = 0, to = 100) progress: Int, animated: Boolean = true) {
        if (progress > 0) {
            indicator.setProgressCompat(progress, animated)
        } else if (!indicator.isIndeterminate) {
            indicator.hide()
            indicator.isIndeterminate = true
            indicator.show()
        }
        updateRotateAnimation()
    }*/

    private fun updateRotateAnimation() {
        if (isAttachedToWindow && indicator.isShown && !indicator.isIndeterminate) {
            if (animation == null) {
                startAnimation(rotateAnimation)
            }
        } else {
            clearAnimation()
        }
    }
}
