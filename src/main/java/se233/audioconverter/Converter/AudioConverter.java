package se233.audioconverter.Converter;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import java.io.File;

public class AudioConverter {
    String[] codecNames = {"libmp3lame", "pcm_s16le", "aac", "flac"};
    String[] formatNames = {"mp3", "wav", "ipod", "flac"};
    int[] channels = {1, 2};
    FileNameManager fileNameManager = new FileNameManager();

    public void convert(File sourceAudio, int formatIndex, int bitrateValue, int sampleRateValue, int channelIndex) {
        String targetFormat = formatNames[formatIndex];

        if (!areParametersValid(targetFormat, bitrateValue, sampleRateValue)) {
            throw new IllegalArgumentException("Invalid settings for " + targetFormat +
                    " - Bitrate: " + bitrateValue + " bps, Sample Rate: " + sampleRateValue + " Hz."
            );
        }

        String sourceCodec = codecNames[formatIndex];
        File targetAudio = fileNameManager.chname(sourceAudio.getPath(), formatIndex);
        System.out.println("Starting to convert file: " + sourceAudio.getName());

        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec(sourceCodec);
            audio.setChannels(this.channels[channelIndex]);
            audio.setSamplingRate(sampleRateValue);


            if (!targetFormat.equals("wav") && !targetFormat.equals("flac")) {
                audio.setBitRate(bitrateValue);
            }

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat(targetFormat);
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(sourceAudio), targetAudio, attrs);

            System.out.println("Successfully converted to " + targetFormat + " -> " + targetAudio.getName());

        } catch (Exception ex) {
            System.err.println("Conversion failed!");
            ex.printStackTrace();
        }
    }

    private boolean areParametersValid(String formatName, int bitrate, int sampleRate) {
        if (formatName == null) {
            return false;
        }

        switch (formatName) {
            case "mp3":
                boolean isValidMp3SampleRate = (sampleRate == 32000 || sampleRate == 44100 || sampleRate == 48000);
                boolean isValidMp3Bitrate = (bitrate >= 64000 && bitrate <= 320000);
                System.out.println("MP3 Validation: " + isValidMp3SampleRate + " " + isValidMp3Bitrate);
                return isValidMp3SampleRate && isValidMp3Bitrate;

            case "wav":
                boolean isValidWavSampleRate = (sampleRate == 44100 || sampleRate == 48000 || sampleRate == 96000);
                return isValidWavSampleRate;

            case "ipod":
                boolean isValidAacSampleRate = (sampleRate == 44100 || sampleRate == 48000);
                boolean isValidAacBitrate = (bitrate >= 96000 && bitrate <= 256000);
                return isValidAacSampleRate && isValidAacBitrate;

            case "flac":
                boolean isValidFlacSampleRate = (sampleRate == 44100 || sampleRate == 48000 || sampleRate == 96000 || sampleRate == 192000);
                return isValidFlacSampleRate;

            default:
                return false;
        }
    }




}