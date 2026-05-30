package com.chatapp.client.media;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioRecorder {
    private TargetDataLine line;
    private AudioFormat format;
    private ByteArrayOutputStream out;
    private boolean isRecording;

    public AudioRecorder() {
        format = new AudioFormat(16000, 16, 1, true, true);
    }

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        isRecording = true;
        out = new ByteArrayOutputStream();

        Thread recordingThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isRecording) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
        });
        recordingThread.start();
    }

    public byte[] stopRecording() {
        isRecording = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        return out.toByteArray();
    }
}
