package com.tonpackage.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Artiste")
public class Artiste {

    @PrimaryKey
    @ColumnInfo(name = "IdArtiste")
    public Integer idArtiste;

    @ColumnInfo(name = "Pseudo")
    public String Pseudo;

    @ColumnInfo(name = "ImagePath")
    public String imagePath;

    @ColumnInfo(name = "IsPodcast")
    public boolean isPodcast;
}