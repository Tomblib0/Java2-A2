package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


public class Client {

  private final Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private String username;
  public String onlineUsers;
  public HashMap<String, ArrayList<String>> group_members;

  public Client(String username, Socket socket) throws IOException {
    this.socket = socket;
    this.username = username;
    this.group_members = new HashMap<>();
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out = new PrintWriter(socket.getOutputStream(), true);
    System.out.println("New Client " + username + " created!");
  }

  public void receiveMessageFromServer() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String messageFromServer;
          while (socket.isConnected()) {
            if ((messageFromServer = in.readLine()) != null) {
              String from = messageFromServer.split("::")[0];
              String to = messageFromServer.split("::")[1];
              String data = messageFromServer.split("::")[2];
              System.out.println("I got: " + data);
              if (from.equals("server")) {
                if (to.equals("/list")) {
                  onlineUsers = data;
                } else if (to.equals("/createGroupTalk")) {
                  createGroupTalk(data);
                }
              } else {
                if (data.equals("/createPrivateTalk")) {
                  createPrivateTalk(from);
                } else {
                  Controller.upDateHistory(new Message(1L, from, to, data.replace("`", "\n")));
                }
              }
            }

          }
        } catch (Exception e) {
          Controller.alertServerClose();
          System.out.println("Error receiving from server, close socket and I/O");
          closeAll(socket, in, out);
        }

      }
    }).start();
  }

  public void sendMessageToServer(String messageToServer) throws IOException {
    out.println(messageToServer);
    if (!messageToServer.split("::")[1].equals("server") && !messageToServer.split(
        "::")[2].startsWith("/")) {
      Controller.upDateHistory(
          new Message(1L, messageToServer.split("::")[0], messageToServer.split("::")[1],
              messageToServer.split("::")[2].replace("`", "\n")));
    }
  }

  public String getOnlineUsers() {
    return this.onlineUsers;
  }

  public void createGroupTalk(String data) {
    String groupName = data.split("@")[0];
    String groupMembers = data.split("@")[1];
    int l = groupMembers.length();
    groupMembers = groupMembers.substring(1, l - 1);
    if (!group_members.containsKey(groupName)) {
      ArrayList<String> members = new ArrayList<>(Arrays.asList(groupMembers.split(", ")));
      group_members.put(groupName, members);
      Controller.addCurrentChatters(groupName);
      System.out.println(username + " created a group talk with " + groupName);
    }
  }

  public void createPrivateTalk(String chatterName) throws IOException {
    if (!group_members.containsKey(chatterName)) {
      ArrayList<String> members = new ArrayList<>();
      members.add(chatterName);
      group_members.put(chatterName, members);
      Controller.addCurrentChatters(chatterName);
      System.out.println(username + " create a private talk with " + chatterName);
    }
  }

  public void notifyServerNewCreateChat(String groupName, String groupMembers) {
    String msg = groupName + "@" + groupMembers + "::server::/newCreatedGroup";
    out.println(msg);
  }

  public void closeAll(Socket socket, BufferedReader bufferedReader, PrintWriter printWriter) {
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
      System.out.println("Hard to close in closeAll()");
    }
  }

}
