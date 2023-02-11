package com.example.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Declarar ArrayList de caminhos de faixas
    ArrayList<String> paths;
    // Declarar ViewFlipper que alterna entre os dois layouts
    ViewFlipper viewFlipper;
    // Declarar TextView do texto da mensagem caso não forem encontradas faixas
    TextView txtEmptyMessage;
    // Declarar ListView das faixas
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Instalar a SplashScreen
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Definir ViewFlipper
        viewFlipper = findViewById(R.id.viewFlipper);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);

        runtimePermission();
    }

    // Pedir permissão de acesso aos ficheiros do telemóvel
    private void runtimePermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        displayTracks();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        viewFlipper.setDisplayedChild(0);

                        txtEmptyMessage.setText(getResources().getText(R.string.perm_files_denied));
                        txtEmptyMessage.setOnClickListener(view -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    // Devolve todos os caminhos para ficheiros de áudio que encontrar num dado diretório.
    //      Nota que este método vai a todos os subníveis do diretório, o que significa
    //      que vai procurar faixas até mesmo nas sub-pastas do diretório.
    private ArrayList<String> findTracks(File file) {
        // Definir ArrayList de caminhos a retornar
        ArrayList<String> arrayList = new ArrayList<>();

        // Definir Array de ficheiros/diretórios no diretório do ficheiro dado
        File[] files = file.listFiles();

        // Se forem encontrados alguns ficheiros/diretórios
        if (files != null) {
            // Para cada um
            for (File f : files) {
                // Se for um diretório e se não estiver escondido
                if (f.isDirectory() && !f.isHidden()) {
                    // Usa recursão sob este método para adicionar todos os caminhos de áudios nesse diretório
                    arrayList.addAll(findTracks(f));
                // Senão, se for um ficheiro de áudio
                } else if (f.getName().endsWith(".mp3") || f.getName().endsWith(".wav")) {
                    // Adiciona o caminho à lista de caminhos
                    arrayList.add(f.getPath());
                }
            }
        }
        // Retorna a lista de caminhos
        return arrayList;
    }

    // Exibir faixas na ListView
    private void displayTracks() {
        // Detetar todos os caminhos para ficheiros de áudio
        if (paths == null) paths = findTracks(Environment.getExternalStorageDirectory());

        // Se nenhum ficheiro encontrado
        if (paths.size() == 0) {
            // Muda para o layout para conteúdo vazio
            viewFlipper.setDisplayedChild(0);
            // Define o texto da mensagem
            txtEmptyMessage.setText(getResources().getText(R.string.no_tracks));
            return;
        }

        // Muda para o layout para conteúdo preenchido
        viewFlipper.setDisplayedChild(1);

        // Definir a ListView das faixas
        listView = findViewById(R.id.lvTracks);

        // Definir dados da ListView
        customAdapter customAdapter = new customAdapter();
        listView.setAdapter(customAdapter);

        // Listener - Ao clicar num item da ListView
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            // Criar um novo Bundle para os extras
            Bundle extras = new Bundle();
            // Incluir lista de caminhos
            extras.putParcelableArrayList(PlayerActivity.EXTRA_TRACKS, (ArrayList) paths);
            // Incluir índice da faixa escolhida
            extras.putInt(PlayerActivity.EXTRA_POSITION, i);

            // Criar um Intent
            Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
            // Incluir o Bundle de extras no Intent
            intent.putExtra("bundle", extras);

            // Começar a PlayerActivity com os dados passados pelo Intent
            startActivity(intent);
            overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top);
        });
    }

    // Adapter que constrói cada list item (cada faixa na lista de faixas)
    private class customAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return paths.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Buscar a faixa em questão
            File file = new File(paths.get(i));

            // Criar novo list item
            View listItem = getLayoutInflater().inflate(R.layout.list_item, null);

            // Buscar Views
            ImageView imgCover = listItem.findViewById(R.id.imgCover);
            TextView txtTitle = listItem.findViewById(R.id.txtTitle);
            TextView txtArtist = listItem.findViewById(R.id.txtArtist);

            // Definir imagem da ImageView para a imagem da faixa
            imgCover.setImageDrawable(Track.getImage(getApplicationContext(), file));
            // Definir texto da TextView para o título da faixa
            txtTitle.setText(Track.getTitle(file));
            // Definir texto da TextView para o título do artista
            txtArtist.setText(Track.getArtist(file));

            return listItem;
        }
    }
}