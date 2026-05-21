package com.tonpackage.database;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Playlist")
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "IdPlaylist")
    public Integer IdPlaylist;

    @ColumnInfo(name = "Name")
    public String Name;

    @ColumnInfo(name = "ImagePath")
    public String imagePath;

    @ColumnInfo(name = "SelectedColor")
    public Integer selectedColor; // Nouvelle colonne pour stocker la couleur
}