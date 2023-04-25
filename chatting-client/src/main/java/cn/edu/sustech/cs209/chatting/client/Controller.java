package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList;
  @FXML
  ListView<String> chatList;
  @FXML
  TextArea inputArea;
  @FXML
  Label currentUsername;
  @FXML
  Label currentOnlineCnt;
  String toChatter;
  static String username;
  private Client client;
  private static String selectedChat;
  private BufferedReader in;
  private PrintWriter out;
  private ArrayList<String> multi_users;

  private HashMap<String, ArrayList<String>> group_users = new HashMap<>();
  private static ObservableList<String> current_chatters = FXCollections.observableArrayList();

  private static ObservableList<Message> history = FXCollections.observableArrayList();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");

    try {
      Socket clientSocket = new Socket("localhost", 4321);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      out = new PrintWriter(clientSocket.getOutputStream(), true);
    } catch (IOException e) {
      System.out.println("Connect to server failed");
    }

    while (true) {
      Optional<String> input = dialog.showAndWait();
      if (input.isPresent() && !input.get().isEmpty()) {
        username = input.get();
        try {
          out.println(username + "::server::/checkName");
          String receiveMsg = in.readLine();
          if (receiveMsg.split("::")[2].equals("true")) {
            client = new Client(username, new Socket("localhost", 4321));
            client.receiveMessageFromServer();
            client.sendMessageToServer(username + "::" + "server::/setName");
            System.out.println("Client connects to server successfully!");
            break;
          } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Invalid Username");
            alert.setHeaderText("Username duplicated!");
            alert.setContentText("Please input another one next time.");
            alert.showAndWait();
            break;
          }
        } catch (IOException e) {
          System.out.println("Invalid username " + input + ", exiting");
          Platform.exit();
        }
      } else {
        System.out.println("Invalid username " + input + ", exiting");
        Platform.exit();
      }
    }

    chatContentList.setCellFactory(new MessageCellFactory());
    currentUsername.setText("Current User: " + username);
    chatList.setOnMouseClicked(event -> {
      selectedChat = chatList.getSelectionModel().getSelectedItem();
      chatContentList.setItems(history.filtered(message ->
          (selectedChat.contains("(") && message.getSendTo().equals(selectedChat))
              || ((!selectedChat.contains("(")) &&
              ((message.getSendTo().equals(selectedChat) && message.getSentBy().equals(username))
                  || (message.getSendTo().equals(username) && message.getSentBy()
                  .equals(selectedChat))))));
    });
    history.addListener(new ListChangeListener<Message>() {
      @Override
      public void onChanged(Change<? extends Message> change) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            chatContentList.setItems(history.filtered(message ->
                (selectedChat.contains("(") && message.getSendTo().equals(selectedChat))
                    || ((!selectedChat.contains("(")) &&
                    ((message.getSendTo().equals(selectedChat) && message.getSentBy()
                        .equals(username))
                        || (message.getSendTo().equals(username) && message.getSentBy()
                        .equals(selectedChat))))));
          }
        });
      }
    });

    current_chatters.addListener(new ListChangeListener<String>() {
      @Override
      public void onChanged(Change<? extends String> change) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            chatList.setItems(current_chatters);
            chatList.getSelectionModel().select(selectedChat);
            chatContentList.setItems(history.filtered(message ->
                (selectedChat.contains("(") && message.getSendTo().equals(selectedChat))
                    || ((!selectedChat.contains("(")) &&
                    ((message.getSendTo().equals(selectedChat) && message.getSentBy()
                        .equals(username))
                        || (message.getSendTo().equals(username) && message.getSentBy()
                        .equals(selectedChat))))));
          }
        });
      }
    });
  }

  public static void addCurrentChatters(String groupName) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        if (!current_chatters.contains(groupName)) {
          current_chatters.add(groupName);
          selectedChat = groupName;
        } else {
          selectedChat = groupName;
        }
      }
    });
  }

  public static void upDateHistory(Message message) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        history.add(message);
        if (message.getSendTo().equals(username)) {
          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle(username + " Got A New Message");
          alert.setHeaderText("From: " + message.getSentBy());
          alert.setContentText(message.getData());
          alert.showAndWait();
        } else if (!message.getSendTo().equals(username) && !message.getSentBy().equals(username)) {
          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle(username + " Got A New Message");
          alert.setHeaderText("From: " + message.getSendTo());
          alert.setContentText(message.getData());
          alert.showAndWait();
        }
      }
    });
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();

    // FIXME: get the user list from server, the current user's name should be filtered out

    try {
      client.sendMessageToServer(username + "::server::/list");
      Thread.sleep(200);
      String onlineUsers = client.getOnlineUsers();
      int l = onlineUsers.length();
      userSel.getItems().addAll(onlineUsers.substring(1, l - 1).split(", "));
      currentOnlineCnt.setText(userSel.getItems().size() + 1 + "");
    } catch (IOException | InterruptedException e) {
      System.out.println("Get online users for too long! Failed");
    }

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name

    toChatter = user.get();
    if (toChatter != null) {
      System.out.println("here choose " + toChatter);

      if (current_chatters.contains(toChatter)) {
        chatList.getSelectionModel().select(toChatter);
        selectedChat = toChatter;
        chatContentList.setItems(history.filtered(message ->
            (selectedChat.contains("(") && message.getSendTo().equals(selectedChat))
                || ((!selectedChat.contains("(")) &&
                ((message.getSendTo().equals(selectedChat) && message.getSentBy()
                    .equals(username))
                    || (message.getSendTo().equals(username) && message.getSentBy()
                    .equals(selectedChat))))));

      } else {
        try {
          client.createPrivateTalk(toChatter);
          client.sendMessageToServer(username + "::" + toChatter + "::/createPrivateTalk");
        } catch (IOException e) {
          System.out.println("Create private talk failed.");
        }
      }
    }
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {

    Stage stage = new Stage();

    // FIXME: get the user list from server, the current user's name should be filtered out
    ListView<String> multipleChoice = new ListView<>();
    try {
      client.sendMessageToServer(username + "::server::/list");
      Thread.sleep(200);
      String onlineUsers = client.getOnlineUsers();
      int l = onlineUsers.length();

      multipleChoice.getItems().addAll(onlineUsers.substring(1, l - 1).split(", "));
      multipleChoice.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
      currentOnlineCnt.setText(multipleChoice.getItems().size() + 1 + "");
    } catch (IOException | InterruptedException e) {
      System.out.println("Get online users for group talk failed.");
    }

    Button okBtn = new Button("OK");
    multi_users = new ArrayList<>();
    okBtn.setOnAction(e -> {
      multi_users.addAll(multipleChoice.getSelectionModel().getSelectedItems());
      stage.close();
    });
    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(multipleChoice, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name

    System.out.println("here choose " + multi_users);
    if (multi_users.size() > 0) {
      multi_users.add(username);
      List<String> sortedUser = multi_users.stream().sorted().collect(Collectors.toList());
      String groupName = "";
      if (sortedUser.size() >= 3) {
        groupName = sortedUser.get(0) + ", " + sortedUser.get(1) + ", " + sortedUser.get(2) + "("
            + sortedUser.size() + ")";
      } else {
        groupName = sortedUser.get(0) + ", " + sortedUser.get(1) + "(" + sortedUser.size() + ")";
      }
      if (current_chatters.contains(groupName)) {
        chatList.getSelectionModel().select(groupName);
        selectedChat = groupName;
        chatContentList.setItems(history.filtered(message ->
            (selectedChat.contains("(") && message.getSendTo().equals(selectedChat))
                || ((!selectedChat.contains("(")) &&
                ((message.getSendTo().equals(selectedChat) && message.getSentBy()
                    .equals(username))
                    || (message.getSendTo().equals(username) && message.getSentBy()
                    .equals(selectedChat))))));
      } else {
        client.createGroupTalk(groupName + "@" + multi_users);
        client.notifyServerNewCreateChat(groupName, multi_users.toString());
      }
    }


  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML
  public void doSendMessage() throws IOException {
    // TODO
    String text = inputArea.getText();
    if (text != null) {
      chatList.getSelectionModel().select(selectedChat);
      String groupName = selectedChat;
      String inputMsg = inputArea.getText().replace("\n", "`");
      client.sendMessageToServer(username + "::" + groupName + "::" + inputMsg);
      inputArea.clear();
    } else {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle("Empty Message Alert");
      alert.setHeaderText("Empty message is not allowed");
      alert.setContentText("Please input something");
      alert.showAndWait();
    }

  }

  @FXML
  public void addEmojiSmile() {
    inputArea.appendText("\uD83D\uDE04");
  }

  @FXML
  public void addEmojiCry() {
    inputArea.appendText("\uD83D\uDE2D");
  }

  public static void alertServerClose() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Server Closed");
        alert.setHeaderText("Server closed and connections cut.");
        alert.setContentText("Please log out.");
        alert.showAndWait();
      }
    });
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }
}
