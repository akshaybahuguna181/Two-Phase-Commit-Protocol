package twopc;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ParticipantClass {

	private int port = 9998;
	private String ip = "localhost";
	private DataOutputStream dos;
	private static DataInputStream dis;
	private Socket clientsoc;
	private static String clientname;
	
	private String state = "INIT";
	private String data = "";
	private String decision = "";
	
	/*Boolean flag to indicate whether the client is logged in or logged out*/
	private boolean connected = false;
	
	/*Regex for filtering bad usernames (not alpha-numeric)*/
	public String regex = "^[a-zA-Z0-9]+$";
	
	/*Static Http variables used for building http request headers*/
	private final static String host = "Host: localhost";
	private final static String userAgent = "User-Agent: MultiChat/2.0";
	private final static String contentType = "Content-Type: text/html";
	private final static String contentlength = "Content-Length: ";
	private final static String date = "Date: ";
	
	/*These 4 file reader/writer variables used for Server Logging*/
	private FileWriter fw;
	private BufferedWriter bw;
	private FileReader fr;
	private BufferedReader br;
	
	private Timer t;
	
	private JFrame frame;
	private JTextField textRegName;
	private static JTextArea chatArea;
	private JButton btnAbort;
	private JLabel lblNewLabel;


	/*Main method is started first and starts the Participant Class and initializes the GUI Frame*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					/*Instantiating Participant Class*/
					ParticipantClass window = new ParticipantClass();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	/*Constructor of Participant Class which initializes the GUI Frame*/
	public ParticipantClass() {
		
		/*calls the method to initialize the contents of GUI Frame*/
		initialize();
	}

	/*Method to Initialize the contents of the Swing GUI frame (Participant).*/
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 484, 349);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		/*Button for participant to abort the incoming string or transaction*/
		btnAbort = new JButton("Abort");
		btnAbort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if(!(data.equals("")||data.trim().isEmpty())) {
					
					try {
						/*String builder object to encode the message in Http Request format*/
						StringBuilder sbconnreq = new StringBuilder();
						
						/*Building the Http Connection Request and passing Client name as body. Thus the Http Header
					are encoded around the client name data.*/
						sbconnreq.append("POST /").append("ABORT").append("/ HTTP/1.1\r\n").append(host).append("\r\n").
						append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(clientname.length()).append("\r\n").
						append(date).append(new Date()).append("\r\n");
						
						dos.writeUTF(sbconnreq.toString());
						
						chatArea.append("Voted to: ABORT\n");
						
						state = "ABORT";
						data = "";
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				else {
					JOptionPane.showMessageDialog(null, "Can't Vote Now !");
				}
			}
		});
		btnAbort.setBounds(50, 231, 106, 37);
		frame.getContentPane().add(btnAbort);
		
		/*Button for participant to commit the incoming string or transaction*/
		JButton btnCommit = new JButton("Commit");
		btnCommit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
					/*check to prevent participant from voting more than once in middle of transaction*/
					if((state.equals("READY")||(data.trim().isEmpty()||data.equals("")))) {
						JOptionPane.showMessageDialog(null, "Can't Vote Now !");
					}
					else {
						
						try {
							StringBuilder sbconnreq = new StringBuilder();
							
							/*Building the Http Connection Request and passing Client name as body. Thus the Http Header
							are encoded around the client name data.*/
							sbconnreq.append("POST /").append("COMMIT").append("/ HTTP/1.1\r\n").append(host).append("\r\n").
							append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(clientname.length()).append("\r\n").
							append(date).append(new Date()).append("\r\n");
							
							dos.writeUTF(sbconnreq.toString());
							
							chatArea.append("Voted to: COMMIT\n");
							
							/*state transistion to Ready state*/
							state = "READY";
							decision = "";
							
							/*Initiating a new timer to keep track of decision of coordinator and times out 
							if it doesnt arrive in stipulated time*/
							t = new Timer();
							
							/*initiating a timer task to be associated with timer which performs the actual check
							of whether the decision has arrived or not*/
							TimerTask tt2 = new TimerTask() {
								
								@Override
								public void run() {
									/*check to see whether decision has arrived or not*/
									if(!(decision.equals("g_c")||decision.equals("g_a"))) {
										chatArea.append("Did not receive decision from coordinator.. SO LOCAL ABORT !\n");
										data = "";
										state = "ABORT";
									}
								}
							};
							/*scheduling this check task every one minute(60000 miliseconds)*/
							t.schedule(tt2, 60000);
							
						} catch (IOException e1) {
							e1.printStackTrace();
					}
						
						
				}
			}
		});
		btnCommit.setBounds(255, 231, 106, 37);
		frame.getContentPane().add(btnCommit);
		
		/*Entering participant's username to register to server
*/		JLabel lblEnterParticipantName = new JLabel("Enter Participant Name");
		lblEnterParticipantName.setBounds(33, 11, 135, 14);
		frame.getContentPane().add(lblEnterParticipantName);
		
		textRegName = new JTextField();
		textRegName.setBounds(178, 8, 146, 20);
		frame.getContentPane().add(textRegName);
		textRegName.setColumns(10);
		
		/*Press this button to connect to server*/
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				/*Code to make sure the already logged in client can't connect again by using boolean flag
				*/				if(connected == true) {
									JOptionPane.showMessageDialog(null, "You are already connected !");
								}

								/*Only if client is not already logged in, will it be able to 
								send connection request to server*/
								else if(connected == false) {
									
									clientname = textRegName.getText().trim();
									
									/*Checking for bad client usernames and accepting only alphanumeric names*/
									if(clientname.equals(null)||clientname.trim().isEmpty()||(!Pattern.matches(regex, clientname)))
									{
										JOptionPane.showMessageDialog(null, "Please enter an alphanumeric username to connect to server! ");
									}
									
									else {
										
										/*calling the method to start client connection.*/
										startClientConnection();

										/*Initiating a new timer to keep track of voting request of coordinator and times out 
										if it doesn't arrive in stipulated time*/
										t = new Timer();
										
										/*initiating a timer task to be associated with timer which performs the actual check
										of whether the voting request has arrived or not*/
										TimerTask tt = new TimerTask() {
											
											@Override
											public void run() {
												if(data.equals("")&&state.equals("INIT")) {
													chatArea.append("Did Not get Voting Request...Local Abort!\n");
													state = "ABORT";
												}
											}
										};
										/*scheduling this check task in one minute(60000 miliseconds)*/
										t.schedule(tt, 35000);
									}
								}

				
			}
		});
		btnConnect.setBounds(334, 7, 89, 23);
		frame.getContentPane().add(btnConnect);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(33, 51, 337, 157);
		frame.getContentPane().add(scrollPane);
		
		chatArea = new JTextArea();
		scrollPane.setViewportView(chatArea);
		chatArea.setEditable(false);
		
		/*Additional button to tell the state of participant*/
		JButton btnState = new JButton("STATE");
		btnState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lblNewLabel.setText(state);
			}
		});
		btnState.setBounds(380, 95, 78, 66);
		frame.getContentPane().add(btnState);
		
		lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(390, 172, 68, 14);
		frame.getContentPane().add(lblNewLabel);
	}
	
	/*Requests the server for Connection by creating as stream socket fotr the server port number- 9998*/
	private void startClientConnection() {
			
		try {
			
			/*making connection request*/
			clientsoc = new Socket(ip,port);
			
			/*Input and output streams for data sending and receiving through client and server sockets.*/
			dis = new DataInputStream(clientsoc.getInputStream());	
			dos = new DataOutputStream(clientsoc.getOutputStream());
			
			/*Creating a new file if it does not exist for each participant this acts as non volatile memory*/
			File f = new File(clientname);
			f.createNewFile();
			
			StringBuilder sbconnreq = new StringBuilder();

			/*Building the Http Connection Request and passing Client name as body. Thus the Http Header
			are encoded around the client name data.*/
			sbconnreq.append("GET /").append("{"+clientname+"}").append("/ HTTP/1.1\r\n").append(host).append("\r\n").
			append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(clientname.length()).append("\r\n").
			append(date).append(new Date()).append("\r\n");
			
			dos.writeUTF(sbconnreq.toString());

			chatArea.append("You have logged in!\n");
			connected = true;
			
			/*Buffered and file reader to read if a file(non volatile memory) already exists for this
			participant to read previously commited data from*/
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			String smh = "";
			while(!((smh = br.readLine())==null)) {
				chatArea.append("Previously Commited Data: \n");
				chatArea.append(smh);
				chatArea.append("\n");
			}
			br.close();
			
			/*File writer to write into participant's file, the commited data*/
			fw = new FileWriter(f,true);
			bw = new BufferedWriter(fw);
		} 	
		catch (FileNotFoundException e) {
			chatArea.append("File not located..exception");
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Server Down. Couldn't Connect");
		}
		
		/*instantiate a new object of nested Classs(which extends Thread) and
		 invokng it's run method to start the thread*/
		new ParticipantThread().start();
	}
	
	/*Nested Class of ParticipantClass, this class extends Thread Class because this is used as a dedicated thread 
	to handle incoming messages from coordinator and server such as Vote Request, Decision etc*/
	public class ParticipantThread extends Thread{
		
		/*overriding the thread's run method. this method runs as soon as we send command:
		thread.start();*/
		
		@Override
		public void run() {
			
			String readChat = ""; 
			String arr[],msgin;
			
			try {
				
			while(true) {
				
				readChat = dis.readUTF();
					
					/*Deconstructing the HTTP message from server
					and decoding it from http format to read the contents*/
					arr = readChat.split("\n");
					
					msgin = arr[0].split("/")[1];
					
					/*Decoding for POST type messages from Server*/
					if(arr[0].contains("POST")) {
						
						/*In case decision is global commit*/
						if(msgin.equalsIgnoreCase("GLOBAL_COMMIT")) {
							decision = "g_c";
							
							/*State Transition from Ready to Commit after decision of coordinator*/
							state = "COMMIT";
							chatArea.append("Commiting due to Global Commit!\n");
							
							/*In case of Global Commit, We write this string to non volatile memory 
							  i.e. File in this case*/
							bw.write(data);
							bw.newLine();
							bw.flush();
							
							/*Set the reveived data to empty as this has been commited now*/
							data = "";
						}
						/*In case decision is global abort*/
						else if(msgin.equalsIgnoreCase("GLOBAL_ABORT")) {
							
							chatArea.append("Aborting due to Global Abort!\n");
							
							/*State Transition from Ready to Abort after decision of coordinator*/
							state = "ABORT";
							decision = "g_a";
							
							data = "";
							
						}else {
							/*This is the Vote request from coordinator so decoding it*/
							data = msgin.split(":")[0];
							chatArea.append("VOTE to COMMIT OR ABORT the string: "+data+"\n");
							
							/*Stopping timer as participant has received the Vote Request within stipulated time*/
							t.cancel();
							t.purge();
						}
					}
					
					
				}
			}
				/*When connection with server has lost due to server crash*/
				catch (Exception e) {
					chatArea.append("SERVER DOWN....");
				}
			
		}
	}
}
