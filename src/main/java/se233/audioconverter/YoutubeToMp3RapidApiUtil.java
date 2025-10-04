package se233.audioconverter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.URL;

public class YoutubeToMp3RapidApiUtil {

    public static void main(String[] args) {
        String userUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        System.out.println("Processing URL: " + userUrl);
        String videoId = extractVideoId(userUrl);
        if (videoId == null) {
            System.err.println("Error: Could not find a valid video ID in the URL.");
            return;
        }
        System.out.println("Found Video ID: " + videoId);

        String mp3Url = fetchMp3LinkFromApi(videoId);
        if (mp3Url == null) {
            System.err.println("Error: Failed to get the MP3 link from the API. Check your API key or network.");
            return;
        }
        System.out.println("Got MP3 link. Asking user where to save...");

        String savePath = chooseSaveLocation(videoId);
        if (savePath == null) {
            System.out.println("Operation canceled by user.");
            return;
        }
        System.out.println("Saving file to: " + savePath);

        System.out.println("Starting download...");
        boolean success = downloadMp3(mp3Url, savePath);

        if (success) {
            System.out.println("✅ Download complete!");
        } else {
            System.err.println("❌ Download failed.");
        }
    }

    public static String chooseSaveLocation(String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a location to save the MP3 file");
        fileChooser.setSelectedFile(new File(defaultFileName + ".mp3"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Audio", "mp3");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String path = fileToSave.getAbsolutePath();

            // Ensure the file has the .mp3 extension
            if (!path.toLowerCase().endsWith(".mp3")) {
                path += ".mp3";
            }
            return path;
        } else {
            return null;
        }
    }

    public static String extractVideoId(String url) {
        int vIndex = url.indexOf("v=");
        if (vIndex != -1) {
            String idPart = url.substring(vIndex + 2);
            int ampIndex = idPart.indexOf("&");
            if (ampIndex != -1)
                return idPart.substring(0, ampIndex);
            else
                return idPart;
        }
        int youtuIndex = url.indexOf("youtu.be/");
        if (youtuIndex != -1) {
            String idPart = url.substring(youtuIndex + 9);
            int ampIndex = idPart.indexOf("?");
            if (ampIndex != -1)
                return idPart.substring(0, ampIndex);
            else
                return idPart;
        }
        return null;
    }

    public static String fetchMp3LinkFromApi(String videoId) {
        try {
            String apiUrl = "https://youtube-mp36.p.rapidapi.com/dl?id=" + videoId;
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-rapidapi-key", "44145a5eadmsh2d73a0120be904dp1a471cjsn43769ce8f00b");
            conn.setRequestProperty("x-rapidapi-host", "youtube-mp36.p.rapidapi.com");

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            String result = json.toString();

            if (result.contains("\"link\":\"")) {
                return result.split("\"link\":\"")[1].split("\"")[0].replace("\\/", "/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean downloadMp3(String mp3Url, String saveAs) {
        try (InputStream in = new URL(mp3Url).openStream();
             FileOutputStream fos = new FileOutputStream(saveAs)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) fos.write(buffer, 0, length);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}