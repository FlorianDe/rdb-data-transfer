package de.florian.rdb.datatransfer.controller

import org.slf4j.LoggerFactory
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class AESService(keyStr: String) {
    private val  log = LoggerFactory.getLogger(javaClass)

    private var secretKey: SecretKeySpec? = null
    private var key: ByteArray = ByteArray(0)

    init {
        setKey(keyStr)
    }

    fun setKey(keyStr: String) {
        try {
            key = keyStr.toByteArray(charset("UTF-8"))
            val sha = MessageDigest.getInstance("SHA-1")
            key = sha.digest(key)
            key = key.copyOf(16)
            secretKey = SecretKeySpec(key, "AES")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    fun encrypt(strToEncrypt: String): String? {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding").apply { init(Cipher.ENCRYPT_MODE, secretKey) }
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.toByteArray(charset("UTF-8"))))
        } catch (e: Exception) {
            log.error("Error while encrypting: $e")
            null
        }
    }

    fun decrypt(strToDecrypt: String): String? {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING").apply { init(Cipher.DECRYPT_MODE, secretKey) }
            return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
        } catch (e: Exception) {
            log.error("Error while decrypting: $e")
            null
        }
    }
}
