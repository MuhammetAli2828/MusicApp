package com.example.musicapp;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    Button btnplay, btnnext, btnback, btnforwardnext, btnforwardback;
    TextView txtsname, txtsstart, txtsstop;
    SeekBar seekmusic;
    ImageView imageView;
    ImageButton btnBackToList;

    MediaPlayer mediaPlayer;
    ArrayList<String> songUris;
    ArrayList<String> songTitles;
    int currentPosition;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        btnplay        = findViewById(R.id.playbtn);
        btnnext        = findViewById(R.id.btnnext);
        btnback        = findViewById(R.id.btnback);
        btnforwardnext = findViewById(R.id.btnforwardnext);
        btnforwardback = findViewById(R.id.btnforwardback);
        txtsname       = findViewById(R.id.txtsn);
        txtsstart      = findViewById(R.id.txtsstart);
        txtsstop       = findViewById(R.id.txtsstop);
        seekmusic      = findViewById(R.id.seekbar);
        imageView      = findViewById(R.id.Imageview);
        btnBackToList  = findViewById(R.id.btnBackToList);

        songUris   = getIntent().getStringArrayListExtra("songUris");
        songTitles = getIntent().getStringArrayListExtra("songTitles");
        currentPosition = getIntent().getIntExtra("pos", 0);

        if (songUris == null || songUris.isEmpty()) {
            Toast.makeText(this, "Şarkı verisi alınamadı.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mediaPlayer = new MediaPlayer();
        playSong(currentPosition);

        btnplay.setOnClickListener(v -> togglePlayPause());
        btnnext.setOnClickListener(v -> playNextSong());
        btnback.setOnClickListener(v -> playPreviousSong());
        btnforwardnext.setOnClickListener(v -> seekForward());
        btnforwardback.setOnClickListener(v -> seekBackward());
        btnBackToList.setOnClickListener(v -> finish());

        seekmusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void playSong(int position) {
        try {
            mediaPlayer.reset();

            Uri uri = Uri.parse(songUris.get(position));
            mediaPlayer.setDataSource(getApplicationContext(), uri);

            String title = songTitles != null ? songTitles.get(position) : "Şarkı";
            txtsname.setText(title);
            txtsstart.setText("00:00");
            txtsstop.setText("--:--");
            btnplay.setBackgroundResource(R.drawable.ikonpause);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                txtsstop.setText(formatTime(mp.getDuration()));
                seekmusic.setMax(mp.getDuration());
                handler.removeCallbacks(updateSeekBar);
                handler.post(updateSeekBar);
            });

            mediaPlayer.setOnCompletionListener(mp -> playNextSong());

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("PlayerActivity", "MediaPlayer hatası: what=" + what + " extra=" + extra);
                Toast.makeText(this, "Bu şarkı çalınamıyor, sonrakine geçiliyor.", Toast.LENGTH_SHORT).show();
                playNextSong();
                return true;
            });

            mediaPlayer.prepareAsync(); // Ana thread'i bloklamaz

        } catch (Exception e) {
            Log.e("PlayerActivity", "Şarkı ayarlanamadı: " + songUris.get(position), e);
            Toast.makeText(this, "Şarkı açılamıyor.", Toast.LENGTH_SHORT).show();
            playNextSong();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnplay.setBackgroundResource(R.drawable.iconplay);
        } else {
            mediaPlayer.start();
            btnplay.setBackgroundResource(R.drawable.ikonpause);
        }
    }

    private void playNextSong() {
        currentPosition = (currentPosition + 1) % songUris.size();
        playSong(currentPosition);
    }

    private void playPreviousSong() {
        currentPosition = (currentPosition - 1 < 0) ? songUris.size() - 1 : currentPosition - 1;
        playSong(currentPosition);
    }

    private void seekForward() {
        if (mediaPlayer == null) return;
        mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + 10000, mediaPlayer.getDuration()));
    }

    private void seekBackward() {
        if (mediaPlayer == null) return;
        mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - 10000, 0));
    }

    private String formatTime(int ms) {
        int minutes = ms / 1000 / 60;
        int seconds = (ms / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                seekmusic.setProgress(mediaPlayer.getCurrentPosition());
                txtsstart.setText(formatTime(mediaPlayer.getCurrentPosition()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
