package com.rvetiveau.rvscorecount.data.scan

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rvetiveau.rvscorecount.domain.scan.ScannedSymbol
import com.rvetiveau.rvscorecount.domain.scan.ScannedWord
import com.rvetiveau.rvscorecount.domain.scan.TrumpReader
import com.rvetiveau.rvscorecount.domain.scan.TrumpReading
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the numbers of cards held in a fan with ML Kit's on-device text recognition. The cards of a
 * fan lean more and more, and ML Kit reads text best when level: the photo is read once per angle of
 * [ANGLES], and [TrumpReading] merges what every pass saw.
 */
@Singleton
class MlKitTrumpReader @Inject constructor() : TrumpReader {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun read(photoPath: String, numbers: IntRange): Set<Int> = withContext(Dispatchers.Default) {
        val file = File(photoPath)
        val photo = try {
            decode(file)
        } finally {
            file.delete()
        }
        TrumpReading.numbers(ANGLES.flatMap { angle -> words(photo, angle) }, numbers)
    }

    /** Upright as shot (EXIF applied) and at most [MAX_SIDE] pixels long, the size the reading was tuned on. */
    private fun decode(file: File): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }

    /** Words read on [photo] turned by [angle] degrees clockwise, mapped back to the photo's frame. */
    private fun words(photo: Bitmap, angle: Int): List<ScannedWord> {
        val rotation = Matrix().apply { postRotate(angle.toFloat()) }
        val rotated = if (angle == 0) photo else Bitmap.createBitmap(photo, 0, 0, photo.width, photo.height, rotation, true)
        // createBitmap shifts the rotated photo back to the origin: the same shift maps points into it.
        val bounds = RectF(0f, 0f, photo.width.toFloat(), photo.height.toFloat())
        rotation.mapRect(bounds)
        val toRotated = Matrix(rotation).apply { postTranslate(-bounds.left, -bounds.top) }
        val toPhoto = Matrix().also(toRotated::invert)

        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(rotated, 0)))
        return text.textBlocks.flatMap { it.lines }.flatMap { it.elements }.map { element ->
            ScannedWord(
                symbols = element.symbols.mapNotNull { symbol ->
                    val box = symbol.boundingBox ?: return@mapNotNull null
                    val center = floatArrayOf(box.exactCenterX(), box.exactCenterY()).also(toPhoto::mapPoints)
                    ScannedSymbol(
                        text = symbol.text,
                        x = center[0],
                        y = center[1],
                        height = height(symbol.cornerPoints),
                        confidence = symbol.confidence
                    )
                },
                angle = element.angle - angle
            )
        }
    }

    /** Left edge of the corners (top-left, top-right, bottom-right, bottom-left): the text's own height. */
    private fun height(corners: Array<Point>?): Float {
        if (corners == null || corners.size < 4) return 0f
        return hypot((corners[3].x - corners[0].x).toFloat(), (corners[3].y - corners[0].y).toFloat())
    }

    private companion object {
        const val MAX_SIDE = 2000
        val ANGLES = listOf(0, 30, 60, 90, -30, -60, -90)
    }
}
