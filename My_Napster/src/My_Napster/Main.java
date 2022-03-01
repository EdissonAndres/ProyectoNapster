/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_Napster;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.myid3.MyID3;
import javazoom.jl.player.Player;


/**
 *
 * @author Edison Andres
 */
public class Main {
    
    static MainFrame Frame  = new MainFrame();
    
    static final String HOST[] = {"127.0.0.1","127.0.0.1"};
    static final int PORT[] = {5000, 5001};
    static int posHostPort = 0;
    
    static final String HOSTC = "127.0.0.1";
    
    //MODIFICAR PORTC CADA QUE SE DESEE EJECUTAR UN NUEVO CLIENTE LOCAL
    static final int PORTC = 6000;
    
    static int totalRequest = 0;
    
    static ArrayList listSongs = new ArrayList();
    static ArrayList listMetaDataSongs = new ArrayList();
    static Gson json = new Gson();
    
    static DataOutputStream outDataServer;
    static DataInputStream inDataServer;
    static BufferedReader in;
    
    static File directory;
    
    static File directoryToDownloads;
    
    static boolean firstTime = true;
    static boolean firstDownload = true;
    
    static ArrayList listOfDownloads = new ArrayList();
    
    public static ArrayList dataBytesSong = new ArrayList();
    
    static Player player;
    
    
    private static String lenghtSong(File file) throws UnsupportedAudioFileException, IOException, BitstreamException {
        Header h = null;
        FileInputStream filet = null;
        filet = new FileInputStream(file);
        Bitstream bitstream = new Bitstream(filet);
        h = bitstream.readFrame();
        int size = h.calculate_framesize();
        long tn = 0;
        tn = file.length();
        float mili = (float) (h.total_ms((int) tn)/1000);
        int min = (int) (mili / 60);
        float minF = (float) (mili / 60);
        float deci = minF - min; 
        int seg = (int) (deci*60);
        Formatter obj = new Formatter();
        String segf = String.valueOf(obj.format("%02d", seg));
        String time = Integer.toString(min)+":"+(segf);
        return time;
    }
    
