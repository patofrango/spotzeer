package com.example.musicplayer;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;

import java.io.File;

public class Track {
    private static final MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    private Track() {}

    public static String getTitle(File f) {
        mmr.setDataSource(String.valueOf(f));
        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title == null)
            title = f.getName().replace(".mp3", "").replace(".wav", "");
        return title;
    }

    public static String getArtist(File f) {
        mmr.setDataSource(String.valueOf(f));
        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist == null)
            artist = "Unknown artist";
        return artist;
    }

    public static Drawable getImage(Context context, File f) {
        mmr.setDataSource(String.valueOf(f));
        byte[] imageBytes = mmr.getEmbeddedPicture();
        return (imageBytes != null) ?
                new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length))
                :
                context.getDrawable(R.drawable.ic_note);
    }
}
