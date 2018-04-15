package peerClient;

import pool.PeerPool;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {
	
	private int peer_id;
	private int files_num;
	private HashMap<String, String> files;
	private String directory;
	private String address;
	private int port;
	private int score;
	private PeerPool<Connection> peerPool;
    private ServerSocket serverSocket;
    private int FTPort = 4000;
    public static String FTAddress = "localhost";
    private Socket FTSocket;

	public Peer(String directory,  HashMap<String, String> files, int files_num, String address, int port){
		this.directory = directory;
		this.files = files;
		this.files_num = files_num;
		this.address = address;
		this.port = port;
		score = 0;
		peerPool = new PeerPool<>();
	}

	// As each peer is also a server, we need to keep its socket open
    // and welcoming new connections if any
    public void server() throws IOException{

        System.out.println("P: Ready to serve as a server...");
        try {
            serverSocket = new ServerSocket(port);
        } catch(Exception e){
            return;
        }

        while(true){
            Socket socket = serverSocket.accept();

            synchronized(peerPool){
                peerPool.add(new Connection(socket, directory));
            }
        }
    }

    public void income() {

	    System.out.println("P: Ready to accept income connections...");
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        while(true){
            if(peerPool.peek() == null){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            synchronized(peerPool){
                Connection c = peerPool.poll();
                Server s = new Server(c.getSocket(), c.getDirectory());
                executor.execute(s);
            }
        }

    }

    public void register(Socket socket, HashMap<String, String> files) throws IOException {

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("HELLO"); out.flush();
        String welcomeMsg = in.readLine();


        System.out.println("FT Responded: " + welcomeMsg);

        while(!welcomeMsg.equals("HI")) {
            System.out.println("An error occurred. Trying to reconnect...");
            out.println("HELLO");
            out.flush();
            welcomeMsg = in.readLine();
        }

        this.files = files;

        // To inform the server how many filenames it should expect
        files_num = files.size();
        // Remove the files with empty string names
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if(entry.getKey().equals("")) files_num--;
        }
        out.println(files_num); out.flush();

        // To pass the files info to the server
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if(!entry.getKey().equals("")) {
                out.println(entry.getKey() + ", " + entry.getValue());
            }
        }

        String input = in.readLine();

        // Set timeout to 30s
        try {
            socket.setSoTimeout(30000);
        } catch(SocketException e) {
            System.out.println("P: TIMEOUT - you are not accepted to FileTracker. Rerun the app.");
            System.exit(0);
        }

        // Get the id for a peer from the FTPeerManager
        peer_id = Integer.parseInt(input);

        System.out.println("P: PeerID assigned = " + peer_id);

        out.close();
        in.close();
        socket.close();

    }

    public ArrayList<String> searchFile(String filename, Socket socket, int count) throws IOException{

        ArrayList<String> records = new ArrayList<>();
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("SEARCH: " + filename); out.flush();
        String search_result = in.readLine();
        System.out.println("FT responded: " + search_result);

        if(search_result.startsWith("FOUND: ")){
            String recs = search_result.substring(7);
            StringTokenizer st = new StringTokenizer(recs, "|");

            while (st.hasMoreTokens()) {
                records.add(filename + ", " + st.nextToken());
            }
        } else if(search_result.equals("NOT FOUND")){
            records.add(filename + " NOT FOUND");
        }

        in.close();
        socket.close();
        return records;
    }

    // Downloads files to the current directory
    public void download(String peerAddress, int port, String filename, String fileinfo)  throws IOException {
    	Socket socket = new Socket(peerAddress, port);
    	FTSocket = new Socket(FTAddress, FTPort);

        // The streams to exchange the data as a stream of bytes if success in download
    	DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        InputStream in = socket.getInputStream();
        BufferedReader usualIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Get the type of a file to construct it later in case of a successful download
        StringTokenizer st = new StringTokenizer(fileinfo, ", ");
        String type = st.nextToken();

        // Connect to the FileTracker to report on Score of the peer
        PrintWriter FTOut = new PrintWriter(FTSocket.getOutputStream(), true);

        dataOutputStream.writeUTF("DOWNLOAD: " + filename + ", " + fileinfo);
        String response = usualIn.readLine();

        if(response.startsWith("FILE: ")){
            // Save the downloads into the same folder from which the peer is running
            OutputStream out = new FileOutputStream(System.getProperty("user.dir") + "/" + filename + type);

            // REFERENCE:
            // https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
            // - the standard way to transfer files over a socket
            int count;
            byte[] buffer = new byte[4096];
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }

            FTOut.println("SCORE of " + peerAddress + ", " + port + " : 1"); FTOut.flush();

            out.close();
        } else if (response.equals("NO!")) {
            FTOut.println("SCORE of " + peerAddress + ", " + port + " : 0"); FTOut.flush();
        }

        dataOutputStream.close();
        in.close();
        socket.close();
    }

    public void exit(String address, int port) throws IOException {

        FTSocket = new Socket(FTAddress, FTPort);
        PrintWriter FTOut = new PrintWriter(FTSocket.getOutputStream(), true);

        //TODO: REMOVE THIS LINE
        System.out.println("Client is leaving and = " + address + ", " + port + ".");
        FTOut.println("BYE"); FTOut.flush();
        FTOut.println(address + ", " + port);
    }
    
    
}

	
