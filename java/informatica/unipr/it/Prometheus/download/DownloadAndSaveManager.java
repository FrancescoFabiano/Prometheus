package informatica.unipr.it.Prometheus.download;


import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadAndSaveManager {
    private static Object lock = new Object();  // statica altrmenti quando si crea più oggetti della classe crea semafori diversi
    Context context;
    Thread saveImageInternalFromReferenceThread;

    public DownloadAndSaveManager(Context contextTmp) {
        context = contextTmp;
    }


    public void downloadUriAndSaveImageInternal(String filename, Uri uri) {
        Long reference = downloadUri(uri, filename);
        if (reference != 90000) {    // 0 è convenzione nostra per dire che l'uri non conteneva nessuna immagine
            saveImageInternalFromReference(reference, filename);
        }
    }

    public void downloadURLAndSaveImageInternal(String filename, String url) {
        InputStream input = downloadURL(url);
        saveImageInternalFromInputStream(input, filename);
    }

    public void downloadAndSaveFacebookProfilePicture(String userID) {
        final String s = "https://graph.facebook.com/" + userID + "/picture?type=large";  // https://graph.facebook.com/1314182960/picture?type=large
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream input = downloadURL(s);

                saveImageInternalFromInputStream(input, "FacebookProfileImage");
            }
        });

        thread.start();

    }


    public Long downloadUri(Uri uri, String filename) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request dmRequest;

        if (uri != null) {
            dmRequest = new DownloadManager.Request(uri);
            dmRequest.setTitle(filename);
            dmRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);

            return downloadManager.enqueue(dmRequest);
        }
        return 90000L;
    }

    public InputStream downloadURL(String urlString) {    // chiudere input Stream quando usato
        InputStream input = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            input = (connection.getInputStream());
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return input;
    }

    public void saveImageInternalFromReference(final Long reference, final String filename) { // variabili locali dichiarate final perche passate al thread
        saveImageInternalFromReferenceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        lock.wait();        // monitor in stop
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
                Bitmap profileImageTmp = readImageInternal(reference.toString());
                saveImageInternal(profileImageTmp, filename);
            }
        });
        saveImageInternalFromReferenceThread.start();
    }

    public void saveImageInternalFromInputStream(InputStream input, String filename) {
        File file = new File(context.getFilesDir(), filename + ".jpg");
        Bitmap profileImageTmp = BitmapFactory.decodeStream(input);

        saveImageInternal(profileImageTmp, filename);
    }

    private String saveImageInternal(Bitmap bitmapImage, String filename) {
        String filePath = context.getFilesDir().getPath().toString() + "/" + filename + ".jpg";
        File file = new File(filePath);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            //System.out.println("File not found: " + e.getMessage());
        } catch (Exception e) {
            //System.out.println("Error accessing file: " + e.getMessage());
        }

        return file.getAbsolutePath();
    }

    public Bitmap readImageInternal(String filename) {
        String filePath = context.getFilesDir().getPath().toString() + "/" + filename + ".jpg";
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        return bitmap;
    }


    public void moveFromCacheToInternal(String cacheFilePath, String filenameInternal) {    // sposta dalla memoria cache alla memoria interna
        Bitmap bitmap = BitmapFactory.decodeFile(cacheFilePath);
        saveImageInternal(bitmap, filenameInternal);
        synchronized (lock) {
            lock.notify();  // sblocca il monitor
        }
    }

    public boolean deleteImageInternal(String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);
        return file.delete();
    }

    /*private void SaveImageExternal(Bitmap finalBitmap) {

        String root = context.Environment.ExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
