package eu.kanade.tachiyomi.ui.more.upcoming

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.UpcomingControllerBinding
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.view.liftAppbarWith
import eu.kanade.tachiyomi.util.view.withFadeTransaction

class UpcomingController : BaseController<UpcomingControllerBinding>(), SmallToolbarInterface {

    private val presenter = UpcomingPresenter()

    override fun getTitle() = resources?.getString(R.string.upcoming)

    override fun createBinding(inflater: LayoutInflater) = UpcomingControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        liftAppbarWith(binding.recycler, true, changeMarginsInstead = true)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)

        val items = presenter.getUpcomingItems()
        binding.emptyView.isVisible = items.isEmpty()
        if (items.isEmpty()) {
            binding.emptyView.show(R.drawable.ic_calendar_text_outline_24dp, R.string.upcoming_empty)
        } else {
            binding.recycler.adapter = UpcomingAdapter(items) { item ->
                router.pushController(MangaDetailsController(item.manga).withFadeTransaction())
            }
        }
    }
}
