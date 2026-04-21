package com.example.ecosnap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TFLiteHelper {

    private Interpreter interpreter;

    // 🔥 KEMBALIKAN KE 640
    private final int INPUT_SIZE = 640;

    // 🔥 TURUNIN BIAR KELUAR SEMUA
    private final float CONF_THRESHOLD = 0.01f;

    public TFLiteHelper(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ByteBuffer loadModelFile(Context context) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("best_fp32.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(),
                fileDescriptor.getDeclaredLength());
    }

    private ByteBuffer convertBitmap(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255f);
            buffer.putFloat((pixel & 0xFF) / 255f);
        }

        return buffer;
    }

    public static class Result implements Serializable {
        public RectF rect;
        public String label;
        public float confidence;

        public Result(RectF rect, String label, float confidence) {
            this.rect = rect;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public List<Result> detect(Bitmap bitmap) {

        List<Result> results = new ArrayList<>();

        try {
            ByteBuffer input = convertBitmap(bitmap);

            float[][][] output = new float[1][11][8400];
            interpreter.run(input, output);

            String[] labels = {
                    "Organik","Kardus","Kaca",
                    "Logam","Kertas","Plastik","Bukan Sampah"
            };

            int imgWidth = bitmap.getWidth();
            int imgHeight = bitmap.getHeight();

            int count = 0; // 🔥 batasi max box biar nggak kebanyakan

            for (int i = 0; i < 8400; i++) {

                float x = output[0][0][i];
                float y = output[0][1][i];
                float w = output[0][2][i];
                float h = output[0][3][i];

                float objConf = output[0][4][i];

                int classId = -1;
                float maxClass = 0;

                for (int c = 5; c < 11; c++) {
                    if (output[0][c][i] > maxClass) {
                        maxClass = output[0][c][i];
                        classId = c - 5;
                    }
                }

                float finalConf = objConf * maxClass;

                if (finalConf > CONF_THRESHOLD && classId >= 0) {

                    // 🔥 FIX KOORDINAT (TANPA SCALE)
                    float left = x - w / 2;
                    float top = y - h / 2;
                    float right = x + w / 2;
                    float bottom = y + h / 2;

                    // 🔥 CLAMP BIAR TIDAK KELUAR LAYAR
                    left = Math.max(0, left);
                    top = Math.max(0, top);
                    right = Math.min(imgWidth, right);
                    bottom = Math.min(imgHeight, bottom);

                    results.add(new Result(
                            new RectF(left, top, right, bottom),
                            labels[classId],
                            finalConf * 100
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}