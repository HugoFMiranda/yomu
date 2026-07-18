package eu.kanade.tachiyomi.ui.more.upcoming

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.chapter.NextChapterEtaCalculator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class UpcomingPresenter(
    private val db: DatabaseHelper = Injekt.get(),
) {

    data class UpcomingItem(val manga: Manga, val nextChapterDate: Long)

    fun getUpcomingItems(): List<UpcomingItem> {
        val now = Date().time
        val libraryMangas = db.getLibraryMangas().executeAsBlocking()
        return libraryMangas
            .filterNot { it.isOneShotOrCompleted(db) }
            .mapNotNull { manga ->
                val chapters = db.getChapters(manga).executeAsBlocking()
                val nextDate = NextChapterEtaCalculator.estimateNextChapterDate(chapters) ?: return@mapNotNull null
                // Only show manga whose estimate is still in the future; a manga that's gone
                // quiet/on hiatus can otherwise produce an estimate years in the past.
                if (nextDate <= now) return@mapNotNull null
                UpcomingItem(manga, nextDate)
            }
            .sortedBy { it.nextChapterDate }
    }
}
