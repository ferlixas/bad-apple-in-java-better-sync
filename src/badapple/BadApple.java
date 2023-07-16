// 원래 30프레임인걸 추출할 때 60프레임으로 해서 같은 프레임이 2개 껴있는 식임.
// When extracting the original 30fps, it was extracted at 60fps, so the same frame is stuck.
package badapple;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BadApple extends Application {
    private static final int FRAME_WIDTH = 1355;
    private static final int FRAME_HEIGHT = 940;
    private static final int FONT_SIZE = 16;
    // Adjust the sync of music and animation.
    // 음악하고 애니메이션의 싱크 조절
    private static final double AUDIO_DELAY = 0.7;
    private final String AUDIO_FILE = "badapple/bad_apple.mp3";
    private List<String> lines;
    private int frameIndex;
    private int lineY;

    private ScheduledExecutorService executor;
    private VBox vbox;
    private Label frameLabel;
    private Label timeLabel;
    private long startTime;
    private MediaPlayer mediaPlayer;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        lines = new ArrayList<>();
        frameIndex = 0;
        lineY = 0;

        loadTextFile();

        primaryStage.setTitle("Bad Apple");
        Group root = new Group();
        Scene scene = new Scene(root, FRAME_WIDTH, FRAME_HEIGHT);

        frameLabel = createLabel(FRAME_WIDTH - 70, 10);
        timeLabel = createLabel(FRAME_WIDTH - 300, 10);

        vbox = new VBox(frameLabel, timeLabel);
        root.getChildren().add(vbox);

        primaryStage.setScene(scene);
        primaryStage.show();

        playTextFile();
        startTime = System.nanoTime();
        playAudioFile();

        // Program exit when JavaFX app is closed
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private Label createLabel(double layoutX, double layoutY) {
        Label label = new Label();
        label.setStyle("-fx-font-family: Consolas; -fx-font-size: " + FONT_SIZE + "px;");
        label.setLayoutX(layoutX);
        label.setLayoutY(layoutY);
        return label;
    }

    private void loadTextFile() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("bad_apple_frames.txt")), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playTextFile() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        double delayInMillis = 16.66666666666666667; // 1000 / 16.666666.. = 60 (fps)
        long delayInNanos = (long) (delayInMillis * 1_000_000);

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::renderNextFrame, 0, delayInNanos, TimeUnit.NANOSECONDS);
    }

    private void playAudioFile() {
        try {
            Thread.sleep((long) (AUDIO_DELAY * 1000));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            // 오디오 파일을 임시저장 파일에 저장
            // Save the audio file to a temporary file
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(AUDIO_FILE);
            File tempFile = File.createTempFile("bad_apple_temp", ".mp3");
            tempFile.deleteOnExit();

            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 임시 파일 경로로 Media 객체 생성
            // Create the Media object using the temporary file path
            String tempFilePath = tempFile.toURI().toString();
            Media media = new Media(tempFilePath);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            });

            mediaPlayer.setOnError(() -> {
                System.out.println("MediaPlayer error occurred: " + mediaPlayer.getError());
            });

            mediaPlayer.play();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderNextFrame() {
        if (frameIndex >= lines.size()) {
            executor.shutdown();
            Platform.exit();
            // 애니메이션 종료 후 JavaFX 애플리케이션 종료
            // If animation is finished, close JavaFx application
            System.exit(0);
            return;
        }

        List<String> currentFrame = new ArrayList<>();
        int lineCount = 0;

        // 텍스트 라인 50개마다 1프레임으로 계산, 빈 줄을 만나면 다음 프레임으로.
        // 1 frame for every 50 text lines / On empty line, skip that line and move on to the next frame
        while (lineCount < 50 && frameIndex < lines.size()) {
            String line = lines.get(frameIndex).trim();
            if (!line.isEmpty()) {
                currentFrame.add(line);
                lineCount++;
            }
            frameIndex++;
        }

        // 현재 프레임 번호 표시
        // Display current frame number
        int currentFrameIndex = frameIndex / 52 + 1;
        String frameText = "Frame: " + currentFrameIndex;
        Platform.runLater(() -> {
            frameLabel.setText(frameText);
            renderFrame(currentFrame);
        });

        // 현재 경과 시간 표시
        // Display current elapsed time
        Duration elapsedTime = Duration.ofNanos(System.nanoTime() - startTime);
        Platform.runLater(() -> {
            String elapsedTimeText = formatDuration(elapsedTime);
            timeLabel.setText("Time: " + elapsedTimeText);
        });
    }

    private void renderFrame(List<String> currentFrame) {
        vbox.getChildren().clear();
        vbox.getChildren().addAll(frameLabel, timeLabel);

        for (String line : currentFrame) {
            Label label = createLabel(0, 0);
            label.setText(line);
            vbox.getChildren().add(label);
        }
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        long milliseconds = duration.toMillis() % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }
}