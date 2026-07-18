package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.system.ImageUtil
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File

/**
 * Loader used to load a chapter from a .7z or .cb7 file.
 */
class SevenZipPageLoader(file: File) : PageLoader() {

    /**
     * The 7z archive to load pages from.
     */
    private val archive = SevenZFile(file)

    /**
     * Recycles this loader and the open archive.
     */
    override fun recycle() {
        super.recycle()
        archive.close()
    }

    /**
     * Returns the pages found on this 7z archive ordered with a natural comparator.
     */
    override suspend fun getPages(): List<ReaderPage> {
        return archive.entries.asSequence()
            .filter { !it.isDirectory && ImageUtil.isImage(it.name) { archive.getInputStream(it) } }
            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
            .mapIndexed { i, entry ->
                ReaderPage(i).apply {
                    stream = { archive.getInputStream(entry) }
                    status = Page.State.READY
                }
            }
            .toList()
    }

    /**
     * No additional action required to load the page
     */
    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }
}
