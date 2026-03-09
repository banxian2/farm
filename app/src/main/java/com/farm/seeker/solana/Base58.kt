package com.farm.seeker.solana

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val ALPHABET_ARRAY = ALPHABET.toCharArray()

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) {
            return ""
        }
        
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) {
            zeros++
        }
        
        val inputCopy = input.copyOf(input.size)
        val encoded = CharArray(inputCopy.size * 2)
        var outputStart = encoded.size
        
        var inputStart = zeros
        while (inputStart < inputCopy.size) {
            outputStart--
            encoded[outputStart] = ALPHABET_ARRAY[divmod(inputCopy, inputStart, 256, 58)]
            if (inputCopy[inputStart] == 0.toByte()) {
                inputStart++
            }
        }
        
        while (outputStart < encoded.size && encoded[outputStart] == ALPHABET_ARRAY[0]) {
            outputStart++
        }
        
        while (--zeros >= 0) {
            outputStart--
            encoded[outputStart] = ALPHABET_ARRAY[0]
        }
        
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Int {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) {
            return ByteArray(0)
        }

        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            if (digit < 0) {
                throw IllegalArgumentException("Illegal character $c at index $i")
            }
            input58[i] = digit.toByte()
        }

        var zeros = 0
        while (zeros < input58.size && input58[zeros] == 0.toByte()) {
            zeros++
        }

        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256).toByte()
            if (input58[inputStart] == 0.toByte()) {
                inputStart++
            }
        }

        while (outputStart < decoded.size && decoded[outputStart] == 0.toByte()) {
            outputStart++
        }

        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    private val INDEXES = IntArray(128) { -1 }

    init {
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].code] = i
        }
    }
}
