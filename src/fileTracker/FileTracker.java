package fileTracker;
import pool.PeerPool;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class FileTracker {

    // The HashMap that stores information about the files:
    // mapped from a String - filename to a list of Strings
    // that contain information about the files
    private static HashMap<String, ArrayList<String>> files;
    // The Pool that stores all the online peers identifying them with sockets
	private static PeerPool<Socket> peerPool;
	// Two Hashmaps to be used for Scoring system
    // 1 - Keeps track of the Peer through storing his IP Address and port,
    // Integer[0] for NumOfRequests, Integer[1] for NumOfUploads
    // 2 - Keeps track of the Peer and his overall score
	private static HashMap<String, Integer[]> peersRequests;
    private static HashMap<String, Integer> peers;

	
	private static void server() throws IOException{

	    // Bind server to a port number = 4000
        // and define a welcoming socket
		int port = 4000;
		ServerSocket serverSocket = new ServerSocket(port);

		// Accept requests to connect while running
		while(true){
			Socket socket = serverSocket.accept();
			synchronized(peerPool){
			    // Add a newly connected socket to the peers' queue
                // for it to be served later in income() method
				peerPool.add(socket);
			}
		}
		
	}

	private static void income() {

	    // To provide an asynchronous execution mechanism
		int numThreads = 5;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		// Check for new peers in a pool while running
		while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
			// If no peers connected, continue running
			if(peerPool.peek() == null) continue;
			// To serve all the pending requests concurrently
			// and run a separate thread of FTPeerManager
            // for each new peer connected
			synchronized(peerPool){
			    Socket socket = peerPool.poll();
			    // An FT class to manage each peer connection;
                // after being initialized, added to the executor
                // for several FTPeerManagers to run concurrently
			    FTPeerManager s = new FTPeerManager(socket);
			    executor.execute(s);
			}
		}
		
	}

	// A method that registers in a FileTracker files provided by a peer
	public static void registerFiles(int peer_id, String peerAddress, String port, HashMap<String, String> peerfiles){

	    String newPeer = peerAddress + ", " + port;
	    Integer[] requests = new Integer[2];
	    requests[0] = 0; requests[1] = 0;
        peers.put(newPeer, 0);
	    peersRequests.put(newPeer, requests);

        for(Map.Entry<String, String> entry : peerfiles.entrySet()) {
            String key_filename = entry.getKey();

            // If the database has a file owned by a peer,
            // add this peer's info to the file's ArrayList<String>
            if(files.containsKey(key_filename)){
                files.get(key_filename).add(entry.getValue());
            }
            // If the database does not have a file owned by a peer,
            // create a new ArrayList<String>
            else {
                files.put(key_filename, new ArrayList<>());
                files.get(key_filename).add(entry.getValue());
            }
        }

	}

	public static void changeScore(String peer, int score) {
        Integer[] requests = peersRequests.get(peer);
        if(requests == null) {
            requests = new Integer[2];
            requests[0] = 1;
            if(score == 1) {
                requests[1] = 1;
            }
            else {
                requests[1] = 0;
            }
            peersRequests.put(peer, requests);
        } else {
            requests[0]++;
            if(score == 1) requests[1]++;
            peersRequests.put(peer, requests);
        }
        double x = ((double) requests[1])/requests[0];
        int new_score = (int) x * 100;
        peers.put(peer, new_score);
    }

    public static void peerLeave(String peer) {
        for (Map.Entry<String, ArrayList<String>> entry : files.entrySet()) {
            Iterator iterator = entry.getValue().iterator();
            while(iterator.hasNext()) {
                String str = (String) iterator.next();
                if(str.contains(peer)) {
                    iterator.remove();
                }
            }
        }
    }


    public static void main(String[] args) {

        files = new HashMap<>();
        peerPool = new PeerPool<>();
        peers = new HashMap<>();
        peersRequests = new HashMap<>();

        System.out.println("Waiting for peers to join at the port number = 4000"
                + " and IP Address = 127.0.0.1");

        new Thread() {
            public void run() {
                try {
                    server();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            public void run() {
                income();
            }
        }.start();

    }

	// A method that returns all the files available with ArrayList of info on peers that have them
	public static HashMap<String,ArrayList<String>> getFiles(){
		return files;
	}

	public static int getPeerScore(String peer){
	    int score = peers.get(peer);
        return score;
    }

    public static void printAll() {
        for(Map.Entry<String, ArrayList<String>> entry : files.entrySet()) {
            System.out.println(entry);
        }
    }

}
