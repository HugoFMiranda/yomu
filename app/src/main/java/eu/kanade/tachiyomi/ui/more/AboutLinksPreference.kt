package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.GITHUB_REPO
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.compatToolTipText

class AboutLinksPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        layoutResource = R.layout.pref_about_links
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.itemView as LinearLayout).apply {
            checkHeightThen {
                val childCount = (this.getChildAt(0) as ViewGroup).childCount
                val childCount2 = (this.getChildAt(1) as ViewGroup).childCount
                val fullCount = childCount + childCount2
                orientation =
                    if (width >= (56 * fullCount).dpToPx) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            }
        }
        // Upstream community links removed as part of the rebrand
        listOf(
            R.id.btn_website,
            R.id.btn_discord,
            R.id.btn_x,
            R.id.btn_facebook,
            R.id.btn_reddit,
            R.id.btn_tachiyomi,
        ).forEach { holder.findViewById(it).isVisible = false }
        holder.findViewById(R.id.btn_github).apply {
            compatToolTipText = (contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://github.com/$GITHUB_REPO") }
        }
    }
}
