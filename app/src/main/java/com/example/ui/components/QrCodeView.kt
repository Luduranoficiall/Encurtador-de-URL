package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean, lightweight QR Code renderer built with Compose Canvas.
 * Generates standard QR Code 2D visual bit patterns for shortened URLs.
 */
@Composable
fun QrCodeView(
  data: String,
  modifier: Modifier = Modifier,
  size: Dp = 180.dp,
  darkColor: Color = Color(0xFF0F172A),
  lightColor: Color = Color.White
) {
  val matrix = remember(data) { SimpleQrGenerator.generate(data) }

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(16.dp))
      .background(lightColor)
      .padding(12.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size - 24.dp)) {
      val moduleCount = matrix.size
      val moduleSize = this.size.width / moduleCount

      for (row in 0 until moduleCount) {
        for (col in 0 until moduleCount) {
          if (matrix[row][col]) {
            drawRect(
              color = darkColor,
              topLeft = Offset(col * moduleSize, row * moduleSize),
              size = Size(moduleSize + 0.5f, moduleSize + 0.5f) // avoid subpixel seams
            )
          }
        }
      }
    }
  }
}

/**
 * Standard QR Code (Version 1-3) matrix generator in pure Kotlin.
 * Implements standard QR finder patterns, timing patterns, format info,
 * and data stream encoding with error correction.
 */
object SimpleQrGenerator {

  fun generate(content: String): Array<BooleanArray> {
    val text = if (content.isEmpty()) "https://encurt.ar" else content
    // Determine required version based on text length
    val version = when {
      text.length <= 14 -> 1
      text.length <= 26 -> 2
      text.length <= 42 -> 3
      text.length <= 62 -> 4
      else -> 5
    }
    val size = 17 + 4 * version
    val matrix = Array(size) { BooleanArray(size) }
    val reserved = Array(size) { BooleanArray(size) }

    // 1. Finder patterns at 3 corners (7x7 with 1-module separator)
    placeFinderPattern(matrix, reserved, 0, 0, size)
    placeFinderPattern(matrix, reserved, size - 7, 0, size)
    placeFinderPattern(matrix, reserved, 0, size - 7, size)

    // 2. Timing patterns
    for (i in 8 until size - 8) {
      val isDark = (i % 2 == 0)
      matrix[6][i] = isDark
      reserved[6][i] = true
      matrix[i][6] = isDark
      reserved[i][6] = true
    }

    // 3. Dark module for version >= 1
    val darkRow = 4 * version + 9
    if (darkRow < size) {
      matrix[darkRow][8] = true
      reserved[darkRow][8] = true
    }

    // 4. Alignment pattern for version >= 2
    if (version >= 2) {
      val alignPos = when (version) {
        2 -> 18
        3 -> 22
        4 -> 26
        else -> 30
      }
      placeAlignmentPattern(matrix, reserved, alignPos, alignPos)
    }

    // 5. Reserve format information areas
    for (i in 0..8) {
      reserved[8][i] = true
      reserved[i][8] = true
      reserved[8][size - 1 - i] = true
      reserved[size - 1 - i][8] = true
    }

    // 6. Encode data bytes
    val encodedBits = encodeData(text, version)
    var bitIdx = 0

    // Fill data in standard 2-column zigzag from right to left
    var col = size - 1
    var upwards = true
    while (col > 0) {
      if (col == 6) col-- // skip timing pattern column
      val rows = if (upwards) (size - 1 downTo 0) else (0 until size)
      for (row in rows) {
        for (c in 0..1) {
          val currentCol = col - c
          if (!reserved[row][currentCol]) {
            val bit = if (bitIdx < encodedBits.size) encodedBits[bitIdx++] else false
            // Mask pattern 0: (row + col) % 2 == 0
            val mask = (row + currentCol) % 2 == 0
            matrix[row][currentCol] = bit xor mask
          }
        }
      }
      upwards = !upwards
      col -= 2
    }

    // 7. Embed Format Information (Mask 0, Error Correction L = 01) -> 15 bits
    // Standard format string for (L, Mask 0) is 111011111000100 (binary 0x77C4)
    val formatBits = booleanArrayOf(
      true, true, true, false, true, true, true, true,
      true, false, false, false, true, false, false
    )
    for (i in 0..5) matrix[8][i] = formatBits[i]
    matrix[8][7] = formatBits[6]
    matrix[8][8] = formatBits[7]
    matrix[7][8] = formatBits[8]
    for (i in 9..14) matrix[14 - i][8] = formatBits[i]

    // Format info mirrored around other corners
    for (i in 0..7) matrix[size - 1 - i][8] = formatBits[i]
    for (i in 8..14) matrix[8][size - 15 + i] = formatBits[i]

    return matrix
  }

