package multichat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

  public static void main(String[] args) {
    List<Client> clients = new Vector<Client>();

    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    Runnable task = new Runnable() {
      @Override
      public void run() {
        try (ServerSocket serverSocket = new ServerSocket()) {
          serverSocket.bind(new InetSocketAddress("localhost", 5001));

          while (true) {
            try {
              Socket socket = serverSocket.accept();
              System.out.println("[연결 수락: " + socket.getRemoteSocketAddress() + "]");
              Client client = new Client(socket, clients, es);
              clients.add(client);
              System.out.println("[연결 개수: " + clients.size() + "]");
              client.sendClientList();
            } catch (IOException e) {}
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    es.submit(task);
  }
}

class Client {

  Socket socket;
  List<Client> clients;
  ExecutorService es;
  InputStream is;
  BufferedInputStream bis;
  OutputStream os;
  BufferedOutputStream bos;
  String name;

  public Client(Socket socket, List<Client> clients, ExecutorService es) {
    this.socket = socket;
    this.clients = clients;
    this.es = es;

    try {
      is = socket.getInputStream();
      bis = new BufferedInputStream(is);
      os = socket.getOutputStream();
      bos = new BufferedOutputStream(os);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      byte[] readBytes = new byte[200];
      int readByteNo = bis.read(readBytes);
      if (readByteNo == -1) {
        throw new IOException();
      }
      name = new String(readBytes, 0, readByteNo, "UTF-8");
      for (Client client : this.clients) {
        client.send("*** " + this.name + "님이 입장하셨습니다. ***");
      }
    } catch (IOException e) {
      try {
        clients.remove(Client.this);
        System.out.println(
          "[receive: 클라이언트 통신 안 됨: " + socket.getRemoteSocketAddress() + "]"
        );
        socket.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }

    receive();
  }

  void receive() {
    Runnable task = new Runnable() {
      @Override
      public void run() {
        Receive:while (true) {
          try {
            byte[] readBytes = new byte[200];
            int readByteNo = bis.read(readBytes);
            if (readByteNo == -1) {
              throw new IOException();
            }
            System.out.println("[요청 처리: " + socket.getRemoteSocketAddress() + "]");
            String data = new String(readBytes, 0, readByteNo, "UTF-8");

            if (data.startsWith("changeNickName:!:")) {
              String changedNickname = data.substring(17);
              for (Client client : clients) {
                client.send("*** 닉네임 변경: " + name + " -> " + changedNickname + " ***");
              }
              name = changedNickname;
              Thread.sleep(10);
              sendClientList();
              continue;
            }

            if (data.startsWith("socketRequest:!:")) {
              String[] socketData = data.split(":!:");
              int socketIndex = Integer.parseInt(socketData[1]);
              if (socketIndex <= 0) {
                continue;
              }

              Client target = clients.get(socketIndex - 1);
              if (target.socket == socket) {
                continue;
              }

              send("socketData:!:" + target.socket.toString() + ":!:" + target.name);
              continue;
            }

            if (data.startsWith("whisper:!:")) {
              String[] whisperData = data.split(":!:");
              for (Client client : clients) {
                if (client.socket.toString().equals(whisperData[1])) {
                  client.send(
                    "whisper:!:" +
                    socket.toString() +
                    ":!:" +
                    name +
                    ":!:" +
                    name +
                    ":!:" +
                    whisperData[2]
                  );
                  send(
                    "whisper:!:" +
                    whisperData[1] +
                    ":!:" +
                    client.name +
                    ":!:" +
                    name +
                    ":!:" +
                    whisperData[2]
                  );
                  continue Receive;
                }
              }
              send("whisper:!:" + whisperData[1] + ":!:" + " :!: :!:" + "-- 대상 연결 안 됨 --");
              continue;
            }

            for (Client client : clients) {
              client.send(name + ": " + data);
            }
          } catch (IOException e) {
            try {
              System.out.println(
                "[receive: 클라이언트 통신 안 됨: " + socket.getRemoteSocketAddress() + "]"
              );
              clients.remove(Client.this);
              quit();
              try {
                Thread.sleep(10);
              } catch (InterruptedException e2) {
                e2.printStackTrace();
              }
              sendClientList();
              if (socket != null) {
                if (!socket.isClosed()) {
                  socket.close();
                }
              }
              break;
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };

    es.submit(task);
  }

  void send(String data) {
    try {
      byte[] dataBytes = data.getBytes("UTF-8");
      bos.write(dataBytes);
      bos.flush();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("[send: 클라이언트 통신 안 됨: " + socket.getRemoteSocketAddress() + "]");
      clients.remove(Client.this);
      quit();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e2) {
        e2.printStackTrace();
      }
      sendClientList();
      try {
        if (socket != null) {
          if (!socket.isClosed()) {
            socket.close();
          }
        }
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

  void sendClientList() {
    String clientNames = "clientNames:!:";
    StringBuffer sb = new StringBuffer(clientNames);
    for (Client cli : clients) {
      sb.append(cli.name + ":!:");
    }
    clientNames = sb.toString();
    for (Client cli : clients) {
      cli.send(clientNames);
    }
  }

  void quit() {
    for (Client client : clients) {
      client.send("*** " + name + "님이 나가셨습니다. ***");
    }
  }
}