    public static ArrayList findAllMetaDataFilesInFolder(File folder) throws IOException, UnsupportedAudioFileException, BitstreamException {
        String path;
        SongClass s;
        MusicMetadata datos;
        String size;
        String title;
        String artist;
        String album;
        String length;
        
        for (File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                path = folder + "\\" + file.getName();
                File song = new File(path);
                datos = (MusicMetadata) new MyID3().read(song).getSimplified();
                size = Long.toString(song.length());
                title = file.getName();
                artist = datos.getArtist();
                if (artist == null){
                    artist = "Unknown";
                }
                album = datos.getAlbum();
                if (album == null){
                    album ="Unknown";
                }
                length = lenghtSong(song);
                s = new SongClass(title, artist, album, length, size, HOSTC, Integer.toString(PORTC));
                listMetaDataSongs.add(s);
            } else {
                findAllMetaDataFilesInFolder(file);
            }
        }
        return listMetaDataSongs;
    }
    
    public static void connectToServer() throws IOException{
        try{
            Socket socket = new Socket(HOST[posHostPort], PORT[posHostPort]);
            outDataServer = new DataOutputStream(socket.getOutputStream());  
            inDataServer = new DataInputStream(socket.getInputStream());
            InputStreamReader isr = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
            in = new BufferedReader(new BufferedReader(isr));
        } catch( IOException e ) {
            System.out.println( e.getMessage() );
            posHostPort++;
            if (posHostPort >= HOST.length-1){
                String[] options = {"Exit", "Retry"};
                int seleccion = JOptionPane.showOptionDialog(Frame, "Server not available!", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE , null, options, options[0]);
                if (seleccion == 1){
                    posHostPort = 0;
            }
            connectToServer();
            }
        }
    }
    
    public static void sendDataSongToServer() throws IOException, UnsupportedAudioFileException, BitstreamException{
        if (firstTime){
            String[] options = {"Exit", "OK"};
            int seleccion = JOptionPane.showOptionDialog(Frame, "Hello, Welcome to My Napster, please share the best music\nfolder with us, so that we can all find the song we want!", 
                    "Welcome", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE , null, options, options[0]);
            if (seleccion == 0){
                System.exit(0);
            }
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int respuesta = fc.showOpenDialog(Frame);
        if (respuesta == JFileChooser.APPROVE_OPTION) {
            firstTime = false;
            //Crear un objeto File con el archivo elegido
            //File archivoElegido = fc.getCurrentDirectory();
            //Mostrar el nombre del archvivo en un campo de texto
            directory = fc.getSelectedFile();
            try{
                listSongs = findAllMetaDataFilesInFolder(directory);
                String sendData = json.toJson(listSongs);

                System.out.println(sendData);

                outDataServer.writeUTF(sendData);
                outDataServer.flush();
            } catch(RuntimeException e){
                System.out.println(e);
                JOptionPane.showMessageDialog(Frame, "Dear user, the folder you selected contains files with\ninvalid formats for the system, please make sure that\nthe folder to select only contains files in .mp3 format.");
                sendDataSongToServer();
            }
                listMetaDataSongs.clear();     
        }
        else{
            if (firstTime){
                sendDataSongToServer();
            }
        }
    }
    
    public static void sendSearchToServer(String type, String data_search) throws IOException{
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        object.addProperty("data_search", data_search);
        String message = object.toString();
        
        //System.out.println(message);
        String msg = "@" + type + "@" + data_search;
        
        outDataServer.writeUTF(msg);
        outDataServer.flush();
        System.out.println(msg);
    }
    
    public static class threadToReceivedFromServer implements Runnable {
        final MainFrame frame;
        
        public threadToReceivedFromServer(MainFrame frame){
            this.frame = frame;
        }
        
        @Override
        public void run(){
            while (true){
                try {
                    String str;
                    while ((str = in.readLine()) != null) {
                        System.out.println(str);
                        if (str.charAt(0) == '['){
                            SongClass[] songsResults = json.fromJson(str,SongClass[].class);
                            SwingUtilities.invokeLater(() -> {
                                this.frame.showTable3(songsResults);
                            });
                        }
                        else{
                            if ("DATA_INVALID".equals(str)){
                                JOptionPane.showMessageDialog(Frame, "Dear user, some of the metadata of your shared songs contains symbols\nthat cause read incompatibilities, please make sure your songs\ndo not contain the following symbols (', ´, \", |, ^,)");
                            }
                            if ("RECEIVED".equals(str)){
                                JOptionPane.showMessageDialog(Frame, "Thanks for sharing your songs with us, enjoy My Napster.");
                            }
                        }
                    }
                } catch (IOException ex) {
                    try {
                        connectToServer();
                    } catch (IOException ex1) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
        }
    }
    
    public static class threadToConnectClient implements Runnable {
        final MainFrame frame;
        final String ip;
        final int port;
        final String nameSong;
        final long size;
        final int startReadBytes;
        final int totalBytes;
        final int pos;
        
        public threadToConnectClient(MainFrame frame, String ip, int port, String nameSong, String size, int startReadBytes, int totalBytes, int pos){
            this.frame = frame;
            this.ip = ip;
            this.port = port;
            this.nameSong = nameSong;
            this.size = Long.parseLong(size);
            this.startReadBytes = startReadBytes;
            this.totalBytes = totalBytes;
            this.pos = pos;
        }
        
        @Override
        public void run(){
            Socket socketClient = null;
            try {
                socketClient = new Socket(ip, port);
                DataOutputStream outDataClient = new DataOutputStream(socketClient.getOutputStream());
                BufferedInputStream inDataClient = new BufferedInputStream(socketClient.getInputStream());
                outDataClient.writeUTF("/@/" + nameSong + "/@/" + startReadBytes + "/@/" + totalBytes);
                
                int bytesRead;
                byte[] buffer = new byte[10240];
                float con = 1;
                float iter = (int)size / 10240;
                int percent;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                
                while ((bytesRead = inDataClient.read(buffer)) != -1){
                    outputStream.write(buffer, 0, bytesRead);
                    
                    //dataFile.write(buffer, 0, bytesRead);
                    //percent = (int) ((con/iter)*100);
                    //con++;
                    //this.frame.updateTable2((Integer.toString(percent))+"%",listOfDownloads.indexOf(nameSong));
                }
                
                byte[] dataSong = outputStream.toByteArray();
                System.out.println("size data song " + dataSong.length);
                dataBytesSong.set(pos-1, dataSong);
                
                System.out.println("READY!");
                //dataFile.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(Frame, "The connection with the client has been lost, do\nthe search for the song again to verify that the\nclient is still active and start the download again");
                this.frame.updateTable2("Error",listOfDownloads.indexOf(nameSong));
                try {
                    socketClient.close();
                } catch (IOException ex1) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            
        }
    }
    
    public static class threadToConnectClient2 implements Runnable {
        final MainFrame frame;
        final String ip;
        final int port;
        final String nameSong;
        final long size;
        
        public threadToConnectClient2(MainFrame frame, String ip, int port, String nameSong, String size){
            this.frame = frame;
            this.ip = ip;
            this.port = port;
            this.nameSong = nameSong;
            this.size = Long.parseLong(size);
        }
        
        @Override
        public void run(){
            Socket socketClient = null;
            try {
                socketClient = new Socket(ip, port);
                DataOutputStream outDataClient = new DataOutputStream(socketClient.getOutputStream());
                BufferedInputStream inDataClient = new BufferedInputStream(socketClient.getInputStream());
                outDataClient.writeUTF("/@/" + nameSong);
                
                int bytesRead;
                byte[] buffer = new byte[10240];
                //System.out.println(iter);
                //System.out.println(size);
                
                File file = new File(directoryToDownloads+"\\"+nameSong);
                BufferedOutputStream dataFile = new BufferedOutputStream (new FileOutputStream(file,false));
                
                while ((bytesRead = inDataClient.read(buffer)) != -1){
                    dataFile.write(buffer, 0, bytesRead);
                }
                this.frame.updateTable2("Complete!",listOfDownloads.indexOf(nameSong));
                System.out.println("READY!");
                dataFile.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(Frame, "The connection with the client has been lost, do\nthe search for the song again to verify that the\nclient is still active and start the download again");
                this.frame.updateTable2("Error",listOfDownloads.indexOf(nameSong));
                try {
                    socketClient.close();
                } catch (IOException ex1) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            
        }
    }
    
    public static void initializeDataArray(int size){
        int i;
        for (i=0;i<size;i++){
            dataBytesSong.add(i, "");
        }
    }    
    
    public static class buildSong implements Runnable {
        final MainFrame frame;
        final String nameSong;
    
        public buildSong(MainFrame frame, String nameSong) throws FileNotFoundException, IOException{
            this.frame = frame;
            this.nameSong = nameSong;
        }
            
        @Override
        public void run(){
            BufferedOutputStream dataFile = null;
            try {
                boolean ready = false;
                while (ready == false){
                    ready= true;
                    for(Object b : dataBytesSong){
                        if (b==""){
                            ready = false;
                        }
                    }
                }   
                File file = new File(directoryToDownloads+"\\"+nameSong);
                dataFile = new BufferedOutputStream (new FileOutputStream(file,false));
                System.out.println(dataBytesSong);
                for(Object b :dataBytesSong){
                    dataFile.write((byte[]) b);
                }   
                dataFile.close();
                frame.updateTable2("Complete!", listOfDownloads.indexOf(nameSong));
                new Thread(new playSong(this.frame, nameSong)).start();
                clearDataArray();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    dataFile.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    public static class buildSongAlbum implements Runnable {
        final MainFrame frame;
        final ArrayList<SongClass> clientsToDownload;
        
        public buildSongAlbum(MainFrame frame, ArrayList<SongClass> clientsToDownload) throws IOException{
            this.frame = frame;
            this.clientsToDownload = clientsToDownload;
        }
        
        @Override
        public void run(){
            boolean ready = false;
            while (ready == false){
                ready= true;
                for(Object b : dataBytesSong){
                    if (b==""){
                        ready = false;
                    }
                }
            }
                
            int cont = 0;
            System.out.println(dataBytesSong);
            for(SongClass b :clientsToDownload){
                try{
                    File file = new File(directoryToDownloads+"\\"+b.getTitle());
                    BufferedOutputStream dataFile = new BufferedOutputStream (new FileOutputStream(file,false));
                    dataFile.write((byte[]) dataBytesSong.get(cont));
                    dataFile.close();
                    System.out.println("pos list "+listOfDownloads.indexOf(b.getTitle()));
                    frame.updateTable2("Complete!", listOfDownloads.indexOf(b.getTitle()));
                    cont++;
                }catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            int poslastSong = clientsToDownload.size()-1;
            SongClass lastSong = clientsToDownload.get(poslastSong);
            new Thread(new playSong(this.frame, lastSong.getTitle())).start();
            clearDataArray();
        }
    }
    
    public static class addToTable implements Runnable {
        final MainFrame frame;
        final SongClass Song;
    
        public addToTable(MainFrame frame, SongClass Song){
            this.frame = frame;
            this.Song =Song;
        }
        
        @Override
        public void run(){
            if (listOfDownloads.indexOf(Song.getTitle()) == -1){
                listOfDownloads.add(Song.getTitle());
                System.out.println("songs: "+listOfDownloads);
            }
        }
    }
    
    public static void clearDataArray(){
        dataBytesSong.clear();
    }
    
    public static class playSong implements Runnable {
        final MainFrame frame;
        final String nSong;
        
        public playSong(MainFrame frame, String nsong){
            this.nSong = nsong;
            this.frame = frame;
        }
        
        @Override
        public void run(){
            try {
                FileInputStream path = new FileInputStream(directoryToDownloads+"\\"+nSong);
                BufferedInputStream bis = new BufferedInputStream(path);
                player = new Player(bis);
                player.play();
            } catch (JavaLayerException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void stopSong() throws FileNotFoundException, JavaLayerException, InterruptedException{
        if (listOfDownloads.size() > 0){
            player.close();
        }
    }
    
    public static class buildServer implements Runnable {
        
        public buildServer(){
        }
        
        @Override
        public void run(){
            try {
                ServerSocket server = new ServerSocket(PORTC);
                Socket sc = null;
                System.out.println("SERVER LISTENING");
                
                while (true){
                    sc = server.accept();
                    System.out.println("CLIENT CONNECT " + sc);
                    new Thread(new threadToSentSong2(sc)).start();
                }   } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static class threadToSentSong implements Runnable {
        final Socket sc;
        
        public threadToSentSong(Socket sc){
            this.sc = sc;
        }
        
        @Override
        public void run(){
            
            try {
                BufferedOutputStream outDataSong = new BufferedOutputStream(sc.getOutputStream());
                DataInputStream inDataSong = new DataInputStream(sc.getInputStream());
                String s = inDataSong.readUTF();
                String[] dataS = s.split("/@/");
                if (dataS.length != 0){
                    File file = new File(directory+"\\"+dataS[1]);
                    try (BufferedInputStream dataFile = new BufferedInputStream(new FileInputStream(file))) {
                        int bytesRead;
                        byte [] buffer = new byte[10240];
                        while ((bytesRead = dataFile.read(buffer)) != -1){
                            outDataSong.write(buffer,0,bytesRead);
                        }
                        //outDataSong.flush();
                        outDataSong.close();
                    }
                    System.out.println("OK SONG");
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static class threadToSentSong2 implements Runnable {
        final Socket sc;
        
        
        public threadToSentSong2(Socket sc){
            this.sc = sc;
        }
        
        @Override
        public void run(){
            
            try {
                totalRequest++;
                BufferedOutputStream outDataSong = new BufferedOutputStream(sc.getOutputStream());
                DataInputStream inDataSong = new DataInputStream(sc.getInputStream());
                String s = inDataSong.readUTF();
                String[] dataS = s.split("/@/");
                String nameSong = dataS[1];
                int startReadBytes = Integer.parseInt(dataS[2]);
                int totalBytes = Integer.parseInt(dataS[3]); 
                
                System.out.println("song "+nameSong);
                System.out.println("start "+startReadBytes);
                System.out.println("total "+totalBytes);
                
                if (dataS.length != 0){
                    File fileAux = new File("test/byte"+Integer.toString(PORTC)+Integer.toString(totalRequest)+".byt");
                    BufferedOutputStream dataFileAuxOut = new BufferedOutputStream (new FileOutputStream(fileAux));
                    BufferedInputStream dataFileAuxIn = new BufferedInputStream(new FileInputStream(fileAux));
                    
                    File file = new File(directory+"\\"+nameSong);
                    try (BufferedInputStream dataFile = new BufferedInputStream(new FileInputStream(file))) {
                        int bytesRead;
                        byte [] bytesToSend = new byte[totalBytes-startReadBytes];
                        byte [] buffer = new byte[10240];
                        byte [] bytesSong = dataFile.readAllBytes();
                        int i;
                        int a=0;
                        for(i=startReadBytes;i<totalBytes-1;i++){
                            bytesToSend[a] = bytesSong[i];
                            a++;
                        }
                        System.out.println("byte "+ i);
                        dataFileAuxOut.write(bytesToSend);
                        
                        while ((bytesRead = dataFileAuxIn.read(buffer)) != -1){
                            outDataSong.write(buffer,0,bytesRead);
                        }
                        outDataSong.flush();
                        dataFileAuxOut.close();
                        dataFileAuxIn.close();
                    }
                    fileAux.delete();
                    outDataSong.close();
                    System.out.println("OK SONG");
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static boolean selectFolderToDownloads(){
        boolean confirm = true;
        if (firstDownload){
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int respuesta = fc.showOpenDialog(Frame);
            if (respuesta == JFileChooser.APPROVE_OPTION) {
                //Crear un objeto File con el archivo elegido
                //File archivoElegido = fc.getCurrentDirectory();
                //Mostrar el nombre del archvivo en un campo de texto
                directoryToDownloads = fc.getSelectedFile();
                firstDownload = false;
            }
            else{
                confirm = false;
            }
        }
        return confirm;
    }
    
    public static void selectNewFolderToDownloads(){
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int respuesta = fc.showOpenDialog(Frame);
        if (respuesta == JFileChooser.APPROVE_OPTION) {
            //Crear un objeto File con el archivo elegido
            //File archivoElegido = fc.getCurrentDirectory();
            //Mostrar el nombre del archvivo en un campo de texto
            directoryToDownloads = fc.getSelectedFile();
            firstDownload = false;
        }
    }
}
