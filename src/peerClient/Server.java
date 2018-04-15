package peerClient;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Random;
import java.util.StringTokenizer;

import static java.lang.Math.toIntExact;

public class Server extends Thread{
	
	private String directory;
	private Socket socket;
	private Socket FTSocket;
	private Socket ServerSocket;
	private int FTPort = 4000;
    public static String FTAddress = "localhost";
	
	public Server(Socket socket, String directory){
		this.directory = directory;
		this.socket = socket;
	}
	
	public void run(){
		try {
            ServerSocket = new Socket(FTAddress, FTPort);
	        DataInputStream dIn = new DataInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            PrintWriter pwOut = new PrintWriter(socket.getOutputStream(), true);

            String line = dIn.readUTF();
            System.out.println("Peer received " + line);
            if(!line.startsWith("DOWNLOAD: ")){
                dIn.close();
                out.close();
                return;
            }

            String file = line.substring(10);
            StringTokenizer st0 = new StringTokenizer(file, ", ");

            String filename = st0.nextToken();

            StringBuilder y = new StringBuilder();
            y.append(st0.nextToken()); y.append(", ");
            y.append(st0.nextToken()); y.append(", ");
            y.append(st0.nextToken()); y.append(", ");

            String fileinfo = y.toString();

            //TODO: REMOVE THIS LINE
            System.out.println("P Server: request to send " + filename + ", " + fileinfo);

            StringTokenizer st1 = new StringTokenizer(fileinfo, ", ");
            String filetype = st1.nextToken();
            int filesize = Integer.parseInt(st1.nextToken());
            String fileLastModified = st1.nextToken();

            String file_to_send = filename;
            File folder = new File(directory);
            for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {

                int lastIndexOf = fileEntry.getName().lastIndexOf('.');

                String name = fileEntry.getName().substring(0, lastIndexOf);
                String type = fileEntry.getName().substring(lastIndexOf);

                int size = toIntExact(fileEntry.length());

                SimpleDateFormat sdf = new SimpleDateFormat("DD/MM/YY");
                String lastModified = sdf.format(fileEntry.lastModified());

                if(name.equals(filename) && type.equals(filetype) && (size == filesize) &&
                        lastModified.equals(fileLastModified) ) {
                    file_to_send = name + type;
                    break;
                }
            }

            InputStream in = new FileInputStream(directory + "/" + file_to_send);

            Random r = new Random();
            int p = r.nextInt(100 - 1) + 1;
            System.out.println("P Server: generated num = " + p);
            if(p < 50) {
                // REFERENCE:
                // https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
                // - the standard way to transfer files over a socket
                pwOut.println("FILE: "); pwOut.flush();

                int count;
                byte[] buffer = new byte[4096];
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }

            } else {
                pwOut.println("NO!"); pwOut.flush();
            }


	        dIn.close();
	        out.close();
	        in.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}
}
