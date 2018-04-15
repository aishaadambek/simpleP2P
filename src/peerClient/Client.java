package peerClient;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import static java.lang.Math.toIntExact;


public class Client extends JFrame implements ActionListener {

    private static Peer peer;
    // FileTracker information
    private static String serverAddress = "localhost";
    private static int serverPort = 4000;

    // This peers' port and IP address
    private static int port;
    private static String address;

    // Buttons
    private JButton search;
    private JButton dload;
    private JButton close;
    private JButton clear;
    // List that will show found files
    private JList jl;
    // Label "File Name"
    private JLabel label;
    // Two text fields: one is for typing a file name, the other is just to show the selected file
    private JTextField tf,tf2;
    // Used to select items in the list of found files
    private DefaultListModel listModel;

    private Client(){
        super("P2P File Sharing System");
        setLayout(null);
        setSize(500,600);

        label=new JLabel("File name:");
        label.setBounds(50,50, 80,20);
        add(label);

        tf=new JTextField();
        tf.setBounds(130,50, 220,20);
        add(tf);

        search=new JButton("Search");
        search.setBounds(360,50,80,20);
        search.addActionListener(this);
        add(search);

        listModel = new DefaultListModel();
        jl=new JList(listModel);

        JScrollPane listScroller = new JScrollPane(jl);
        listScroller.setBounds(50, 80,300,300);

        add(listScroller);

        dload=new JButton("Download");
        dload.setBounds(200,400,130,20);
        dload.addActionListener(this);
        add(dload);

        tf2=new JTextField();
        tf2.setBounds(100,430,330,20);
        add(tf2);

        close=new JButton("Close");
        close.setBounds(360,470,80,20);
        close.addActionListener(this);
        add(close);

        clear = new JButton("Clear");
        clear.setBounds(60,470,80,20);
        clear.addActionListener(this);
        add(clear);

        setVisible(true);
    }


    public static HashMap<String, String> listFilesForFolder(final File folder, int port, String address) throws IOException {

        HashMap<String, String> files = new HashMap<>();
        int limit = 0;

        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            int lastIndexOf = fileEntry.getName().lastIndexOf('.');

            String name = fileEntry.getName().substring(0, lastIndexOf);
            String type = fileEntry.getName().substring(lastIndexOf);

            int size = toIntExact(fileEntry.length());

            SimpleDateFormat sdf = new SimpleDateFormat("DD/MM/YY");
            String lastModified = sdf.format(fileEntry.lastModified());

            String fileInfo = type + ", " + size + ", "
                    + lastModified + ", " + address + ", " + port;

            files.put(name, fileInfo);

            limit++;
            if(limit == 5) {
                break;
            }
        }

        return files;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == search){
            // Search button was pressed
            String filename = tf.getText();
            ArrayList<String> records;
            try {
                // Look for a file in a FileTracker
                int i = 0;
                records = peer.searchFile(filename, new Socket(serverAddress, serverPort), 1);
                for(String record : records) {
                    listModel.insertElementAt(record, i);
                    i++;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else if(e.getSource() == dload){
            // Download button was pressed
            if(jl.getSelectedValue() == null) {
                tf2.setText("Error! You need to SELECT the file to download");
            } else {
                String record = jl.getSelectedValue().toString();

                if(record.contains("NOT FOUND")) {
                    tf2.setText("The file cannot be downloaded");
                    return;
                }

                tf2.setText(record + " to download");

                StringTokenizer st = new StringTokenizer(record, ", ");
                String filename = st.nextToken();
                String type = st.nextToken();
                String size = st.nextToken();
                String last_modified = st.nextToken();
                String IPAddress = st.nextToken();
                int port = Integer.parseInt(st.nextToken());

                try {
                    String fileinfo = type + ", " + size + ", " + last_modified;
                    peer.download(IPAddress, port, filename, fileinfo);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } else if(e.getSource() == close){
            // Close button was pressed
            try {
                peer.exit("127.0.0.1", port);
                System.exit(0);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.exit(0);
        } else if(e.getSource() == clear){
            // /Clear button was pressed
            listModel.removeAllElements();
        }
    }


    public static void main(String[] args) throws IOException  {

        Scanner keyboardInput = new Scanner(System.in);
		System.out.println("Welcome to a Simple P2P File Sharing system!" +
                "\nTO START Please provide the following:" +
                "\n1 - a directory name where you store your files for sharing");

        String directory = keyboardInput.nextLine();
        File folder = new File(directory);
        if(!folder.isDirectory()){
            System.out.println("Incorrect input. A valid directory name is required. Restart the app.");
            return;
        } else {
            System.out.println("2 - a port number (as an integer) you want to be running on");
        }
        port = keyboardInput.nextInt();
        if(port < 1024 || port > 65535) {
            System.out.println("Port number out of range. Restart the app.");
            return;
        }

        // Run a peer on a local host
	    address = InetAddress.getLocalHost().getHostAddress();

	    // Create an ArrayList of file names
    	HashMap<String, String> files = listFilesForFolder(folder, port, "127.0.0.1");

  	    // Initialize the peer class for the running client
        peer = new Peer(directory, files, files.size(), address, port);

    	Socket socket;
    	try {
    		socket = new Socket(serverAddress, serverPort);
    	} catch (IOException e){
    		System.out.println("No running FileTracker available. Please, try again.");
    		return;
    	}

    	// To welcome a FileTracker and create the connection as required
        // i.e. by following the protocol and sending the files information
        peer.register(socket, files);

        // Turn on the GUI
        Client client = new Client();
        client.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // For close operation
        // REFERENCE:
        // https://stackoverflow.com/questions/9093448/do-something-when-the-close-button-is-clicked-on-a-jframe
        client.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(client,
                        "Are you sure to close this window?", "Confirm your action",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                    try {
                        peer.exit("127.0.0.1", port);
                        System.exit(0);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

    	// Now run each peer also as a server that can accept invitations
        // and share its files
        new Thread(){
            public void run(){
                try {
                    peer.server();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread(){
            public void run(){
                peer.income();
            }
        }.start();

    }



}
