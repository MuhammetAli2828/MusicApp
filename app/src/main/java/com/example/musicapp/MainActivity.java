package com.example.musicapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listview;
    String[] items;
    ArrayList<String> songUris;   // content:// URI listesi
    ArrayList<String> songTitles; // başlık listesi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listview = findViewById(R.id.listViewSong);
        runtimePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (Environment.isExternalStorageManager() &&
                    (songUris == null || songUris.isEmpty())) {
                displaySongs();
            }
        }
    }

    public void runtimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            Dexter.withContext(this)
                    .withPermission(Manifest.permission.READ_MEDIA_AUDIO)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse r) {
                            displaySongs();
                        }
                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse r) {
                            if (r.isPermanentlyDenied()) showSettingsDialog();
                            else Toast.makeText(MainActivity.this,
                                    "Müzik erişimi için izin gerekli.", Toast.LENGTH_LONG).show();
                        }
                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest req, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Depolama İzni Gerekli")
                        .setMessage("Müzik dosyalarına erişmek için depolama iznine ihtiyaç duyar.")
                        .setPositiveButton("Ayarlar", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton("İptal", (d, w) -> d.dismiss())
                        .show();
            } else {
                displaySongs();
            }
        } else {
            // Android 10 ve altı
            Dexter.withContext(this)
                    .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse r) {
                            displaySongs();
                        }
                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse r) {
                            if (r.isPermanentlyDenied()) showSettingsDialog();
                            else Toast.makeText(MainActivity.this,
                                    "Müzik erişimi için izin gerekli.", Toast.LENGTH_LONG).show();
                        }
                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest req, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("İzin Gerekli")
                .setMessage("Ayarlardan müzik iznini etkinleştirin.")
                .setPositiveButton("Ayarlar", (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                })
                .setNegativeButton("İptal", (d, w) -> d.dismiss())
                .show();
    }

    void displaySongs() {
        songUris = new ArrayList<>();
        songTitles = new ArrayList<>();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        // Harici depolama
        queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, sortOrder);
        // Dahili depolama (bazı cihazlarda)
        queryMediaStore(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, sortOrder);

        if (!songUris.isEmpty()) {
            items = songTitles.toArray(new String[0]);
            customAdapter adapter = new customAdapter();
            listview.setAdapter(adapter);
            listview.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                intent.putStringArrayListExtra("songUris", songUris);
                intent.putStringArrayListExtra("songTitles", songTitles);
                intent.putExtra("pos", position);
                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "Cihazda müzik bulunamadı. Lütfen bir müzik dosyası ekleyin.", Toast.LENGTH_LONG).show();
            Log.w("MainActivity", "MediaStore'da müzik yok.");
        }
    }

    private void queryMediaStore(Uri contentUri, String[] projection, String sortOrder) {
        // 30 saniyeden kısa sistem sesleri, bildirim sesleri vb. hariç tut
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                + MediaStore.Audio.Media.DURATION + " >= 30000";
        try (Cursor cursor = getContentResolver().query(
                contentUri, projection, selection, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                do {
                    long songId = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    Uri songUri = ContentUris.withAppendedId(contentUri, songId);

                    if (title == null || title.isEmpty()) title = "Bilinmeyen Şarkı";
                    if (artist != null && !artist.isEmpty() && !artist.equals("<unknown>"))
                        title = title + " - " + artist;

                    songUris.add(songUri.toString());
                    songTitles.add(title);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "MediaStore sorgusu hatası: " + contentUri, e);
        }
    }

    class customAdapter extends BaseAdapter {
        @Override
        public int getCount() { return items != null ? items.length : 0; }
        @Override
        public Object getItem(int position) { return items != null ? items[position] : null; }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View myView = convertView;
            if (myView == null)
                myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textsong = myView.findViewById(R.id.txtsongname);
            textsong.setSelected(true);
            textsong.setText(items[position]);
            return myView;
        }
    }
}
