import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
class CreateServer extends Thread{
    @Override
    public void run() {
        while (true){
            try {
                Socket socP2P = Peer.serverSocket.accept();
                HandlerConversation handlerConversation = new HandlerConversation(socP2P);
                handlerConversation.start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
class HandlerConversation extends Thread{
    Socket socP2P;
    BufferedReader in;
    PrintWriter out;
    DataOutputStream dataOutputStream = null;
    DataInputStream dataInputStream = null;
    HandlerConversation(Socket socP2P){
        this.socP2P = socP2P;
    }
    private void receiveFile(String fileName){
        try{
            int bytes;
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            long size = dataInputStream.readLong();
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0,(int)Math.min(buffer.length, size)))!= -1){
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;
            }
            System.out.println("File is Received");
            fileOutputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void ReceiveFileMain(String fileName){
        try{
            String fileType = fileName.substring(fileName.indexOf("."));
            fileName = fileName.substring(0,fileName.indexOf("."));
            dataInputStream = new DataInputStream(socP2P.getInputStream());
            dataOutputStream = new DataOutputStream(socP2P.getOutputStream());
            File f = new File(Peer.index2+File.separator+fileName+fileType);
            int count = 1;
            String s = fileName;
            while(f.exists()) {
                s = fileName;
                s += "(" + Integer.toString(count) + ")";
                f = new File(Peer.index2 + File.separator + s + fileType);
                count++;
            }
            receiveFile(Peer.index2+File.separator+s+fileType);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        try{
            in = new BufferedReader(new InputStreamReader(socP2P.getInputStream()));
            out = new PrintWriter(socP2P.getOutputStream(), true);
            while(true){
                try {
                    String message = in.readLine();
                    try {
                        if (message.startsWith("SENDFILE")) {
                            ReceiveFileMain(message.substring(8));
                            continue;
                        }
                        if (message.substring(0, message.indexOf(":")).equals(Peer.index1)) {
                            Peer.chatArea.append(message.substring(message.indexOf(":") + 1) + "\n");
                        }else{
                            String temp = message.substring(0,message.indexOf(":"));
                            if(Peer.hashMap.containsKey(temp)){
                                Peer.hashMap.put(temp,Peer.hashMap.get(temp)+1);
                            }else{
                                Peer.hashMap.put(temp,1);
                            }
                            Peer.out.println("UNSEENMESSAGE"+Peer.index2+":"+message.substring(0,message.indexOf(":")));
                        }
                    } catch (NullPointerException e) {
                        break;
                    }
                }catch(SocketException se){
                    socP2P.close();
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
public class Peer {
    static String ipAddress;
    static Socket soc;
    static ServerSocket serverSocket;
    static Socket socP2P;
    static String index1 = "-1";
    static String index2 = "-1";
    static JFrame chatWindow = new JFrame("Chat Application");
    static JTextArea listUsers = new JTextArea(5, 10);
    static JTextArea listOnline = new JTextArea(5, 10);
    static JTextArea chatArea = new JTextArea(22, 40);
    static JTextField textField = new JTextField(40);
    static JTextField fileField = new JTextField(40);
    static JTextField userField = new JTextField(20);
    static JLabel blankLabel = new JLabel("           ");
    static JButton sendButton = new JButton("Send");
    static JButton sendFile = new JButton("Send File");
    static JButton browseFile = new JButton("Browse File");
    static JButton oKButton = new JButton("OK");
    static BufferedReader in;
    static BufferedReader inP2P;
    static PrintWriter out;
    static PrintWriter outP2P;
    static JLabel nameLabel = new JLabel("         ");
    static JLabel userChatLabel = new JLabel("      ");
    static HashMap<String,Integer> hashMap = new HashMap<String,Integer>();
    Peer() {
        chatWindow.setLayout(new FlowLayout());
        chatWindow.add(blankLabel);
        chatWindow.add(nameLabel);
        chatWindow.add(new JScrollPane(listUsers));
        chatWindow.add(new JScrollPane(listOnline));
        chatWindow.add(new JScrollPane(chatArea));
        chatWindow.add(blankLabel);
        chatWindow.add(userField);
        chatWindow.add(oKButton);
        chatWindow.add(userChatLabel);
        chatWindow.add(textField);
        chatWindow.add(sendButton);
        chatWindow.add(fileField);
        chatWindow.add(sendFile);
        chatWindow.add(browseFile);

        chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatWindow.setSize(500, 700);
        chatWindow.setVisible(true);
        textField.setEditable(false);
        listUsers.setEditable(false);
        listOnline.setEditable(false);
        chatArea.setEditable(false);
        fileField.setEditable(true);

        sendButton.addActionListener(new Listener());
        textField.addActionListener(new Listener());
        oKButton.addActionListener(new ConnectListener());
        userField.addActionListener(new ConnectListener());
        sendFile.addActionListener(new SendFileListener());
        fileField.addActionListener(new SendFileListener());
        browseFile.addActionListener(new BrowseFileListener());
    }
    void startChat() throws Exception{
        ipAddress = JOptionPane.showInputDialog(chatWindow,"Enter IP Address:","IP Address Required",JOptionPane.PLAIN_MESSAGE);
        soc = new Socket(ipAddress, 100);
        in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        out = new PrintWriter(soc.getOutputStream(), true);
        String address="0";
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            address = socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true){
            String str = in.readLine();
            if(str.equals("NAMEREQUIRED")){
                String name = JOptionPane.showInputDialog(chatWindow, "Enter a name:","Name Required",JOptionPane.PLAIN_MESSAGE);
                out.println(address+":"+name);
            }
            else if (str.equals("NAMEALREADYEXISTS")){
                String name = JOptionPane.showInputDialog(chatWindow,"Enter another name:","Name Already Exits",JOptionPane.WARNING_MESSAGE);
                out.println(address+":"+name);
            }
            else if (str.startsWith("NAMEACCEPTED")){
                index2 = str.substring(12,str.indexOf(":"));
                File f = new File(index2);
                if(!f.exists()){
                    f.mkdir();
                }
                serverSocket = new ServerSocket(Integer.parseInt(str.substring(12,str.indexOf(":")))+1);
                CreateServer createServer = new CreateServer();
                createServer.start();
                nameLabel.setText("You login as " + str.substring(str.indexOf(":")+1));
            } else if (str.equals("DELETELISTUSER")) {
                listUsers.setText("List Friend: " + "\n");
                listOnline.setText("Online:" + "\n");
            }else if(str.equals("DELETELISTONLINE")){
                listOnline.setText("Online: "+"\n");
            }else if (str.startsWith("UPDATELISTUSERS")) {
                String index = str.substring(15,str.indexOf(":"));
                if(hashMap.get(index) !=null && hashMap.get(index)!=0){
                    listUsers.append(str.substring(str.indexOf(":")+1)+"("+hashMap.get(index)+")" + "\n");
                }else{
                    listUsers.append(str.substring(str.indexOf(":")+1) + "\n");
                }
                listOnline.append(str.substring(str.indexOf(":")+1) + "\n");
            }else if (str.startsWith("INDEX")) {
                index1 = str.substring(5, str.indexOf("+"));
                index2 = str.substring(str.indexOf("+") + 1,str.indexOf("IP"));
                String IPAddress = str.substring(str.indexOf("IP")+2);
                if(Peer.hashMap.get(index1) != null && Peer.hashMap.get(index1) != 0){
                    Peer.hashMap.put(index1,0);
                }
                if(Peer.socP2P != null){
                    Peer.socP2P.close();
                }
                Peer.socP2P = new Socket(IPAddress,Integer.parseInt(Peer.index1)+1);
                Peer.inP2P = new BufferedReader(new InputStreamReader(Peer.socP2P.getInputStream()));
                Peer.outP2P = new PrintWriter(Peer.socP2P.getOutputStream(), true);
            }else if(str.startsWith("UPDATECHATAREA")){
                Peer.chatArea.append(str.substring(14)+"\n");
            }
            else if(str.startsWith("UPDATELISTONLINE")){
                listOnline.append(str.substring(16)+"\n");
            }else if(str.startsWith("RECONNECT")){
                if(index1.equals(str.substring(9,str.indexOf("IP")))){
                    Peer.socP2P = new Socket(str.substring(str.indexOf("IP")+2), Integer.parseInt(Peer.index1)+1);
                    Peer.inP2P = new BufferedReader(new InputStreamReader(Peer.socP2P.getInputStream()));
                    Peer.outP2P = new PrintWriter(Peer.socP2P.getOutputStream(), true);
                }
            }else if(str.startsWith("REPLYUNSEENMESSAGE")){
                String name = str.substring(str.indexOf(":")+1);
                String listUsers = Peer.listUsers.getText();
                int index = listUsers.indexOf(name);
                listUsers = listUsers.substring(0,index+name.length())
                        +"("+Integer.toString(Peer.hashMap.get(str.substring(18,str.indexOf(":"))))+")"
                        +listUsers.substring(listUsers.indexOf("\n",listUsers.indexOf(name)));
                Peer.listUsers.setText(listUsers);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Peer client = new Peer();
        client.startChat();
    }
}
class SendFileListener implements ActionListener{
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static void sendFileMain(){
        try {
            dataInputStream = new DataInputStream(Peer.socP2P.getInputStream());
            dataOutputStream = new DataOutputStream(Peer.socP2P.getOutputStream());
            System.out.println("File Sended");
            String path = Peer.fileField.getText();
            sendFile(path);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void sendFile(String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream= new FileInputStream(file);
        dataOutputStream.writeLong(file.length());
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer))!= -1) {
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        String fileName = Peer.fileField.getText();
        for(int i = fileName.length()-1;i>=0;i--){
            if(fileName.charAt(i) == '\\' || fileName.charAt(i)=='/'){
                fileName = fileName.substring(i+1);
                break;
            }
        }
        Peer.outP2P.println("SENDFILE"+fileName);
        sendFileMain();
        Peer.fileField.setText("");
    }
}
class ConnectListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        try{
            Peer.chatArea.setText("");
            String name = Peer.userField.getText();
            String listUsers = Peer.listUsers.getText();
            if(!listUsers.contains(name)||name.equals(Peer.nameLabel.getText().substring(13))){
                Peer.index1 = "-1";
                Peer.userField.setText("");
                Peer.textField.setEditable(false);
                if(name.equals(Peer.nameLabel.getText().substring(13))){
                    Peer.userChatLabel.setText("Cannot chat with yourself");
                    return;
                }
                Peer.userChatLabel.setText(name + " is not in friend list");
                return;
            }
            Peer.out.println("ChooseUser" + name + "+" + Peer.nameLabel.getText().substring(13));
            Peer.userChatLabel.setText("You are chatting with " + Peer.userField.getText());
            Peer.userField.setText("");
            Peer.textField.setEditable(true);
            int index = listUsers.indexOf(name);
            listUsers = listUsers.substring(0,index+name.length())
                    +listUsers.substring(listUsers.indexOf("\n",listUsers.indexOf(name)));
            Peer.listUsers.setText(listUsers);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
}

class BrowseFileListener implements ActionListener{
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        int temp = fc.showSaveDialog(null);
        if(temp == JFileChooser.APPROVE_OPTION){
            String file = fc.getSelectedFile().getAbsolutePath();
            Peer.fileField.setText(file);
        }
    }
}

class Listener implements ActionListener{
    @Override
    public void actionPerformed(ActionEvent e) {
        String message = Peer.index2+":"+ Peer.nameLabel.getText().substring(13)+": "+Peer.textField.getText();
        Peer.chatArea.append(message.substring(message.indexOf(":")+1)+"\n");
        Peer.outP2P.println(message);
        Peer.out.println(Peer.index1 +","+ Peer.index2+":" + Peer.nameLabel.getText().substring(13)+": "+Peer.textField.getText());
        Peer.textField.setText("");
    }
}