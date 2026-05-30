package com.chatapp.client.media;

import javax.sound.sampled.*;

public class AudioPlayer {
    public static void play(byte[] audioData) {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            Thread playThread = new Thread(() -> {
                line.write(audioData, 0, audioData.length);
                line.drain();
                line.stop();
                line.close();
            });
            playThread.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
