package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class Client {

  private final Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private String username;
  public String onlineUsers;
  public HashMap<String, ArrayList<String>>group_members;
  //public HashMap<String, ObservableList<Message>>chat_history;



  public Client(String username,Socket socket) throws IOException {
    this.socket = socket;
    this.username = username;
    this.group_members = new HashMap<>();
    //this.chat_history = new HashMap<>();
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out = new PrintWriter(socket.getOutputStream(),true);
    System.out.println("New Client "+ username +" created!");
  }

  public void receiveMessageFromServer() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String  messageFromServer;
          while (socket.isConnected()) {
            if((messageFromServer = in.readLine()) != null){
              String from = messageFromServer.split("::")[0];
              String to = messageFromServer.split("::")[1];
              String data = messageFromServer.split("::")[2];
              System.out.println("I got: " + data);
              if(from.equals("server")){
                if(to.equals("/list")) {
                  onlineUsers = data;
                }else if(to.equals("/createGroupTalk")){
                  createGroupTalk(data);
                }
              }else {
                if(data.equals("/createPrivateTalk")){
                  createPrivateTalk(from);
                }else {
                  //addChatHistory(from,new Message(1L, from,to,data));
                  Controller.upDateHistory(new Message(1L, from,to,data));
                }
              }
            }

          }
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error on recv from server");
          //closeAll(socket, bufferedReader,bufferedWriter);
        }

      }
    }).start();
  }

  public void sendMessageToServer(String messageToServer) throws IOException {
    out.println(messageToServer);
    if(!messageToServer.split("::")[1].equals("server") && !messageToServer.split("::")[2].startsWith("/")){
      Controller.upDateHistory(new Message(1L, messageToServer.split("::")[0],
          messageToServer.split("::")[1],messageToServer.split("::")[2]));
    }
  }
  public String getOnlineUsers(){
    return this.onlineUsers;
  }
  /*public HashMap<String, ObservableList<Message>> getChat_history(){
    return chat_history;
  }*/
/*  public HashMap<String, ArrayList<String>> getGroup_members(){
    return group_members;
  }*/

  /*public void addChatHistory(String groupName, Message message){
    if(chat_history.containsKey(groupName)){
      chat_history.get(groupName).add(message);
    }else {
      ObservableList<Message>history = FXCollections.observableArrayList();
      history.add(message);
      chat_history.put(groupName, history);
    }

  }*/

  public void createGroupTalk(String data){
    String groupName = data.split("@")[0];
    String groupMembers = data.split("@")[1];
    int l = groupMembers.length();
    groupMembers = groupMembers.substring(1,l-1);
    if(!group_members.containsKey(groupName)){
      ArrayList<String> members = new ArrayList<>(Arrays.asList(groupMembers.split(", ")));
      group_members.put(groupName, members);
      //ObservableList<Message> history = FXCollections.observableArrayList();
      //chat_history.put(groupName, history);
      Controller.addCurrentChatters(groupName);
      System.out.println(username+" created a group talk with "+groupName);
    }
  }
  public void createPrivateTalk(String chatterName) throws IOException {
    if(!group_members.containsKey(chatterName)){
      ArrayList<String> members = new ArrayList<>();
      members.add(chatterName);
      group_members.put(chatterName, members);
      //ObservableList<Message> history = FXCollections.observableArrayList();
      //chat_history.put(chatterName, history);
      Controller.addCurrentChatters(chatterName);
      System.out.println(username+" create a private talk with "+chatterName);
    }
  }
  public void notifyServerNewCreateChat(String groupName, String groupMembers){
    String msg = groupName+"@"+groupMembers+"::server::/newCreatedGroup";
    out.println(msg);
  }
  /*public ObservableList<Message> getChatHistory(String groupName){
    return chat_history.get(groupName);
  }*/
  public void closeAll(Socket socket, BufferedReader bufferedReader,
      PrintWriter printWriter) {
    try {
      if (bufferedReader != null) {
        bufferedReader.close();
      }
      if (printWriter != null) {
        printWriter.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
