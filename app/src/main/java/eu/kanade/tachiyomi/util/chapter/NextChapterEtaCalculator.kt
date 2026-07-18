package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter

/**
 * Estimates when a manga's next chapter will be released, based on the average interval
 * between the upload dates of its most recent chapters. Mirrors Mihon's "upcoming" estimate.
 */
object NextChapterEtaCalculator {

    private const val MIN_CHAPTERS_FOR_ESTIMATE = 3

    /**
     * @return the estimated epoch millis of the next chapter release, or null if there's not
     * enough upload-date history to make a meaningful estimate.
     */
    fun estimateNextChapterDate(chapters: List<Chapter>): Long? {
        val dates = chapters
            .mapNotNull { chapter -> chapter.date_upload.takeIf { it > 0 } }
            .distinct()
            .sortedDescending()
        if (dates.size < MIN_CHAPTERS_FOR_ESTIMATE) return null

        val intervals = dates.zipWithNext { newer, older -> newer - older }.filter { it > 0 }
        if (intervals.isEmpty()) return null

        val avgIntervalMs = intervals.average().toLong()
        return dates.first() + avgIntervalMs
    }
}
