package cn.edu.sustech.cs209.chatting.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Server {

  private ServerSocket sererSocket;

  private BufferedReader in;
  private PrintWriter out;
  private static HashMap<String, Socket> user_socket;
  private static HashMap<String, ArrayList<String>> group_members;


  public Server(ServerSocket serverSocket) {
    this.sererSocket = serverSocket;
    user_socket = new HashMap<>();
    group_members = new HashMap<>();
  }

  public static void main(String[] args) {
    try {
      Server server = new Server(new ServerSocket(4321));
      server.startServer();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void startServer() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (!sererSocket.isClosed()) {
            Socket clientSocket = sererSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            Thread t = new Thread(handler);
            t.start();
          }
        } catch (IOException e) {
          System.out.println("Server Down.");
          closeAll(sererSocket, in, out);
        }
      }
    }).start();
  }

  public void deliverMessage(String message) {
    String from = message.split("::")[0];
    String to = message.split("::")[1];
    if (group_members.containsKey(to)) {
      for (String mem : group_members.get(to)) {
        if (!Objects.equals(mem, from)) {
          try {
            if (user_socket.containsKey(mem)) {
              PrintWriter out = new PrintWriter(user_socket.get(mem).getOutputStream(), true);
              out.println(message);
            } else {
              System.out.println("Server cant find socket of " + mem);
            }

          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    } else {
      try {
        if (user_socket.containsKey(to)) {
          PrintWriter out = new PrintWriter(user_socket.get(to).getOutputStream(), true);
          out.println(message);
        } else {
          System.out.println("Server cant find socket of " + to);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  public void handleCommand(String username, Socket socket, String cmd) throws IOException {
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    if (cmd.equals("list")) {
      ArrayList<String> onlineUsers = new ArrayList<>(user_socket.keySet());
      onlineUsers.remove(username);
      String newMsg = "server::/list::" + onlineUsers;
      out.println(newMsg);
    } else if (cmd.equals("checkName")) {
      if (user_socket.containsKey(username)) {
        out.println("server::/checkName::false");
      } else {
        out.println("server::/checkName::true");
      }
    } else if (cmd.equals("setName")) {
      user_socket.put(username, socket);
      System.out.println("Server got " + username + " socket");
    } else if (cmd.equals("newCreatedGroup")) {
      String groupName = username.split("@")[0];
      String groupMembers = username.split("@")[1];
      int l = groupMembers.length();
      groupMembers = groupMembers.substring(1, l - 1);
      if (!group_members.containsKey(groupName)) {
        ArrayList<String> members = new ArrayList<>(Arrays.asList(groupMembers.split(", ")));
        group_members.put(groupName, members);
        notifyAllGroupMembers(username);
        System.out.println("I swear I told them!");
      }
    }
  }

  public void notifyAllGroupMembers(String data) throws IOException {
    String groupName = data.split("@")[0];
    ArrayList<String> groupMembers = group_members.get(groupName);
    for (String user : groupMembers) {
      if (user_socket.containsKey(user)) {
        PrintWriter printWriter = new PrintWriter(user_socket.get(user).getOutputStream(), true);
        printWriter.println("server::/createGroupTalk::" + data);
        printWriter.println(groupName + "::" + groupName + "::" + "GroupMembers: " + groupMembers);
      } else {
        System.out.println("Server cant find socket of " + user);
      }
    }
  }

  public void closeAll(ServerSocket socket, BufferedReader bufferedReader,
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


  class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) throws IOException {
      this.clientSocket = socket;
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
      String receivedMsg;
      try {
        while (true) {
          if ((receivedMsg = in.readLine()) != null) {
            String from = receivedMsg.split("::")[0];
            String to = receivedMsg.split("::")[1];
            String data = receivedMsg.split("::")[2];
            System.out.println("From " + from + " to " + to + " : " + data);
            if (to.equals("server")) {
              if (data.startsWith("/")) {
                handleCommand(from, clientSocket, data.substring(1));
              }
            } else {
              deliverMessage(receivedMsg);
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
