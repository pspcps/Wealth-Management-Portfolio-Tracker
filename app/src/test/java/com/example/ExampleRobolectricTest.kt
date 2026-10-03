package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Wealth Management", appName)
  }

  @Test
  fun `user profile default and update test`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val profileRepo = com.example.data.repository.UserProfileRepository(context)
    
    val initialProfile = profileRepo.userProfile.value
    assertEquals("Prakash Seervi", initialProfile.name)
    assertEquals("PS", initialProfile.initials)

    profileRepo.updateProfile(
        name = "Alex Vance",
        email = "alex@example.com",
        tagline = "Growth & Freedom Portfolio",
        targetNetWorth = 25000000.0,
        colorIndex = 3
    )

    val updatedProfile = profileRepo.userProfile.value
    assertEquals("Alex Vance", updatedProfile.name)
    assertEquals("AV", updatedProfile.initials)
    assertEquals("alex@example.com", updatedProfile.email)
    assertEquals(25000000.0, updatedProfile.targetNetWorth, 0.01)
  }
}
