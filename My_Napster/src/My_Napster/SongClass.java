/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_Napster;

/**
 *
 * @author Edison Andres
 */
public class SongClass {
    String title;
    String artist;
    String album;
    String length;
    String size;
    String ip;
    String port;

    public SongClass(String title, String artist, String album, String length, String size, String ip, String port) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.length = length;
        this.size = size;
        this.ip = ip;
        this.port = port;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
    
}
