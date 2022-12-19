import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatServer {
    static ArrayList<String> userNames = new ArrayList<>();
    static ArrayList<PrintWriter> printWriters = new ArrayList<>();
    static ArrayList<Integer> indexDisconnect = new ArrayList<>();
    static ArrayList<String> IPAddress = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Waiting for clients...");
        ServerSocket serverSocket = new ServerSocket(100);
        while (true){
            Socket soc = serverSocket.accept();
            System.out.println("Connection established");
            ConversationHandler handler = new ConversationHandler(soc);
            handler.start();
        }
    }
}

class ConversationHandler extends Thread{
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    String name;
    ConversationHandler(Socket socket){
        this.socket = socket;
    }
    public void run(){
        try{
            boolean flag = false;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            int count = 0;
            while (true){
                if (count > 0){
                    out.println("NAMEALREADYEXISTS");
                }
                else{
                    out.println("NAMEREQUIRED");
                }
                String str = in.readLine();
                name = str.substring(str.indexOf(":")+1);
                if(ChatServer.userNames.contains(name+":D")){
                    int index = ChatServer.userNames.indexOf(name+":D");
                    ChatServer.userNames.set(index, name);
                    name = ChatServer.userNames.get(index);
                    flag = true;
                    break;
                }
                if (!ChatServer.userNames.contains(name)){
                    if(!ChatServer.userNames.isEmpty()){
                        for(int i = 0; i< ChatServer.userNames.size();i++){
                            try {
                                FileWriter fileWriter = new FileWriter(Integer.toString(i)+Integer.toString(ChatServer.userNames.size())+".txt");
                                fileWriter.close();
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                    ChatServer.IPAddress.add(str.substring(0,str.indexOf(":")));
                    ChatServer.userNames.add(name);
                    break;
                }
                count++;
            }
            if(flag){
                flag = false;
                int index = ChatServer.userNames.indexOf(name);
                ChatServer.printWriters.set(index,out);
                for(PrintWriter pw : ChatServer.printWriters) {
                    if (pw == out) {
                        continue;
                    }
                    pw.println("RECONNECT" + Integer.toString(index)+"IP"+ChatServer.IPAddress.get(index));
                }
            }else{
                ChatServer.printWriters.add(out);
            }
            out.println("NAMEACCEPTED"+Integer.toString(ChatServer.userNames.indexOf(name)) + ":" + name);
            for (PrintWriter writer : ChatServer.printWriters) {
                writer.println("DELETELISTUSER");
            }
            String list = "";
            for (String s : ChatServer.userNames) {
                list = list + "UPDATELISTUSERS"+ChatServer.userNames.indexOf((s))+":"+ s + "\n";
            }
            for (PrintWriter writer : ChatServer.printWriters) {
                writer.println(list);
            }
            while (true)
            {
                try{
                    String message = in.readLine();
                    if (message.startsWith("ChooseUser")){
                        int index = message.indexOf('+');
                        String s1 = message.substring(10, index);
                        String s2 = message.substring(index+1);
                        int index1 = ChatServer.userNames.indexOf(s1);
                        int index2 = ChatServer.userNames.indexOf(s2);
                        out.println("INDEX"+Integer.toString(index1)+"+"+Integer.toString(index2)+"IP"+ChatServer.IPAddress.get(index1));
                        String fileName;
                        if (index1 > index2) {
                            fileName = Integer.toString(index2) + Integer.toString(index1) + ".txt";
                        } else {
                            fileName = Integer.toString(index1) + Integer.toString(index2) + ".txt";
                        }
                        try {
                            File myObj = new File(fileName);
                            Scanner myReader = new Scanner(myObj);
                            StringBuilder data = new StringBuilder();
                            while (myReader.hasNextLine()) {
                                data.append("UPDATECHATAREA").append(myReader.nextLine()).append("\n");
                            }
                            out.println(data.toString());
                            myReader.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }else if(message.startsWith("UNSEENMESSAGE")){
                        ChatServer.printWriters.get(Integer.parseInt(message.substring(13,message.indexOf(":")))).println("REPLYUNSEENMESSAGE"+message.substring(message.indexOf(":")+1)+":"
                                +ChatServer.userNames.get(Integer.parseInt(message.substring(message.indexOf(":")+1))));
                        continue;
                    }
                    int index1 = Integer.parseInt(message.substring(0,message.indexOf(",")));
                    int index2 = Integer.parseInt(message.substring(message.indexOf(",")+1,message.indexOf(":")));
                    String fileName;
                    if(index1 > index2){
                        fileName = Integer.toString(index2)+ Integer.toString(index1)+".txt";
                    }else{
                        fileName = Integer.toString(index1)+ Integer.toString(index2)+".txt";
                    }
                    FileWriter fw = new FileWriter(fileName, true);
                    fw.write(message.substring(message.indexOf(":")+1) + "\n");
                    fw.close();
                }catch (SocketException | NullPointerException e){
                    ChatServer.indexDisconnect.add(ChatServer.printWriters.indexOf(out));
                    String name = ChatServer.userNames.get(ChatServer.printWriters.indexOf(out));
                    ChatServer.userNames.set(ChatServer.printWriters.indexOf(out),name+":D");
                    String listOnline = "";
                    for(String s : ChatServer.userNames){
                        if(!s.contains(":D")){
                            listOnline =listOnline +"UPDATELISTONLINE"+s+"\n";
                        }
                    }
                    for (PrintWriter writer : ChatServer.printWriters) {
                        if(writer == out){
                            continue;
                        }
                        writer.println("DELETELISTONLINE");

                    }
                    for(int i = 0; i < ChatServer.printWriters.size(); i++){
                        if(ChatServer.printWriters.get(i) == out){
                            continue;
                        }
                        ChatServer.printWriters.get(i).println(listOnline);
                    }
                    break;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}