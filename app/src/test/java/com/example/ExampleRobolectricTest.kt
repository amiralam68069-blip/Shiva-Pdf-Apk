package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SavedPdfItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SHIVA PDF", appName)
  }

  @Test
  fun `verify saved pdf item defaults`() {
    val item = SavedPdfItem(
        title = "Physics Class 12 Chapter 1 Notes",
        category = "Science",
        subject = "Physics",
        originalUrl = "https://sivapdf.free.je/downloads/physics_ch1.pdf",
        isPremium = true
    )
    assertEquals("Physics Class 12 Chapter 1 Notes", item.title)
    assertTrue(item.isPremium)
    assertEquals(1, item.lastPageRead)
  }

  @Test
  fun `verify navigation tabs paths and endpoints`() {
    val homeTab = com.example.ui.NavigationTab.HOME
    val pdfsTab = com.example.ui.NavigationTab.PDFS
    val quizTab = com.example.ui.NavigationTab.QUIZ
    val purchasesTab = com.example.ui.NavigationTab.PURCHASES
    val profileTab = com.example.ui.NavigationTab.PROFILE

    assertEquals("/index.php", homeTab.path)
    assertEquals("/pdfs.php", pdfsTab.path)
    assertEquals("/quiz-list.php", quizTab.path)
    assertEquals("/my-purchases.php", purchasesTab.path)
    assertEquals("/login.php", profileTab.path)
  }
}
