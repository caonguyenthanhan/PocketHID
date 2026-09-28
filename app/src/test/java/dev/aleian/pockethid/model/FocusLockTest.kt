package dev.aleian.pockethid.model

import android.bluetooth.BluetoothDevice
import dev.aleian.pockethid.action.ActionDispatcher
import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.action.PocketAction
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusLockTest {

    private class FakeInputTransport : InputTransport {
        override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        override val isSupported: Boolean = true
        override val connectedDevice: BluetoothDevice? = null
        override val isConnected: Boolean = true

        var lastKeyClickCode: Byte? = null
        var lastKeyClickMods: Byte? = null
        var lastMouseClickButton: Byte? = null
        var lastGamepadButtons: Int? = null
        var lastConsumerCode: Int? = null

        override fun register() {}
        override fun unregister() {}
        override fun syncConnectionState() {}
        override fun connect(device: BluetoothDevice): Boolean = true
        override fun disconnect(): Boolean = true

        override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean = true

        override suspend fun sendMouseClick(buttons: Byte) {
            lastMouseClickButton = buttons
        }

        override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean = true
        override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte): Boolean = true
        override fun sendKeyRelease(): Boolean = true

        override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {
            lastKeyClickCode = keyCode
            lastKeyClickMods = modifiers
        }

        override fun sendGamepadReport(
            buttons: Int,
            hat: Byte,
            leftX: Short,
            leftY: Short,
            rightX: Short,
            rightY: Short,
            leftTrigger: Byte,
            rightTrigger: Byte
        ): Boolean {
            lastGamepadButtons = buttons
            return true
        }

        override fun sendGamepadNeutral(): Boolean {
            lastGamepadButtons = 0
            return true
        }

        override fun sendConsumerClick(usageCode: Int): Boolean {
            lastConsumerCode = usageCode
            return true
        }

        override fun sendConsumerPress(usageCode: Int): Boolean {
            lastConsumerCode = usageCode
            return true
        }

        override fun sendConsumerRelease(): Boolean = true
        override fun sendTabletReport(status: Byte, x: Int, y: Int): Boolean = true
        override fun sendTabletNeutral(): Boolean = true
    }

    @Before
    fun setUp() {
        FocusLockController.reset()
    }

    @After
    fun tearDown() {
        FocusLockController.reset()
    }

    @Test
    fun testInitialStateIsUnlocked() {
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())
        assertTrue(FocusLockController.allowNavigationGesture())
        assertTrue(FocusLockController.allowsInternalAction())
    }

    @Test
    fun testFocusOffCycleAllModesAllowed() {
        FocusLockController.setLocked(false)

        // 1. Keyboard -> Mouse allowed
        assertEquals(1, FocusLockController.resolveModeSwitch(0, 1))
        assertEquals(ControlMode.MOUSE, FocusLockController.resolveModeSwitch(ControlMode.KEYBOARD, ControlMode.MOUSE))

        // 2. Mouse -> Gamepad allowed
        assertEquals(2, FocusLockController.resolveModeSwitch(1, 2))
        assertEquals(ControlMode.GAMEPAD, FocusLockController.resolveModeSwitch(ControlMode.MOUSE, ControlMode.GAMEPAD))

        // 3. Gamepad -> Presenter allowed
        assertEquals(3, FocusLockController.resolveModeSwitch(2, 3))
        assertEquals(ControlMode.PRESENTER, FocusLockController.resolveModeSwitch(ControlMode.GAMEPAD, ControlMode.PRESENTER))

        // 4. Presenter -> One-Hand allowed
        assertEquals(4, FocusLockController.resolveModeSwitch(3, 4))
        assertEquals(ControlMode.ONE_HAND, FocusLockController.resolveModeSwitch(ControlMode.PRESENTER, ControlMode.ONE_HAND))

        // 5. One-Hand -> Draw allowed
        assertEquals(5, FocusLockController.resolveModeSwitch(4, 5))
        assertEquals(ControlMode.DRAW, FocusLockController.resolveModeSwitch(ControlMode.ONE_HAND, ControlMode.DRAW))

        // 6. Draw -> Keyboard allowed
        assertEquals(0, FocusLockController.resolveModeSwitch(5, 0))
        assertEquals(ControlMode.KEYBOARD, FocusLockController.resolveModeSwitch(ControlMode.DRAW, ControlMode.KEYBOARD))
    }

    @Test
    fun testFocusOnFullModeMatrixRejectsAllSwitches() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)
        assertFalse(FocusLockController.canSwitchMode())

        val modes = ControlMode.entries

        // Verify full 6x6 Mode Matrix: For every current mode, all target mode switches are REJECTED
        for (current in modes) {
            for (target in modes) {
                // Int-indexed resolution
                val resolvedInt = FocusLockController.resolveModeSwitch(current.index, target.index)
                assertEquals(
                    "When Focus is ON, switch from ${current.name} to ${target.name} must remain ${current.name}",
                    current.index,
                    resolvedInt
                )

                // ControlMode resolution
                val resolvedMode = FocusLockController.resolveModeSwitch(current, target)
                assertEquals(
                    "When Focus is ON, switch from ${current.name} to ${target.name} must remain ${current.name}",
                    current,
                    resolvedMode
                )
            }
        }
    }

    @Test
    fun testFocusProtectsCurrentModeDynamicallyWhenEnabled() {
        // Section 19: Focus protects CURRENT mode, without lingering mode bias
        var currentMode = 0 // KEYBOARD

        // Step 1: In KEYBOARD, Focus OFF -> switch to MOUSE
        FocusLockController.setLocked(false)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 1)
        assertEquals(1, currentMode) // Now MOUSE

        // Step 2: Turn Focus ON -> attempt switch to GAMEPAD (2)
        FocusLockController.setLocked(true)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 2)
        assertEquals(1, currentMode) // Still MOUSE (rejected)

        // Step 3: Turn Focus OFF -> switch to GAMEPAD (2)
        FocusLockController.setLocked(false)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 2)
        assertEquals(2, currentMode) // Now GAMEPAD

        // Step 4: Turn Focus ON -> attempt switch to PRESENTER (3)
        FocusLockController.setLocked(true)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 3)
        assertEquals(2, currentMode) // Still GAMEPAD (rejected)

        // Step 5: Turn Focus OFF -> switch to DRAW (5)
        FocusLockController.setLocked(false)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 5)
        assertEquals(5, currentMode) // Now DRAW

        // Step 6: Turn Focus ON -> attempt switch to KEYBOARD (0)
        FocusLockController.setLocked(true)
        currentMode = FocusLockController.resolveModeSwitch(currentMode, 0)
        assertEquals(5, currentMode) // Still DRAW (rejected)
    }

    @Test
    fun testExplicitUnlockRestoresModeSwitching() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)
        assertFalse(FocusLockController.canSwitchMode())

        // User taps Focus toggle button to unlock
        val newLockState = FocusLockController.toggle()
        assertFalse("Toggle from ON must result in OFF", newLockState)
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())

        val resolved = FocusLockController.resolveModeSwitch(currentMode = 3, targetMode = 4)
        assertEquals("After unlocking, mode switch must work normally", 4, resolved)
    }

    @Test
    fun testToggleDoesNotDuplicateState() {
        assertFalse(FocusLockController.isLocked.value)

        FocusLockController.toggle()
        assertTrue(FocusLockController.isLocked.value)

        FocusLockController.toggle()
        assertFalse(FocusLockController.isLocked.value)
    }

    @Test
    fun testResetClearsLockState() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)

        FocusLockController.reset()
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())
        assertTrue(FocusLockController.allowNavigationGesture())
    }

    @Test
    fun testGestureNavigationGating() {
        // FOCUS OFF -> mode-switch gesture works
        FocusLockController.setLocked(false)
        assertTrue(FocusLockController.allowNavigationGesture())
        assertTrue(FocusLockController.allowsInternalAction())

        // FOCUS ON -> mode-switch gesture blocked
        FocusLockController.setLocked(true)
        assertFalse(FocusLockController.allowNavigationGesture())

        // FOCUS ON -> non-navigation internal actions/gestures STILL work!
        assertTrue(FocusLockController.allowsInternalAction())
    }

    @Test
    fun testInternalActionsExecuteNormallyWhenFocusLocked() = runBlocking {
        // When Focus is locked ON, internal actions across all 6 modes MUST execute 100% normally
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)

        val fakeTransport = FakeInputTransport()

        // 1. Keyboard internal control (Copy keystroke)
        val keySuccess = ActionDispatcher.dispatch(PocketAction.EditAction.Copy, fakeTransport, HostOs.WINDOWS)
        assertTrue(keySuccess)
        assertEquals(HidConstants.KEY_C, fakeTransport.lastKeyClickCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, fakeTransport.lastKeyClickMods)

        // 2. Mouse internal control (Pointer click)
        val mouseSuccess = ActionDispatcher.dispatch(PocketAction.PointerAction.MiddleClick, fakeTransport)
        assertTrue(mouseSuccess)
        assertEquals(HidConstants.MOUSE_BUTTON_MIDDLE, fakeTransport.lastMouseClickButton)

        // 3. Gamepad internal control (Button A press)
        val gamepadSuccess = ActionDispatcher.dispatch(PocketAction.GamepadAction.ButtonA, fakeTransport)
        assertTrue(gamepadSuccess)

        // 4. Presenter internal control (Next slide)
        val presenterSuccess = ActionDispatcher.dispatch(PocketAction.PresenterAction.NextSlide, fakeTransport)
        assertTrue(presenterSuccess)
        assertEquals(HidConstants.KEY_RIGHT, fakeTransport.lastKeyClickCode)

        // 5. One-Hand internal control (Media play/pause)
        val mediaSuccess = ActionDispatcher.dispatch(PocketAction.MediaAction.PlayPause, fakeTransport)
        assertTrue(mediaSuccess)
        assertEquals(HidConstants.CONSUMER_PLAY_PAUSE, fakeTransport.lastConsumerCode)

        // 6. Draw internal control: verify Focus allows internal actions
        assertTrue(FocusLockController.allowsInternalAction())
    }
}
