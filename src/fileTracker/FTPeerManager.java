package fileTracker;

import java.io.*;
import java.net.*;
import java.util.*;

public class FTPeerManager extends Thread {
	
	private static int id = 0;
	private Socket socket;
	private ArrayList<String> filesinfo;

	// Class Constructor
    public FTPeerManager(Socket socket){
        this.socket = socket;
    }

	// To identify each peer;
    // Needs to be synchronized to avoid data race for
    // a variable shared between concurrently running threads
	private int generateID(){
		synchronized(this){
			return ++id;
		}
	}
	
	private Boolean search(String filename){
		if(FileTracker.getFiles().containsKey(filename)) {
            filesinfo = FileTracker.getFiles().get(filename);
            return filesinfo.size() != 0;
        } else {
		    return false;
        }
	}

    public void run(){

            try {

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                PrintStream out = new PrintStream(socket.getOutputStream(), true);

                String line = in.readLine();

                if (line == null) {
                    return;
                }

                System.out.println("FTPeerManager received " + line);

                if (line.equals("HELLO")) {

                    out.println("HI");
                    // To track connected peers
                    int peerId = generateID();

                    // To get and store the files sent by client
                    int filesnum = Integer.parseInt(in.readLine());
                    HashMap<String, String> filesinfo = new HashMap<>();

                    if (filesnum == 0) {
                        return;
                    }
                    String x, filename, fileinfo = "";
                    for (int i = 0; i < filesnum; i++) {
                        x = in.readLine();
                        StringTokenizer st = new StringTokenizer(x, ", ");
                        filename = st.nextToken();

                        StringBuilder y = new StringBuilder();
                        y.append(st.nextToken());
                        y.append(", ");
                        y.append(st.nextToken());
                        y.append(", ");
                        y.append(st.nextToken());
                        y.append(", ");
                        y.append(st.nextToken());
                        y.append(", ");
                        y.append(st.nextToken());

                        fileinfo = y.toString();

                        filesinfo.put(filename, fileinfo);
                    }

                    String IPAddress = "", port = "";
                    if (!fileinfo.equals("")) {
                        StringTokenizer st = new StringTokenizer(fileinfo, ", ");
                        st.nextToken();
                        st.nextToken();
                        st.nextToken();
                        IPAddress = st.nextToken();
                        port = st.nextToken();
                    }

                    // Inform the client on id he has been attached to
                    out.println(peerId);
                    out.flush();

                    // Finally, with all information read in
                    synchronized (this) {
                        FileTracker.registerFiles(peerId, IPAddress, port, filesinfo);
                    }
                } else if (line.startsWith("SEARCH")) {

                    String filename = line.substring(8);
                    Boolean contains = search(filename);

                    if (contains) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("FOUND: ");
                        for (String fileinfo : filesinfo) {
                            builder.append(fileinfo);

                            StringTokenizer st = new StringTokenizer(fileinfo, ", ");
                            st.nextToken();
                            st.nextToken();
                            st.nextToken();
                            String address = st.nextToken();
                            String port = st.nextToken();

                            String peer = address + ", " + port;
                            int score = FileTracker.getPeerScore(peer);

                            builder.append(", ");
                            builder.append(score);

                            builder.append("|");
                        }

                        String success = builder.toString();
                        out.println(success);
                        out.flush();
                    } else {
                        out.println("NOT FOUND");
                        out.flush();
                    }
                } else if (line.startsWith("SCORE")) {
                    System.out.println("Received: " + line);
                    StringTokenizer st = new StringTokenizer(line, ":");
                    String x = st.nextToken();
                    String y = st.nextToken();
                    int score = Integer.parseInt(y.substring(1));

                    FileTracker.changeScore(x.substring(9, x.length() - 1), score);
                    System.out.println("FTPM SCORE, name = " +  x.substring(9, x.length() - 1) + ", score = " + score);

                } else if (line.equals("BYE")) {
                    String peer = in.readLine();
                    FileTracker.peerLeave(peer);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

		
	}
}
