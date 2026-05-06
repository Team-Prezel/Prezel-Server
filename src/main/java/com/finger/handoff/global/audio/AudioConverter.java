package com.finger.handoff.global.audio;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;

@Component
public class AudioConverter {

    public File convertToWav(MultipartFile audioFile) {
        File tempOriginalFile = null;
        File convertedWavFile = null;

        try {
            tempOriginalFile = File.createTempFile("original_audio_", ".tmp");
            audioFile.transferTo(tempOriginalFile);

            convertedWavFile = File.createTempFile("converted_audio_", ".wav");

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setBitRate(256000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(tempOriginalFile), convertedWavFile, attrs);

            return convertedWavFile;

        } catch (Exception e) {
            if (convertedWavFile != null && convertedWavFile.exists()) {
                convertedWavFile.delete();
            }
            throw new RuntimeException("오디오 포맷 변환 중 오류가 발생했습니다.");
        } finally {
            if (tempOriginalFile != null && tempOriginalFile.exists()) {
                tempOriginalFile.delete();
            }
        }
    }
}
