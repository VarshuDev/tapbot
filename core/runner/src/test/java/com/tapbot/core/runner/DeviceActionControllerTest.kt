package com.tapbot.core.runner

import com.tapbot.core.runner.device.DeviceActionController
import com.tapbot.core.runner.device.DeviceActionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceActionControllerTest {

    private lateinit var controller: DeviceActionController

    @Before
    fun setUp() {
        controller = DeviceActionController(context = null, handler = null)
    }

    @Test
    fun `isOwnerAuthorized verifies owner matching and fails closed when unconfigured`() {
        assertTrue(controller.isOwnerAuthorized("12345678", 12345678L))
        assertTrue(controller.isOwnerAuthorized("  987654  ", 987654L))

        assertFalse(controller.isOwnerAuthorized("12345678", 87654321L))
        assertFalse(controller.isOwnerAuthorized("12345678", null))

        assertFalse(controller.isOwnerAuthorized(null, 12345678L))
        assertFalse(controller.isOwnerAuthorized("", 12345678L))
        assertFalse(controller.isOwnerAuthorized("   ", 12345678L))
    }

    @Test
    fun `ringPhone activates ringing and stopRing silences`() {
        assertFalse(controller.isRinging())

        val ringResult = controller.ringPhone(durationSec = 15)
        assertTrue(ringResult is DeviceActionResult.Success)
        assertTrue((ringResult as DeviceActionResult.Success).message.contains("15 seconds"))
        assertTrue(controller.isRinging())

        val stopResult = controller.stopRing()
        assertTrue(stopResult is DeviceActionResult.Success)
        assertFalse(controller.isRinging())
    }

    @Test
    fun `ringPhone clamps duration between 5 and 60 seconds`() {
        val resultLow = controller.ringPhone(durationSec = 2)
        assertTrue((resultLow as DeviceActionResult.Success).message.contains("5 seconds"))

        val resultHigh = controller.ringPhone(durationSec = 120)
        assertTrue((resultHigh as DeviceActionResult.Success).message.contains("60 seconds"))
    }

    @Test
    fun `toggleTorch switches torch states and responds to arguments`() {
        assertFalse(controller.isTorchActive())

        val toggle1 = controller.toggleTorch(null)
        assertTrue(toggle1 is DeviceActionResult.Success)
        assertTrue(controller.isTorchActive())

        val toggle2 = controller.toggleTorch(null)
        assertTrue(toggle2 is DeviceActionResult.Success)
        assertFalse(controller.isTorchActive())

        controller.toggleTorch("on")
        assertTrue(controller.isTorchActive())

        controller.toggleTorch("off")
        assertFalse(controller.isTorchActive())
    }

    @Test
    fun `vibrate returns success with duration`() {
        val result = controller.vibrate(1000)
        assertTrue(result is DeviceActionResult.Success)
        assertTrue((result as DeviceActionResult.Success).message.contains("1000ms"))
    }

    @Test
    fun `postNotification validates empty text and formats success`() {
        val emptyResult = controller.postNotification(text = "   ")
        assertTrue(emptyResult is DeviceActionResult.Failure)

        val successResult = controller.postNotification(text = "Test alert from Telegram")
        assertTrue(successResult is DeviceActionResult.Success)
        assertTrue((successResult as DeviceActionResult.Success).message.contains("Test alert from Telegram"))
    }

    @Test
    fun `clipboard get and set preserves text in memory fallback`() {
        assertEquals(null, controller.getClipboard())

        val setSuccess = controller.setClipboard("https://tapbot.dev/download")
        assertTrue(setSuccess)
        assertEquals("https://tapbot.dev/download", controller.getClipboard())
    }

    @Test
    fun `speakText validates empty input and records spoken text in JVM fallback`() {
        val empty = controller.speakText("   ")
        assertTrue(empty is DeviceActionResult.Failure)

        val ok = controller.speakText("Dinner is ready")
        assertTrue(ok is DeviceActionResult.Success)
        assertTrue((ok as DeviceActionResult.Success).message.contains("Dinner is ready"))
        assertEquals("Dinner is ready", controller.getLastSpokenText())
        controller.shutdownTts()
    }

    @Test
    fun `getWifiDetails returns structured Wi-Fi and IP summary on JVM`() {
        val res = controller.getWifiDetails()
        assertTrue(res is DeviceActionResult.Success)
        val msg = (res as DeviceActionResult.Success).message
        assertTrue(msg.contains("Wi-Fi & Network Details"))
        assertTrue(msg.contains("Local IP"))
    }

    @Test
    fun `getLastKnownLocation checks permission override and formats Google Maps link`() {
        val deniedController = DeviceActionController(
            context = null,
            handler = null,
            hasLocationPermissionOverride = false
        )
        val denied = deniedController.getLastKnownLocation()
        assertTrue(denied is DeviceActionResult.Failure)
        assertTrue((denied as DeviceActionResult.Failure).errorMessage.contains("permission"))

        val allowed = controller.getLastKnownLocation()
        assertTrue(allowed is DeviceActionResult.Success)
        val msg = (allowed as DeviceActionResult.Success).message
        assertTrue(msg.contains("maps.google.com/?q="))
        assertTrue(msg.contains("37.421998"))
    }
}
