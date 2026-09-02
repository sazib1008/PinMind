package com.example.pinmind.core.notification

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun `channel and action constants are properly configured`() {
        assertEquals("pinmind_task_reminders", NotificationHelper.CHANNEL_ID)
        assertEquals("com.example.pinmind.ACTION_MARK_DONE", NotificationHelper.ACTION_MARK_DONE)
        assertEquals("extra_task_id", NotificationHelper.EXTRA_TASK_ID)
        assertEquals("extra_notification_id", NotificationHelper.EXTRA_NOTIFICATION_ID)
        assertArrayEquals(longArrayOf(0, 300, 200, 300), NotificationHelper.VIBRATION_PATTERN)
    }
}
