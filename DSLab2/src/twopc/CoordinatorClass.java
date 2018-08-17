package twopc;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class CoordinatorClass {
	
	/*server listens on this port- 9998*/
	private static int port = 9998;
	/*Ip address of server*/
	private String ip = "localhost";
	private static Socket coordinatorSoc;
	private static DataInputStream dis;
	private static DataOutputStream dos;
	
	/*flag to maintain state of coordinator*/
	private String state;
	
	//Unique Id of coordinator
	private static final String COORDINATOR_NAME = "COORDINATOR_77";
	
	/*Various flags to represent diffferent states of Coordinator*/
	private static final String init = "INIT",
								waiting = "WAITING",
								commit = "COMMIT",
								abort = "ABORT";
	
	/*Array-list to store the usernames of clients online*/
	private ArrayList<String> userNames;
	
	/*Array List to store the names of participants to remove(if they get disconnected)
	after each transaction*/
	private ArrayList<String> toRemove;
	
	/*Hashmap to store the votes of all participants*/
	private HashMap<String, String> votes;
	
	private boolean allVoted = false;
	
	/*Timer to calculate the 2PC timeout scenarios of coordinator*/
	private Timer t;
	
	/*Static Http variables used for building http request headers*/
	private final static String host = "Host: localhost";
	private final static String userAgent = "User-Agent: MultiChat/2.0";
	private final static String contentType = "Content-Type: text/html";
	private final static String contentlength = "Content-Length: ";
	private final static String date = "Date: ";

	/*Instance variables for Coordinator GUI (Java Swing Class)*/
	private JTextArea textArea;
	private JFrame frmCoordinator;
	private JTextField textField;

	/*Main method which is run first and starts the Coordinator Class and initializes the GUI Frame*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					/*Instantiating Coordinator Class*/
					CoordinatorClass window = new CoordinatorClass();
					window.frmCoordinator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*Constructor of Coordinator Class which initializes the GUI Frame*/
	public CoordinatorClass() {
		
		/*calls the method to initialize the contents of GUI Frame*/
		initialize();
	}

	 /*Method to Initialize the contents of the Swing GUI frame(Coordinator).*/
	private void initialize() {
		frmCoordinator = new JFrame();
		frmCoordinator.setTitle("Coordinator");
		frmCoordinator.setBounds(100, 100, 509, 387);
		frmCoordinator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCoordinator.getContentPane().setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Enter String");
		lblNewLabel.setBounds(45, 11, 177, 26);
		frmCoordinator.getContentPane().add(lblNewLabel);
		
		textField = new JTextField();
		textField.setBounds(45, 48, 196, 21);
		frmCoordinator.getContentPane().add(textField);
		textField.setColumns(10);
		
		
		/*Sends Vote Request to participants with option to either commit or abort this string*/
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				/*this msg is the string to be sent to participants for vote*/
				String msg = textField.getText();
				
				if(msg.equals(null)||msg.trim().isEmpty()){
					
					JOptionPane.showMessageDialog(null, "Please enter some valid string except just spaces! ");
					
				} else {
					
					try {
						/*String builder object to encode the message in Http Request format*/
						StringBuilder sbvotereq = new StringBuilder();
						
						/*Encoding the vote request into HTTP format to be sent across the network*/
						sbvotereq.append("POST /").append(msg+":VOTE_REQUEST").append("/ HTTP/1.1\r\n").append(host).append("\r\n").
						append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(COORDINATOR_NAME.length()).append("\r\n").
						append(date).append(new Date()).append("\r\n");
						
						/*Sending the vote request across data output stream of coordinator socket*/
						dos.writeUTF(sbvotereq.toString());
						
						/*COORDINATOR STATE TRANSITION FROM INIT TO WAITING*/
						state = waiting;
						
						allVoted = false;
						
						/*Initializing the timer to calculate timeout in case all votes do not come*/
						t = new Timer();
						
						/*Making a timer task to perform the function of checking whether all votes have arrived after a particular time period*/
						TimerTask tt1 = new TimerTask() {
							
							@Override
							public void run() {
								
								int votecount = 0;
								for (String participant : votes.keySet()) {
									if(votes.get(participant).equals("COMMIT")) {
										votecount++;
									}
									else {
										break;
									}
								}
								/*check to see if all participants have voted or not*/
								if(votecount!=votes.size()) {
									textArea.append("Time Out. Not all participants voted.\n");
									textArea.append("Initiating Global Abort.\n");
									
									/*String builder object to encode the message in Http Request format*/
									StringBuilder sbglobalabort = new StringBuilder();
									
									sbglobalabort.append("POST /").append("GLOBAL_ABORT:"+COORDINATOR_NAME).append("/ HTTP/1.1\r\n").append(host).append("\r\n").
									append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(COORDINATOR_NAME.length()).append("\r\n").
									append(date).append(new Date()).append("\r\n");
									
									try {
										dos.writeUTF(sbglobalabort.toString());
									} catch (IOException e) {
										e.printStackTrace();
									}
									
									while(!toRemove.isEmpty()) {
										textArea.append("removed these participant now: "+toRemove.get(0)+"\n");
										userNames.remove(toRemove.get(0));
										votes.remove(toRemove.remove(0));
									}
									toRemove.clear();
									
									for (String client : votes.keySet()) {
										votes.put(client, "");
									}
									state = abort;
								}
								
								
							}
						};
						/*scheduling this check task 45 seconds*/
						t.schedule(tt1, 45000);
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					textField.setText("");
				}
				
			}
		});
		btnSend.setBounds(251, 47, 66, 23);
		frmCoordinator.getContentPane().add(btnSend);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(45, 92, 306, 185);
		frmCoordinator.getContentPane().add(scrollPane);
		
		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
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
		btnUsers.setBounds(374, 128, 89, 23);
		frmCoordinator.getContentPane().add(btnUsers);
		
		JLabel lblState = new JLabel("");
		lblState.setBounds(374, 217, 89, 48);
		frmCoordinator.getContentPane().add(lblState);

		JButton btnState = new JButton("STATE");
		btnState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lblState.setText(state);
			}
		});
		btnState.setBounds(374, 183, 89, 23);
		frmCoordinator.getContentPane().add(btnState);
		
		startClientConnection();
	}
	
	/*Requests the server for Connection by creating as stream socket fotr the server port number- 9998*/
	private void startClientConnection() {
			
		try {
			
			/*making connection request*/
			coordinatorSoc = new Socket(ip,port);
	//		coordinatorSoc.setSoTimeout(10000);
			
			userNames = new ArrayList<>();
			votes = new HashMap<>();
			toRemove = new ArrayList<>();
			
			state = init;
			
			/*Input and output streams for data sending and receiving through client and server sockets.*/
			dis = new DataInputStream(coordinatorSoc.getInputStream());	
			dos = new DataOutputStream(coordinatorSoc.getOutputStream());
			
			/*String builder object to encode the message in Http Request format*/
			StringBuilder sbconnreq = new StringBuilder();

			/*Building the Http Connection Request and passing Client name as body. Thus the Http Header
			are encoded around the client name data.*/
			sbconnreq.append("GET /").append("{"+COORDINATOR_NAME+"}").append("/ HTTP/1.1\r\n").append(host).append("\r\n").
			append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(COORDINATOR_NAME.length()).append("\r\n").
			append(date).append(new Date()).append("\r\n");
			
			dos.writeUTF(sbconnreq.toString());
		//	otherusers = new ArrayList<>(10);
		//	usertimestamp = new HashMap<>(10);
			
		} 		
		
		catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Server Down. Couldn't Connect");
		}
		
		/*instantiate a new object of nested Class(which extends Thread) and
		 invoking it's run method to start the thread*/
		new CoordinatorThread().start();
	}
	
	
	
	
	/*Nested Class of CoordinatorClass, this class extends Thread Class because this is used as a dedicated thread 
	to handle incoming messages from server and clients such as Votes, events such as participant login etc*/
	public class CoordinatorThread extends Thread {
		
		/*overriding the thread's run method. this method runs as soons as we send command:
		thread.start();*/
		
		@Override
		public void run() {
			
			String line = "",msgin,data;
			String arr[];
			
			try {

				while((line = dis.readUTF())!=null) {
				
					arr = line.split("\n");
					
					msgin = arr[0].split("/")[1];
					
					/*Reconstructing the message body from the Http Header.
					  This code decodes the Http message body.*/
					if(arr[0].contains("POST")) {
						
						/*new participant connected*/
						if(msgin.contains("CONNECTED")) {
							
							data = msgin.split(":")[1];
							
							if(!(data.equalsIgnoreCase(COORDINATOR_NAME))) {
								
								/*keeping participants in list to keep track*/
								userNames.add(data);

								/*initially keeping all votes as empty as transaction hasn't begun yet*/
								votes.put(data, "");
								
								textArea.append("new participant : "+data+"\n");
							}
							
						}
						/*participant has voted to abort*/
						else if(msgin.contains("ABORT")) {
							
							data = msgin.split(":")[1];
							votes.put(data, "ABORT");
							state = abort;
							textArea.append("Abort Vote by client: "+data+"\n");
							textArea.append("One client voted to Abort so initiating GLOBAL ABORT !\n");
							//		Thread.sleep(2000);
							
							t.cancel();
							t.purge();
							
							/*String builder object to encode the message in Http Request format*/
							StringBuilder sbglobalabort = new StringBuilder();
							
							sbglobalabort.append("POST /").append("GLOBAL_ABORT:"+COORDINATOR_NAME).append("/ HTTP/1.1\r\n").append(host).append("\r\n").
							append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(COORDINATOR_NAME.length()).append("\r\n").
							append(date).append(new Date()).append("\r\n");
							
							dos.writeUTF(sbglobalabort.toString());
							
							while(!toRemove.isEmpty()) {
								textArea.append("removed these participant now: "+toRemove.get(0));
								userNames.remove(toRemove.get(0));
								votes.remove(toRemove.remove(0));
							}
							toRemove.clear();
							
							for (String client : votes.keySet()) {
								votes.put(client, "");
							}
						}
						/*Commit message by participant*/
						else if(msgin.contains("COMMIT")){
							
							data = msgin.split(":")[1];
							votes.put(data, "COMMIT");
							textArea.append("Commit Vote by client: "+data+"\n");
							
							int counter = 0;
							for (String key : votes.keySet()) {
								if(votes.get(key).equals("COMMIT")){
									counter++;
								}else {
									break;
								}
							}
							/*check to see if all participants have voted*/
							if(counter==(votes.size())){
								
								t.cancel();
								t.purge();

								textArea.append("All participants voted to commit..so initiating global commit.\n");
								state = commit;
								
								/*String builder object to encode the message in Http Request format*/
								StringBuilder sbglobalcom = new StringBuilder();
								
								sbglobalcom.append("POST /").append("GLOBAL_COMMIT:"+COORDINATOR_NAME).append("/ HTTP/1.1\r\n").append(host).append("\r\n").
								append(userAgent).append("\r\n").append(contentType).append("\r\n").append(contentlength).append(COORDINATOR_NAME.length()).append("\r\n").
								append(date).append(new Date()).append("\r\n");
								
								/*Pausing this thread to function to test coordinator crash scenario.*/
								Thread.sleep(2000);
								
								dos.writeUTF(sbglobalcom.toString());
								
								while(!toRemove.isEmpty()) {
									textArea.append("removed these participant now: "+toRemove.get(0));
									userNames.remove(toRemove.get(0));
									votes.remove(toRemove.remove(0));
								}
								toRemove.clear();

								for (String client : votes.keySet()) {
									votes.put(client, "");
								}
								
							}
						} 
						
						/*Logic for coordinator to know the current participants list in case it resumes after crashing*/
						else if(msgin.contains("USER_LIST")) {
							
							data = msgin.split(":")[1];
							String [] userss = data.split(",");
							String cname = "";
							
							for(int i=0;i<userss.length;i++){
								cname = userss[i];
								
								if(cname.contains("[")||cname.contains(" ")){
									cname = cname.replace(cname.substring(0, 1), "") ;
								}
								if(cname.contains("]")) {
									cname = cname.replace(cname.substring(cname.length()-1), "") ;
								}
								
								/*adding participant's to array list to keep track
*/								userNames.add(cname);
								/*initially keeping all votes as empty as transaction hasn't begun yet
*/								votes.put(cname, "");
							}
						}
						else {
							/*this participant has been disconnected so it will be removed from coordinator's list
							after the current transaction*/
							data = msgin.split(":")[1];
							
							if(!(state==waiting)) {
								userNames.remove(data);
								votes.remove(data);
							}
							else {
								toRemove.add(data);
							}
						}
						
					}
				}
					
			} 
			catch (InterruptedException ie) {}
			catch (IOException e) {}
			
			} //run method ends here
		} //nested class ends here
}
