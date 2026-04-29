package com.example.ecosnap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TFLiteHelper {

    private static final String TAG = "EcoSnapDetector";

    private Interpreter interpreter;

    private final int INPUT_SIZE = 640;

    private final float CONF_THRESHOLD = 0.50f;
    private final float NMS_IOU_THRESHOLD = 0.65f;
    private final float DUPLICATE_IOU_THRESHOLD = 0.75f;
    private final float EMA_ALPHA = 0.7f;
    private final float INTERPOLATION_ALPHA = 0.3f;
    private final float TRACKING_DISTANCE_FACTOR = 0.55f;
    private final long MAX_TRACKING_AGE_MS = 900L;

    // Tracking State
    private List<Result> trackedObjects = new ArrayList<>();
    private int nextObjectId = 1;
    private DebugStats lastDebugStats = new DebugStats();

    public TFLiteHelper(Context context) {
        initModel(context);
    }

    private void initModel(Context context) {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseXNNPACK(true);
            interpreter = new Interpreter(loadModelFile(context), options);
            Log.d(TAG, "Model loaded. inputShape="
                    + java.util.Arrays.toString(interpreter.getInputTensor(0).shape())
                    + " inputType=" + interpreter.getInputTensor(0).dataType()
                    + " outputShape=" + java.util.Arrays.toString(interpreter.getOutputTensor(0).shape())
                    + " outputType=" + interpreter.getOutputTensor(0).dataType());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TFLite model", e);
        }
    }

    private ByteBuffer loadModelFile(Context context) throws Exception {
        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = context.getAssets().openFd("best_float16.tflite");
        } catch (Exception e) {
            fileDescriptor = context.getAssets().openFd("best_fp32.tflite");
        }
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private ByteBuffer convertBitmap(Bitmap bitmap) {
        DataType inputType = interpreter.getInputTensor(0).dataType();
        int bytesPerChannel = inputType == DataType.FLOAT32 ? 4 : 1;
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytesPerChannel * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            if (inputType == DataType.FLOAT32) {
                buffer.putFloat(r / 255f);
                buffer.putFloat(g / 255f);
                buffer.putFloat(b / 255f);
            } else {
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
            }
        }
        buffer.rewind();
        return buffer;
    }

    public static class DebugStats {
        public long inferenceMs = 0L;
        public int rawDetections = 0;
        public int preNmsDetections = 0;
        public int postNmsDetections = 0;
        public int trackedDetections = 0;
    }

    public static class Result implements Serializable {
        public RectF rect;
        public String label;
        public float confidence;
        public int classId = -1;
        public int trackingId = -1;
        public boolean isLocked = false;
        public boolean isLowConfidence = false;
        public int stableFrames = 0;
        public int missedFrames = 0;
        public long lastUpdated = 0;

        public Result(RectF rect, String label, float confidence, int classId) {
            this.rect = rect;
            this.label = label;
            this.confidence = confidence;
            this.classId = classId;
            this.isLowConfidence = confidence < 70f;
            this.lastUpdated = System.currentTimeMillis();
        }

        public Result(Result other) {
            this.rect = new RectF(other.rect);
            this.label = other.label;
            this.confidence = other.confidence;
            this.classId = other.classId;
            this.trackingId = other.trackingId;
            this.isLocked = other.isLocked;
            this.isLowConfidence = other.isLowConfidence;
            this.stableFrames = other.stableFrames;
            this.missedFrames = other.missedFrames;
            this.lastUpdated = other.lastUpdated;
        }
    }

    public List<Result> detect(Bitmap bitmap) {
        List<Result> results = new ArrayList<>();
        List<Result> preNmsResults = new ArrayList<>();
        DebugStats stats = new DebugStats();

        try {
            if (interpreter == null) {
                Log.e(TAG, "detect() skipped because interpreter is null");
                lastDebugStats = stats;
                return results;
            }

            ByteBuffer input = convertBitmap(bitmap);

            int[] shape = interpreter.getOutputTensor(0).shape();
            float[][][] output = new float[shape[0]][shape[1]][shape[2]];
            long start = System.currentTimeMillis();
            Log.d(TAG, "Inference running");
            interpreter.run(input, output);
            stats.inferenceMs = System.currentTimeMillis() - start;

            String[] labels = {
                    "Organik","Kardus","Kaca",
                    "Logam","Kertas","Plastik","Bukan Sampah"
            };

            int imgWidth = bitmap.getWidth();
            int imgHeight = bitmap.getHeight();

            // ROI Settings (Center 60% of the screen)
            float roiLeft = imgWidth * 0.2f;
            float roiTop = imgHeight * 0.2f;
            float roiRight = imgWidth * 0.8f;
            float roiBottom = imgHeight * 0.8f;

            boolean isTransposed = shape[1] > shape[2];
            int numElements = isTransposed ? shape[1] : shape[2];
            int numChannels = isTransposed ? shape[2] : shape[1];
            boolean hasObjectness = numChannels == labels.length + 5;
            int classStart = hasObjectness ? 5 : 4;
            int numClasses = numChannels - classStart;

            for (int i = 0; i < numElements; i++) {
                float x = isTransposed ? output[0][i][0] : output[0][0][i];
                float y = isTransposed ? output[0][i][1] : output[0][1][i];
                float w = isTransposed ? output[0][i][2] : output[0][2][i];
                float h = isTransposed ? output[0][i][3] : output[0][3][i];
                float objectness = hasObjectness
                        ? (isTransposed ? output[0][i][4] : output[0][4][i])
                        : 1f;

                float maxClass = 0;
                int classId = -1;

                for (int c = 0; c < numClasses; c++) {
                    float classProb = isTransposed ? output[0][i][classStart + c] : output[0][classStart + c][i];
                    float prob = objectness * classProb;
                    if (prob > maxClass) {
                        maxClass = prob;
                        classId = c;
                    }
                }

                if (maxClass > 0.01f) {
                    stats.rawDetections++;
                }

                if (maxClass >= CONF_THRESHOLD && classId >= 0) {
                    boolean normalizedBox = Math.abs(x) <= 1.5f && Math.abs(y) <= 1.5f
                            && Math.abs(w) <= 1.5f && Math.abs(h) <= 1.5f;

                    float left;
                    float top;
                    float right;
                    float bottom;
                    if (normalizedBox) {
                        left = (x - w / 2) * imgWidth;
                        top = (y - h / 2) * imgHeight;
                        right = (x + w / 2) * imgWidth;
                        bottom = (y + h / 2) * imgHeight;
                    } else {
                        float scaleX = (float) imgWidth / INPUT_SIZE;
                        float scaleY = (float) imgHeight / INPUT_SIZE;
                        left = (x - w / 2) * scaleX;
                        top = (y - h / 2) * scaleY;
                        right = (x + w / 2) * scaleX;
                        bottom = (y + h / 2) * scaleY;
                    }

                    float centerX = (left + right) / 2f;
                    float centerY = (top + bottom) / 2f;

                    // ROI Boosting
                    boolean inROI = centerX > roiLeft && centerX < roiRight && centerY > roiTop && centerY < roiBottom;
                    if (inROI) {
                        maxClass += 0.10f; // Boost inside ROI
                    } else {
                        maxClass -= 0.15f; // Penalize outside ROI
                    }

                    if (maxClass < CONF_THRESHOLD) continue;

                    left = Math.max(0, left);
                    top = Math.max(0, top);
                    right = Math.min(imgWidth, right);
                    bottom = Math.min(imgHeight, bottom);

                    if (right <= left || bottom <= top) {
                        continue;
                    }

                    RectF candidateRect = new RectF(left, top, right, bottom);
                    if (isAbsurdTinyBox(candidateRect, imgWidth, imgHeight)) {
                        continue;
                    }

                    String labelName = classId < labels.length ? labels[classId] : "Unknown";

                    preNmsResults.add(new Result(
                            candidateRect,
                            labelName,
                            Math.min(maxClass * 100, 99.9f),
                            classId
                    ));
                }
            }

            stats.preNmsDetections = preNmsResults.size();
            List<Result> nmsResults = applyNMS(preNmsResults, NMS_IOU_THRESHOLD);
            List<Result> cleanedResults = mergeCentroidDuplicates(suppressSameClassDuplicates(nmsResults));
            stats.postNmsDetections = cleanedResults.size();
            results = updateTracking(cleanedResults);
            stats.trackedDetections = results.size();

        } catch (Exception e) {
            Log.e(TAG, "Detection failed", e);
        }

        lastDebugStats = stats;
        return results;
    }

    public DebugStats getLastDebugStats() {
        return lastDebugStats;
    }

    private List<Result> updateTracking(List<Result> currentResults) {
        long now = System.currentTimeMillis();
        List<Result> activeTracks = new ArrayList<>();
        boolean[] matchedTracks = new boolean[trackedObjects.size()];

        for (Result curr : currentResults) {
            Result bestMatch = null;
            int bestMatchIndex = -1;
            float bestDistance = Float.MAX_VALUE;
            float maxDistance = Math.max(48f, diagonal(curr.rect) * TRACKING_DISTANCE_FACTOR);

            for (int i = 0; i < trackedObjects.size(); i++) {
                if (matchedTracks[i]) {
                    continue;
                }
                Result tracked = trackedObjects.get(i);
                if (tracked.classId != curr.classId) {
                    continue;
                }
                float distance = calculateCentroidDistance(curr.rect, tracked.rect);
                if (distance < maxDistance && distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = tracked;
                    bestMatchIndex = i;
                }
            }

            if (bestMatch != null) {
                matchedTracks[bestMatchIndex] = true;
                curr.trackingId = bestMatch.trackingId;
                curr.stableFrames = bestMatch.stableFrames + 1;
                curr.missedFrames = 0;
                curr.lastUpdated = now;

                if (curr.confidence < 70f) {
                    curr.rect = new RectF(bestMatch.rect);
                    curr.isLowConfidence = true;
                } else {
                    RectF emaRect = applyEma(curr.rect, bestMatch.rect);
                    curr.rect = interpolateRect(emaRect, bestMatch.rect);
                    curr.confidence = curr.confidence * EMA_ALPHA + bestMatch.confidence * (1f - EMA_ALPHA);
                    curr.isLowConfidence = false;
                }

                if (!curr.label.equals(bestMatch.label)) {
                    if (curr.confidence < bestMatch.confidence + 15f) {
                        curr.label = bestMatch.label;
                        curr.classId = bestMatch.classId;
                    }
                }

                curr.isLocked = curr.stableFrames > 10 && curr.confidence > 90f;

                activeTracks.add(curr);
            } else {
                curr.trackingId = nextObjectId++;
                curr.lastUpdated = now;
                curr.missedFrames = 0;
                activeTracks.add(curr);
            }
        }

        for (int i = 0; i < trackedObjects.size(); i++) {
            if (matchedTracks[i]) {
                continue;
            }
            Result oldTrack = trackedObjects.get(i);
            if (now - oldTrack.lastUpdated < MAX_TRACKING_AGE_MS && oldTrack.missedFrames < 4) {
                oldTrack.missedFrames++;
                activeTracks.add(oldTrack);
            }
        }

        trackedObjects = new ArrayList<>(activeTracks);
        if (trackedObjects.isEmpty()) {
            nextObjectId = 1;
        }
        return activeTracks;
    }

    private RectF applyEma(RectF current, RectF previous) {
        float currentCenterX = current.centerX();
        float currentCenterY = current.centerY();
        float currentWidth = current.width();
        float currentHeight = current.height();

        float previousCenterX = previous.centerX();
        float previousCenterY = previous.centerY();
        float previousWidth = previous.width();
        float previousHeight = previous.height();

        float centerX = currentCenterX * EMA_ALPHA + previousCenterX * (1f - EMA_ALPHA);
        float centerY = currentCenterY * EMA_ALPHA + previousCenterY * (1f - EMA_ALPHA);
        float width = currentWidth * EMA_ALPHA + previousWidth * (1f - EMA_ALPHA);
        float height = currentHeight * EMA_ALPHA + previousHeight * (1f - EMA_ALPHA);

        return rectFromCenter(centerX, centerY, width, height);
    }

    private RectF interpolateRect(RectF target, RectF previous) {
        float left = previous.left + (target.left - previous.left) * INTERPOLATION_ALPHA;
        float top = previous.top + (target.top - previous.top) * INTERPOLATION_ALPHA;
        float right = previous.right + (target.right - previous.right) * INTERPOLATION_ALPHA;
        float bottom = previous.bottom + (target.bottom - previous.bottom) * INTERPOLATION_ALPHA;
        return new RectF(left, top, right, bottom);
    }

    private RectF rectFromCenter(float centerX, float centerY, float width, float height) {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        return new RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
    }

    private float calculateCentroidDistance(RectF a, RectF b) {
        float dx = a.centerX() - b.centerX();
        float dy = a.centerY() - b.centerY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private List<Result> applyNMS(List<Result> boxes, float iouThreshold) {
        List<Result> nmsList = new ArrayList<>();
        boxes.sort((o1, o2) -> Float.compare(o2.confidence, o1.confidence));
        boolean[] active = new boolean[boxes.size()];
        for (int i = 0; i < active.length; i++) active[i] = true;

        for (int i = 0; i < boxes.size(); i++) {
            if (!active[i]) continue;
            Result box1 = boxes.get(i);
            nmsList.add(box1);

            for (int j = i + 1; j < boxes.size(); j++) {
                if (!active[j]) continue;
                Result box2 = boxes.get(j);
                if (box1.classId == box2.classId && calculateIoU(box1.rect, box2.rect) > iouThreshold) {
                    active[j] = false;
                }
            }
        }
        return nmsList;
    }

    private List<Result> suppressSameClassDuplicates(List<Result> boxes) {
        List<Result> kept = new ArrayList<>();
        boxes.sort((a, b) -> Float.compare(b.confidence, a.confidence));

        for (Result candidate : boxes) {
            boolean duplicate = false;
            for (Result existing : kept) {
                if (candidate.classId != existing.classId) {
                    continue;
                }
                if (calculateIoU(candidate.rect, existing.rect) > DUPLICATE_IOU_THRESHOLD) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private List<Result> mergeCentroidDuplicates(List<Result> boxes) {
        List<Result> merged = new ArrayList<>();

        for (Result candidate : boxes) {
            Result duplicate = null;
            for (Result existing : merged) {
                if (candidate.classId != existing.classId) {
                    continue;
                }

                float centroidDistance = calculateCentroidDistance(candidate.rect, existing.rect);
                float nearThreshold = Math.max(18f, Math.min(diagonal(candidate.rect), diagonal(existing.rect)) * 0.22f);
                float iou = calculateIoU(candidate.rect, existing.rect);
                boolean sameObject = centroidDistance < nearThreshold || (centroidDistance < nearThreshold * 1.5f && iou > 0.35f);
                if (sameObject) {
                    duplicate = existing;
                    break;
                }
            }

            if (duplicate == null) {
                merged.add(candidate);
            } else if (candidate.confidence > duplicate.confidence) {
                int index = merged.indexOf(duplicate);
                merged.set(index, candidate);
            }
        }

        return merged;
    }

    private boolean isAbsurdTinyBox(RectF rect, int imgWidth, int imgHeight) {
        float minSide = Math.min(imgWidth, imgHeight);
        float minBoxSide = Math.max(10f, minSide * 0.018f);
        float minArea = imgWidth * imgHeight * 0.0005f;
        return rect.width() < minBoxSide || rect.height() < minBoxSide || rect.width() * rect.height() < minArea;
    }

    private float diagonal(RectF rect) {
        return (float) Math.sqrt(rect.width() * rect.width() + rect.height() * rect.height());
    }

    private float calculateIoU(RectF a, RectF b) {
        float x1 = Math.max(a.left, b.left);
        float y1 = Math.max(a.top, b.top);
        float x2 = Math.min(a.right, b.right);
        float y2 = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);
        float union = areaA + areaB - intersectionArea;
        if (union <= 0f) return 0f;
        return intersectionArea / union;
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
