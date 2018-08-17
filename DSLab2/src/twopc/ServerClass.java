package twopc;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ServerClass {
	
	/*server listens on this port- 9998*/
	private static int port = 9998;
	private static ServerSocket ss;
	private static Socket client;
	private static DataOutputStream dos;
	//Unique Id of coordinator
	private static final String COORDINATOR_NAME = "COORDINATOR_77";
	
	/*Array-list to store the usernames of clients online*/
	private ArrayList<String> userNames;
	
	/*Array-list to store the data-output streams of online clients 
	 in order for server to broadcast messages*/
	private ArrayList<DataOutputStream> streams;
	private DataOutputStream coordinatorStream;
	
	/*Static Http variables used for building http request headers*/
	private final static String host = "Host: localhost";
	private final static String userAgent = "User-Agent: MultiChat/2.0";
	private final static String contentType = "Content-Type: text/html";
	private final static String contentlength = "Content-Length: ";
	private final static String date = "Date: ";
	
	private JFrame frmCoordinator;
	private JTextArea textArea;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerClass window = new ServerClass();
					window.frmCoordinator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*Constructor of Server Class which initializes the GUI Frame*/
	public ServerClass() {
		
		/*calls the method to initialize the contents of GUI Frame*/
		initialize();
	}

	/*Method to Initialize the contents of the Swing GUI frame(Server).*/
	private void initialize() {
		frmCoordinator = new JFrame();
		frmCoordinator.setTitle("Server");
		frmCoordinator.setBounds(100, 100, 509, 387);
		frmCoordinator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCoordinator.getContentPane().setLayout(null);
		
		JButton btnUsers = new JButton("Users");
		btnUsers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(userNames.isEmpty()) {
					textArea.append("No User is online. \n");
				}
				
				else {
					textArea.append("Online users are: \n");
					for (String user : userNames) {
						textArea.append(user+"\n");
					}
				}
			}
		});
		btnUsers.setBounds(362, 300, 89, 23);
		frmCoordinator.getContentPane().add(btnUsers);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(34, 35, 412, 222);
		frmCoordinator.getContentPane().add(scrollPane);
		
		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
		/*This sole thread is dedicated for starting and maintaining server connection. 
		  It calls the startsServerConnection method that actually initializes the server.*/
		Thread t = new Thread () {
			
			@Override
			public void run() {
				startServerConnection();
			};
		};
		t.start();
	}
	
	/*Method to initialize the server connection.*/
	protected void startServerConnection() {

		try {
			ss = new ServerSocket(port);
			userNames = new ArrayList<>();
			streams = new ArrayList<>();
			textArea.append("-------SERVER STARTED------\n");
			
			
			/*This loop is used for listening client connections on server port*/
			while(true) {
				
				/*Client has connected to server socket*/
				client = ss.accept();

				dos = new DataOutputStream(client.getOutputStream());
				streams.add(dos);
				
				
				/*We create an instance of this client socket's handler(which is a nested class within 
				  this ServerNew java class and pass the parameters: client socket 
				and that socket's dataoutput stream in the serverclienthandler's constructor*/
				
				ServerClientHandler sch = new ServerClientHandler(client, dos);
				
				/*Initiate the thread for handling this sole client session*/
				sch.start();
			}
		}
		catch (IOException e) {
			e.getMessage();
		}
	}
	
	/*Method to send the messages/decisions/votes to all participents*/
	private void SendDataAllParticipants(String msg) {
		
		/*String builder object to encode the message in Http Request format*/
		StringBuilder sbr = new StringBuilder();
		
		for (DataOutputStream dataOutputStream : streams) {
			try {
				
				sbr.append("POST /").append(msg).append("/ HTTP/1.1\r\n").append(host).append("\r\n").
				append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(msg.length()).append("\r\n").
				append(date).append(new Date()).append("\r\n");
				
				dataOutputStream.writeUTF(sbr.toString());
				
			} catch (Exception e) {
			}
		}
		
	}
	
	/*Method to send the messages/votes/etc data to coordinator*/
	private void sendCoordinator(String data) {
		
		/*String builder object to encode the message in Http Request format*/
		StringBuilder sbr = new StringBuilder();
		
		try {
			sbr.append("POST /").append(data).append("/ HTTP/1.1\r\n").append(host).append("\r\n").
			append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(data.length()).append("\r\n").
			append(date).append(new Date()).append("\r\n");
			
			coordinatorStream.writeUTF(sbr.toString());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/*Nested Class used for MultiThreading and Handling multiple clients at same time.*/
	public class ServerClientHandler extends Thread {
		
		private Socket csoc;
		private String cname;
		private DataInputStream diss;
		
		/*Constructor of this Participant Handler Class, its parameters are client object 
		and client socket's data outputstream*/
		public ServerClientHandler(Socket client, DataOutputStream dosss) {
			this.csoc = client;
			try {
				diss = new DataInputStream(csoc.getInputStream());
			} catch (Exception e) {
				e.getMessage();
			}
		}
		
		/*overriding the thread's run method. this method runs as soons as we send command:
		thread.start();*/
		
		@Override
		public void run() {
			
			String line = "",msgin;
			String arr[];
			
			try {

				while((line = diss.readUTF())!=null) {
				
					textArea.append(line);
					
					arr = line.split("\n");
					
					msgin = arr[0].split("/")[1];

					/*Reconstructing the message body from the Http Header.
					  This code decodes the Http message body.*/
					if(arr[0].contains("GET")) {
						
						if(msgin.contains("{")) {
							cname = msgin.split("\\{")[1];
							cname = cname.replace(cname.substring(cname.length()-1),"");
							
							/*check to know this client is not coordinator*/
							if(!cname.equalsIgnoreCase(COORDINATOR_NAME)) {
								
							//	textArea.append("New Client connected: "+cname+"\n");
								userNames.add(cname);
								sendCoordinator("CONNECTED:"+cname+":");
								
							} else {

								/*This is the coordinator*/
								if(streams.size()==1) {
									coordinatorStream = new DataOutputStream(streams.remove(0));
								}else {
									coordinatorStream = new DataOutputStream(streams.remove(streams.size()-1));
									sendCoordinator("USER_LIST:"+userNames.toString());
								}
							}
							
						}
					}
					else if(arr[0].contains("POST")) {
						
						if(msgin.contains("VOTE_REQUEST")) {
							
							SendDataAllParticipants(msgin);
						//	textArea.append("Sent the vote request to participants.\n");
							
						}
						else if(msgin.equalsIgnoreCase("ABORT")) {
								
							//		textArea.append("Abort Vote by client: "+cname+"\n");
									
									sendCoordinator("ABORT"+":"+cname+":");
								
							}else if(msgin.equals("COMMIT")){
								
							//	textArea.append("Commit Vote by client: "+cname+"\n");
								sendCoordinator("COMMIT"+":"+cname+":");
								
							} else if(msgin.contains("GLOBAL_ABORT")) {
								
								String glab = msgin.split(":")[0];
								SendDataAllParticipants(glab);
								
							}
							else if(msgin.contains("GLOBAL_COMMIT")) {
								
								String glcm = msgin.split(":")[0];
								SendDataAllParticipants(glcm);
							}
								
					}
				}
					
			} 
			/*In case client connection is disconnected, even if client does not press LOGOUT button,
			the server will close the client connection and log it off*/
			catch (IOException e) {
					
			//	textArea.append(cname+" has Disconnected\n");
				
				if(!cname.equals(COORDINATOR_NAME)) {
					sendCoordinator("LOGGEDOUT:"+cname+":");
					//	Removing the client's username from it's memory or arraylist
					userNames.remove(cname);
				}
					
				}
			}

		}

}
