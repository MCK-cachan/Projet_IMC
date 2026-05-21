package com.tonpackage.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MusicDao {

    // --- PLAYLISTS ---
    @Query("SELECT * FROM Playlist")
    List<Playlist> getAllPlaylists();

    @Query("DELETE FROM Playlist WHERE IdPlaylist = :playlistId")
    void deletePlaylist(int playlistId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylist(Playlist playlist);

    // --- MUSIQUES ---
    @Query("SELECT * FROM Musique")
    List<Musique> getAllMusics();

    @Query("SELECT * FROM Musique WHERE IdMusic = :musicId LIMIT 1")
    Musique getMusicById(int musicId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMusic(Musique musique);

    @Query("DELETE FROM Musique WHERE IdMusic = :musicId")
    void deleteMusic(int musicId);

    @Query("SELECT * FROM Musique WHERE IdPlaylist = :playlistId")
    List<Musique> getMusicsByPlaylist(int playlistId);

    @Query("SELECT * FROM Musique WHERE IsPodcast = 0")
    List<Musique> getAllMusicOnly();

    @Query("SELECT * FROM Musique WHERE IsPodcast = 1")
    List<Musique> getAllPodcasts();

    // --- ARTISTES ---

    @Query("SELECT * FROM Artiste")
    List<Artiste> getAllArtistes();

    @Query("SELECT * FROM Artiste WHERE IsPodcast = 0")
    List<Artiste> getMusicians();

    @Query("SELECT * FROM Artiste WHERE IsPodcast = 1")
    List<Artiste> getPodcasters();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertArtiste(Artiste artiste);

    @Query("SELECT * FROM Artiste WHERE Pseudo = :name LIMIT 1")
    Artiste getArtisteByName(String name);

    @Query("SELECT * FROM Musique WHERE ArtisteName = :artisteName")
    List<Musique> getMusicsByArtiste(String artisteName);

    @Query("SELECT * FROM Musique WHERE IdArtiste = :artisteId AND IsPodcast = 0")
    List<Musique> getMusicsByArtiste(int artisteId);

    @Query("SELECT COUNT(*) > 0 FROM Musique WHERE MusicTitle = :title AND IdPlaylist = :playlistId")
    boolean checkIfMusicInPlaylist(String title, int playlistId);
}
