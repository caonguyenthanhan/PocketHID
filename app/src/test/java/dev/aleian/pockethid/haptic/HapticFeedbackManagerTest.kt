package dev.aleian.pockethid.haptic

import org.junit.Assert.assertEquals
import org.junit.Test

class HapticFeedbackManagerTest {

    class MockHapticController : IHapticFeedbackController {
        val playedEvents = mutableListOf<HapticEvent>()

        override fun play(event: HapticEvent) {
            playedEvents.add(event)
        }

        override fun release() {
        }
    }

    @Test
    fun `HapticFeedbackManager delegates play to controller`() {
        val mockController = MockHapticController()
        HapticFeedbackManager.setController(mockController)

        HapticFeedbackManager.play(HapticEvent.ACTION_ACCEPTED)
        HapticFeedbackManager.play(HapticEvent.MODE_CHANGED)
        HapticFeedbackManager.play(HapticEvent.CONNECTED)

        assertEquals(3, mockController.playedEvents.size)
        assertEquals(HapticEvent.ACTION_ACCEPTED, mockController.playedEvents[0])
        assertEquals(HapticEvent.MODE_CHANGED, mockController.playedEvents[1])
        assertEquals(HapticEvent.CONNECTED, mockController.playedEvents[2])
    }
}