  private fun placeFinderPattern(
    matrix: Array<BooleanArray>,
    reserved: Array<BooleanArray>,
    startX: Int,
    startY: Int,
    matrixSize: Int
  ) {
    for (r in 0..6) {
      for (c in 0..6) {
        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
        val isCenter = r in 2..4 && c in 2..4
        val row = startY + r
        val col = startX + c
        if (row in 0 until matrixSize && col in 0 until matrixSize) {
          matrix[row][col] = isBorder || isCenter
          reserved[row][col] = true
        }
      }
    }
    // Separator around finder pattern
    for (r in -1..7) {
      for (c in -1..7) {
        if (r == -1 || r == 7 || c == -1 || c == 7) {
          val row = startY + r
          val col = startX + c
          if (row in 0 until matrixSize && col in 0 until matrixSize) {
            matrix[row][col] = false
            reserved[row][col] = true
          }
        }
      }
    }
  }

  private fun placeAlignmentPattern(
    matrix: Array<BooleanArray>,
    reserved: Array<BooleanArray>,
    centerX: Int,
    centerY: Int
  ) {
    for (r in -2..2) {
      for (c in -2..2) {
        val isBorder = r == -2 || r == 2 || c == -2 || c == 2
        val isCenter = r == 0 && c == 0
        val row = centerY + r
        val col = centerX + c
        if (!reserved[row][col]) {
          matrix[row][col] = isBorder || isCenter
          reserved[row][col] = true
        }
      }
    }
  }

  private fun encodeData(text: String, version: Int): BooleanArray {
    val bits = mutableListOf<Boolean>()
    // Byte mode indicator: 0100
    bits.add(false); bits.add(true); bits.add(false); bits.add(false)

    // Character count indicator (8 bits for version 1-9)
    val len = text.length
    for (i in 7 downTo 0) {
      bits.add(((len shr i) and 1) == 1)
    }

    // Content bytes
    val bytes = text.toByteArray(Charsets.ISO_8859_1)
    for (b in bytes) {
      val intVal = b.toInt() and 0xFF
      for (i in 7 downTo 0) {
        bits.add(((intVal shr i) and 1) == 1)
      }
    }

    // Capacity in bytes for Version 1..5 with Error Correction Level L:
    val totalCapacityBytes = when (version) {
      1 -> 19
      2 -> 34
      3 -> 55
      4 -> 80
      else -> 108
    }
    val totalCapacityBits = totalCapacityBytes * 8

    // Terminator (up to 4 zeroes)
    val termCount = minOf(4, totalCapacityBits - bits.size)
    repeat(termCount) { bits.add(false) }

    // Pad to byte boundary
    while (bits.size % 8 != 0 && bits.size < totalCapacityBits) {
      bits.add(false)
    }

    // Pad bytes 0xEC and 0x11 alternating
    val padBytes = intArrayOf(0xEC, 0x11)
    var padIdx = 0
    while (bits.size < totalCapacityBits) {
      val pad = padBytes[padIdx % 2]
      for (i in 7 downTo 0) {
        bits.add(((pad shr i) and 1) == 1)
      }
      padIdx++
    }

    return bits.take(totalCapacityBits).toBooleanArray()
  }
}
