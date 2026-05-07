package dev.cannoli.scorza.config

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlatformsJsonAssetTest {
    @Test fun `bundled platforms_json parses without throwing`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pc = PlatformConfig(File(ctx.cacheDir, "fake-root"), ctx.assets)
        check(pc.getAllTags().isNotEmpty())
    }
}
