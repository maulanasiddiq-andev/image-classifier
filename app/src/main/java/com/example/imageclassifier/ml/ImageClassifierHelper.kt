package com.example.imageclassifier.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class ImageClassifierHelper {
    fun loadInterPreter(context: Context): Interpreter {
        val modelFile = FileUtil.loadMappedFile(context, "model.tflite")
        return Interpreter(modelFile)
    }

    fun classifyBitmap(interpreter: Interpreter, bitmap: Bitmap): FloatArray {
        var tensorImage = TensorImage.fromBitmap(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), org.tensorflow.lite.DataType.FLOAT32)

        interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        return outputBuffer.floatArray
    }
}