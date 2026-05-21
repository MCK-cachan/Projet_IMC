package com.tonpackage.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
@Entity(
        tableName = "Musique",
        foreignKeys = {
                @ForeignKey(entity = Playlist.class, parentColumns = "IdPlaylist", childColumns = "IdPlaylist"),
                @ForeignKey(entity = Artiste.class,  parentColumns = "IdArtiste",  childColumns = "IdArtiste")
        },
        indices = {@Index("IdPlaylist"), @Index("IdArtiste")}
)
public class Musique {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "IdMusic")
    public Integer idMusic;

    @ColumnInfo(name = "IdPlaylist")
    public Integer idPlaylist;

    @ColumnInfo(name = "IdArtiste")
    public Integer idArtiste;       // minuscule, cohérent avec le reste

    @ColumnInfo(name = "ArtisteName")
    public String artisteName;

    @ColumnInfo(name = "MusicTitle")
    public String musicTitle;

    @ColumnInfo(name = "ImagePath")
    public String imagePath;

    @ColumnInfo(name = "MusicPath")
    public String audioPath;

    @ColumnInfo(name = "IsPodcast")
    public boolean isPodcast;
}