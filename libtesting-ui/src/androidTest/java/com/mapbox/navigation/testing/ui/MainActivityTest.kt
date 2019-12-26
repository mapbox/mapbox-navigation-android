package com.mapbox.navigation.testing.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.agoda.kakao.screen.Screen
import com.agoda.kakao.text.KButton
import com.agoda.kakao.text.KTextView
import com.mapbox.navigation.testing.ui.rules.NotificationTestRule
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityTest : NotificationTestRule<MainActivity>(MainActivity::class.java) {

    @Test
    fun testDefaultMessage() {
        MainScreen {
            textViewMessage {
                isVisible()
                hasText(R.string.default_message)
            }
        }
    }

    @Test
    fun testNewMessage() {
        MainScreen {
            buttonSetMessage {
                isVisible()
                isEnabled()
                click()
            }

            textViewMessage {
                isVisible()
                hasText(R.string.new_message)
            }
        }
    }

    @Test
    fun testNotification() {
        before {
            device.screenshots.take("before test")
        }.after {
            device.uiDevice.clearAllNotifications()
        }.run {
            step("Build and show notification") {
                MainScreen {
                    buttonShowNotification {
                        isVisible()
                        isDisabled()
                    }

                    buttonSetMessage {
                        isVisible()
                        isEnabled()
                        click()
                    }

                    buttonShowNotification {
                        isVisible()
                        isEnabled()
                        click()
                    }
                }
            }

            step("Check notification info") {
                device.uiDevice.let {
                    it.waitForNotification()

                    val expectedTitle = AppNotificationManager.NOTIFICATION_TITLE
                    val expectedMessage = AppNotificationManager.NOTIFICATION_MESSAGE

                    val title = it.findObject(By.text(expectedTitle)).text
                    val message = it.findObject(By.text(expectedMessage)).text

                    Assert.assertEquals(expectedTitle, title)
                    Assert.assertEquals(expectedMessage, message)
                }
            }
        }
    }

    object MainScreen : Screen<MainScreen>() {
        val textViewMessage = KTextView {
            withId(R.id.et_message)
        }

        val buttonSetMessage = KButton {
            withId(R.id.btn_set_text)
        }

        val buttonShowNotification = KButton {
            withId(R.id.btn_show_notification)
        }
    }
}
