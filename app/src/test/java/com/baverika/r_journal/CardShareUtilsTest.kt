package com.baverika.r_journal

import com.baverika.r_journal.utils.CardShareUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CardShareUtilsTest {

    @Test
    fun prepareSharedDir_createsDirectoryAndPrunesStaleFiles() {
        val tempCache = createTempDirectory("r_journal_share_test").toFile()
        try {
            val dir = CardShareUtils.prepareSharedDir(tempCache)
            assertTrue(dir.exists())
            assertEquals(CardShareUtils.SHARED_CARDS_DIR, dir.name)

            val staleFile = File(dir, "old_export.png").apply { writeText("stale") }
            assertTrue(staleFile.exists())

            CardShareUtils.prepareSharedDir(tempCache)
            assertFalse("Stale shared export should be pruned", staleFile.exists())
        } finally {
            tempCache.deleteRecursively()
        }
    }

    @Test
    fun createTargetPngFile_sanitizesPrefixAndUsesPngExtension() {
        val tempCache = createTempDirectory("r_journal_share_name_test").toFile()
        try {
            val file = CardShareUtils.createTargetPngFile(tempCache, "2026 milestone/card")
            assertEquals("png", file.extension)
            assertTrue(file.name.startsWith("2026_milestone_card_"))
            assertEquals(CardShareUtils.SHARED_CARDS_DIR, file.parentFile?.name)
        } finally {
            tempCache.deleteRecursively()
        }
    }
}
