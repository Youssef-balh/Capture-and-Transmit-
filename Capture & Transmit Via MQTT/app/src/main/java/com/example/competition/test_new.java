    package com.example.competition;

    import androidx.camera.core.Preview;


    import androidx.appcompat.app.AppCompatActivity;

    import android.content.Context;
    import android.os.PowerManager;
    import android.view.Surface;
    import android.Manifest;
    import android.content.pm.PackageManager;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Canvas;
    import android.graphics.ColorMatrix;
    import android.graphics.ColorMatrixColorFilter;
    import android.graphics.Paint;
    import android.os.Bundle;
    import android.util.Base64;
    import android.util.Log;
    import android.view.TextureView;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;

    import androidx.camera.core.CameraInfoUnavailableException;
    import androidx.camera.core.CameraSelector;
    import androidx.camera.core.ImageCapture;
    import androidx.camera.core.ImageCaptureException;
    import androidx.camera.core.ImageProxy;
    import androidx.camera.core.SurfaceRequest;
    import androidx.camera.lifecycle.ProcessCameraProvider;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;


    import com.google.common.util.concurrent.ListenableFuture;

    import org.eclipse.paho.client.mqttv3.MqttCallback;
    import org.eclipse.paho.client.mqttv3.MqttMessage;

    import java.io.ByteArrayOutputStream;
    import java.nio.ByteBuffer;
    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.Executor;
    import java.util.concurrent.Executors;



    public class test_new extends AppCompatActivity {

        private PowerManager.WakeLock wakeLock;
        private static final String TAG = "APP_MQTT_CAMERA";
        private MqttHandler mqttHandler;
        private TextView messageTextView;
        private TextView connectionStatusTextView;
        private TextView subscriptionStatusTextView;

        private int messageCounter_s = 1;
        private int messageCounter_p = 1;
        //camera Section
        private ImageCapture imageCapture;
        private Executor executor = Executors.newSingleThreadExecutor();
        private ProcessCameraProvider cameraProvider;

        private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
            private String ipAddress, port, TOPIC_RECEIVE, TOPIC_PUBLISH,brokerUrl;
        private String clientId = "TestClient";

        private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

        Button returnButton;
        Button submitButton;
        EditText editTextIpAddress,editTextPort,editTextRequestTopic,editTextResponseTopic;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);



            editTextIpAddress = findViewById(R.id.editTextIpAddress);
            editTextPort = findViewById(R.id.editTextPort);
            editTextRequestTopic = findViewById(R.id.editTextRequestTopic);
            editTextResponseTopic = findViewById(R.id.editTextResponseTopic);

            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakeLock");

            messageTextView = findViewById(R.id.messageTextView);
            connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
            subscriptionStatusTextView = findViewById(R.id.subscriptionStatusTextView);

            /*returnButton = findViewById(R.id.returnButton);*/
            /*returnButton.setVisibility(View.GONE);*/

            submitButton = findViewById(R.id.submitButton);


            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ipAddress = editTextIpAddress.getText().toString();
                    port = editTextPort.getText().toString();
                    TOPIC_RECEIVE = editTextRequestTopic.getText().toString();
                    TOPIC_PUBLISH = editTextResponseTopic.getText().toString();
                    brokerUrl = "tcp://" + ipAddress + ":" + port;
                    onResume();
                }
            });
        }


        @Override
        protected void onResume() {
            super.onResume();
            if (validateInput() && brokerUrl != null) {
                mqttHandler = new MqttHandler();
                mqttHandler.connect(brokerUrl, clientId);
                if (checkBackCamera() && checkCameraPermission()) {
                    if (mqttHandler.isConnected() && mqttHandler != null) {
                        handleConnectedState();
                        removeEditTexts();
                        previewStart();
                    } else {
                        handleDisconnectedState();
                    }
                } else {
                    Log.e(TAG, "Back-facing camera not available");
                    requestCameraPermission();
                }
                if (wakeLock != null && !wakeLock.isHeld()) {
                    wakeLock.acquire();
                }

            } else {
                // Invalid input, show error message
                Toast.makeText(test_new.this, "Invalid input. Please fill all fields.", Toast.LENGTH_SHORT).show();
            }

        }


        private boolean validateInput() {
            return ipAddress != null && !ipAddress.isEmpty()
                    && port != null && !port.isEmpty()
                    && TOPIC_RECEIVE != null && !TOPIC_RECEIVE.isEmpty()
                    && TOPIC_PUBLISH != null && !TOPIC_PUBLISH.isEmpty();
        }

        private void removeEditTexts() {
            ViewGroup layout = findViewById(R.id.mainLayout);
            layout.removeView(editTextIpAddress);
            layout.removeView(editTextPort);
            layout.removeView(editTextRequestTopic);
            layout.removeView(editTextResponseTopic);
            layout.removeView(submitButton);
        }
        private void previewStart() {
            TextureView textureView = findViewById(R.id.textureView);

            cameraProviderFuture = ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    // Camera provider is now guaranteed to be available
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Bind the preview use case to the lifecycle
                    bindPreview(cameraProvider, textureView);

                } catch (Exception e) {
                    Log.e(TAG, "Error initializing CameraX", e);
                    Toast.makeText(this, "Error initializing CameraX", Toast.LENGTH_SHORT).show();
                }
            }, ContextCompat.getMainExecutor(this));
        }

        private void bindPreview(ProcessCameraProvider cameraProvider, TextureView textureView) {
            Preview preview = new Preview.Builder().build();

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            // Create a surface provider using SurfaceRequest API
            preview.setSurfaceProvider(new Preview.SurfaceProvider() {
                @Override
                public void onSurfaceRequested(SurfaceRequest request) {
                    if (textureView.getSurfaceTexture() != null) {
                        request.provideSurface(new Surface(textureView.getSurfaceTexture()), Executors.newSingleThreadExecutor(),
                                result -> {

                                });
                    }
                }
            });

            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        }


        private void handleConnectedState() {
            Log.d(TAG, "Connected to MQTT broker");
            runOnUiThread(() -> {
                Toast.makeText(test_new.this,"Connected", Toast.LENGTH_SHORT).show();
                connectionStatusTextView.setText("\nConnected");
            });
/*
            returnButton.setVisibility(View.VISIBLE);
*/


            // Subscribe to the MQTT topic
            mqttHandler.subscribe(TOPIC_RECEIVE);
            runOnUiThread(() -> subscriptionStatusTextView.setText("\n\nSubscribed to topic:" + TOPIC_RECEIVE));
            // Set up the callback for handling incoming messages
            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build();

            if (imageCapture != null) {
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
                Log.d(TAG, "ImageCapture is bound to the camera device");
                setupMqttCallback();

            } else {
                Log.e(TAG, "ImageCapture is null. Unable to capture photos.");
                runOnUiThread(() ->
                        Toast.makeText(this, "Error initializing ImageCapture", Toast.LENGTH_SHORT).show()
                );
            }


        }

        private void handleDisconnectedState() {
            Log.d(TAG, "Failed to connect to MQTT broker");
            runOnUiThread(() ->{
                Toast.makeText(this, "Failed to connect try again", Toast.LENGTH_SHORT).show();
                connectionStatusTextView.setText("Failed to connect");
/*
                returnButton.setVisibility(View.GONE);
*/

            });
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            // Disconnect from the MQTT broker when the activity is destroyed
            if (mqttHandler != null) {
                mqttHandler.disconnect();
            }
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
                cameraProvider = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }


        private void setupMqttCallback() {
            mqttHandler.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    String receivedMessage = new String(mqttMessage.getPayload());
                    Log.d(TAG, "Received message: " + receivedMessage);

                    // Update the TextView with the received message
                    updateSubscriptionStatus(receivedMessage);
                    // Publish a new message
                    captureAndPublishPhoto();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Connection lost");
                    runOnUiThread(() -> connectionStatusTextView.setText("\n\n\nDisconnected")); // Update connection status TextView
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
                    // Not used in this example
                }
            });
        }
        private void updateSubscriptionStatus(String receivedMessage) {
            runOnUiThread(() -> {
                String formattedMessage = "\n\n\n\n Received message " + messageCounter_s + ": " + receivedMessage;
                subscriptionStatusTextView.setText(formattedMessage);
                messageCounter_s++;
            });
        }
        private boolean checkBackCamera() {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                Log.d(TAG,"Back-facing camera is available and good");

                return cameraProvider.hasCamera(cameraSelector);
            } catch (CameraInfoUnavailableException | ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error checking back-facing camera availability", e);
                return false;
            }
        }

        private void captureAndPublishPhoto() {
            ImageCapture.Metadata metadata = new ImageCapture.Metadata();
            metadata.setReversedHorizontal(true);


            if (imageCapture.getTargetRotation() == Surface.ROTATION_0) {
                imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try {
                            // Resize the image to 256x256
                            Bitmap resizedBitmap = resizeImage(image, 256, 256);

                            // Convert the image to black and white
                            Bitmap blackAndWhiteBitmap = convertToBlackAndWhite(resizedBitmap);

                            // Encode the bitmap as a Base64 string
                            String base64Image = encodeBitmapToBase64(blackAndWhiteBitmap);

                            // Publish the Base64-encoded image to the MQTT topic
                            mqttHandler.publish(TOPIC_PUBLISH, base64Image);

                            // Update UI
                            runOnUiThread(() -> {
                                String formattedMessage = "\n\n\n\n\n Published " + messageCounter_p + ": Image";
                                messageTextView.setText(formattedMessage);
                                messageCounter_p++;
                                Log.d(TAG,"Photo sent");
                            });
                        } finally {
                            // Close the image proxy in a finally block to ensure it gets closed
                            image.close();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error capturing photo", exception);
                        runOnUiThread(() ->
                                Toast.makeText(test_new.this, "Error capturing photo", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } else {
                Log.e(TAG, "ImageCapture is not bound to a valid camera.");
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: ImageCapture is not bound to a valid camera", Toast.LENGTH_SHORT).show()
                );
            }
        }
        private boolean checkCameraPermission() {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        private void requestCameraPermission() {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onResume();
                } else {
                    Log.e(TAG, "Camera permission denied");
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
        private Bitmap resizeImage(ImageProxy image, int targetWidth, int targetHeight) {
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            return Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options), targetWidth, targetHeight, false);
        }
        private Bitmap convertToBlackAndWhite(Bitmap originalBitmap) {
            Bitmap blackAndWhiteBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(blackAndWhiteBitmap);
            Paint paint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            paint.setColorFilter(filter);
            canvas.drawBitmap(originalBitmap, 0, 0, paint);
            return blackAndWhiteBitmap;
        }
        private String encodeBitmapToBase64(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        }
    }