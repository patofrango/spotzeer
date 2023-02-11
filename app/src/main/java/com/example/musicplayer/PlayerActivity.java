package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    // Definir chaves dos Extras
    public static final String EXTRA_TRACKS = "TRACKS";
    public static final String EXTRA_POSITION = "POSITION";

    // Declarar Views
    Button btnPrev, btnPlay, btnNext, btnShuffle, btnLoop, btnList;
    TextView txtTitle, txtArtist, txtCurrentTime, txtTotalTime;
    ImageView imgCover;
    SeekBar trackBar;

    // Declarar MediaPlayer
    static MediaPlayer mediaPlayer;
    // Declarar lista de faixas
    ArrayList<String> paths;
    // Declarar número da faixa atual (posição na lista)
    int currentPosition;
    // Declarar estado do loop
    boolean loop = false;
    boolean shuffle = false;
    // Declarar Handlers
    Handler currentTimeHandler, trackBarHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Definir Views
        btnPrev = findViewById(R.id.btnPrev);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnLoop = findViewById(R.id.btnLoop);
        btnList = findViewById(R.id.btnList);
        txtTitle = findViewById(R.id.txtTitle);
        txtArtist = findViewById(R.id.txtArtist);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        trackBar = findViewById(R.id.trackBar);
        imgCover = findViewById(R.id.imgCover);

        // Definir a TextView do título como selecionada (só assim o atributo singleLine funciona)
        txtTitle.setSelected(true);

        // Buscar o Intent que iniciou esta Activity
        Bundle b = getIntent().getBundleExtra("bundle");
        getIntent().setExtrasClassLoader(Track.class.getClassLoader());

        // Lista de músicas
        paths = (ArrayList) b.getParcelableArrayList(EXTRA_TRACKS);
        // Posição da faixa atual no array de faixas
        currentPosition = b.getInt(EXTRA_POSITION, 0);

        updatePlayer();
        setListeners();
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    // Criar o MediaPlayer e atualizar as Views com os dados da faixa atual
    private void updatePlayer() {
        // Buscar faixa atual
        File file = new File(paths.get(currentPosition));

        startMediaPlayer(file);

        // Definir texto da TextView para o título da faixa
        txtTitle.setText(Track.getTitle(file));
        // Definir texto da TextView para o artista da faixa
        txtArtist.setText(Track.getArtist(file));
        // Definir texto da TextView para o tempo atual do MediaPlayer
        txtCurrentTime.setText(formatTime(0));
        // Definir texto da TextView para o tempo total do MediaPlayer
        txtTotalTime.setText(formatTime(mediaPlayer.getDuration()));
        // Definir imagem da ImageView para o Bitmap da imagem da faixa
        imgCover.setImageDrawable(Track.getImage(getApplicationContext(), file));
        // Definir imagem de fundo do Button para o ícone de pausa
        btnPlay.setBackgroundResource(R.drawable.ic_pause);

        startTrackBarHandler();
        startCurrentTimeHandler();
    }

    // Cria um novo MediaPlayer com os dados da faixa e começa a tocá-lo
    private void startMediaPlayer(File f) {
        // Se já estiver definido um mediaPlayer, para-o
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        // Gerar URI do ficheiro de áudio da faixa
        Uri uri = Uri.parse(f.getPath());
        // Criar um novo MediaPlayer
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);

        // Listener - Ao terminar uma faixa, saltar para a próxima
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Se o loop estiver desligado
                if (!loop) nextTrack();
                else seekToStart();
            }
        });

        // Começar a tocar
        mediaPlayer.start();
    }

    // Handler para atualizar a View do tempo atual a cada X milisegundos
    private void startCurrentTimeHandler() {
        currentTimeHandler = new Handler();
        final int delayMillis = 1000; // Atualizar a cada X milisegundos

        currentTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Gerar o tempo atual em string formatada
                String formattedCurrentTime = formatTime(mediaPlayer.getCurrentPosition());
                // Definir o texto da view para a string formatada
                txtCurrentTime.setText(formattedCurrentTime);
                // Voltar a correr o método ao fim do delay
                currentTimeHandler.postDelayed(this, delayMillis);
            }
        }, delayMillis);
    }

    // Handler para atualizar a barra da faixa a cada X milisegundos
    private void startTrackBarHandler() {
        trackBarHandler = new Handler();
        final int delayMillis = 500; // Atualizar a cada X milisegundos

        // Resetar a SeekBar para 0
        trackBar.setProgress(0);
        // Definir valor máximo da SeekBar para o tempo total do MediaPlayer em milisegundos
        trackBar.setMax(mediaPlayer.getDuration());

        trackBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                trackBar.setProgress(mediaPlayer.getCurrentPosition());
                trackBarHandler.postDelayed(this, delayMillis);
            }
        }, delayMillis);
    }

    // Mudar para a próxima faixa
    private void nextTrack() {
        // Se o shuffle estiver desligado, a nova posição é igual ao resto da divisão da próxima pelo número total de faixas
        // Senão, retorna posição aleatória
        currentPosition = (!shuffle) ? ((currentPosition + 1) % paths.size()) : randomPosition();
        updatePlayer();
    }

    // Retornar posição aleatória na lista de faixas (excluindo a posição atual)
    private int randomPosition() {
        ArrayList<Integer> positions = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++)
            positions.add(i);
        positions.remove(currentPosition);
        return positions.get((int) (Math.random() * positions.size()));
    }

    // Mudar para a faixa anterior
    private void previousTrack() {
        // Se for a primeira faixa, vai para a última faixa. Caso contrário, recua para a anterior
        currentPosition = (currentPosition - 1 < 0) ? (paths.size() - 1) : (currentPosition - 1);
        updatePlayer();
    }

    private void seekToStart() {
        mediaPlayer.seekTo(0);
        txtCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
        if (!mediaPlayer.isPlaying()) mediaPlayer.start();
        startTrackBarHandler();
    }

    private void seekTo(int millis) {
        mediaPlayer.seekTo(millis);
        txtCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
    }

    private void setNextTrackMode(int mode) {
        switch (mode) {
            // Desativar todos
            case 0:
                shuffle = loop = false;
                btnShuffle.getBackground().clearColorFilter();
                btnLoop.getBackground().clearColorFilter();
                break;
            // Modo shuffle
            case 1:
                shuffle = true;
                loop = false;
                btnShuffle.getBackground().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.selected, null), PorterDuff.Mode.SRC_IN);
                btnLoop.getBackground().clearColorFilter();
                break;
            // Modo loop
            case 2:
                shuffle = false;
                loop = true;
                btnShuffle.getBackground().clearColorFilter();
                btnLoop.getBackground().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.selected, null), PorterDuff.Mode.SRC_IN);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    // Definir listeners
    @SuppressLint("ResourceType")
    private void setListeners() {
        // Botão Play/Pause
        btnPlay.setOnClickListener(view -> {
            if (mediaPlayer.isPlaying()) {
                btnPlay.setBackgroundResource(R.drawable.ic_play);
                mediaPlayer.pause();
            } else {
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                mediaPlayer.start();
            }
        });

        // Botão Next
        btnNext.setOnClickListener(view -> nextTrack());

        // Botão Previous
        btnPrev.setOnClickListener(view -> {
            if (mediaPlayer.getCurrentPosition() <= 1000) {
                previousTrack();
            } else {
                seekToStart();
            }
        });

        // Botão Shuffle
        btnShuffle.setOnClickListener(view -> setNextTrackMode(shuffle ? 0 : 1));

        // Botão Loop
        btnLoop.setOnClickListener(view -> setNextTrackMode(loop ? 0 : 2));

        // Botão Lista das Faixas
        btnList.setOnClickListener(view -> startActivity(new Intent(PlayerActivity.this, MainActivity.class)));

        // Ao mudar a barra de progresso
        trackBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekTo(seekBar.getProgress());
            }
        });
    }
}