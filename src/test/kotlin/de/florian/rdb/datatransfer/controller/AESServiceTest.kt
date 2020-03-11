package de.florian.rdb.datatransfer.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

internal class AESServiceTest {

    @Test
    fun test_encrypt_decrypt() {
        val secretKey = "SECRET_KEY_ABC"

        val originalString = "Hidden_message_no_one_should_see..."
        val aes = AESService(secretKey)

        val encryptedString: String? = aes.encrypt(originalString)
        assertNotNull(encryptedString)

        val decryptedString: String? = aes.decrypt(encryptedString!!)

        assertEquals(originalString, decryptedString)
    }
}