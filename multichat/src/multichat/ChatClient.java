package multichat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChatClient extends JFrame {

  JPanel nickNamePanel;
  JButton nickName_btn;
  JTextArea chat_ta;
  JTextField input_tf;
  JTextField memberNo_tf;
  JList<String> member_li;
  DefaultListModel<String> model = new DefaultListModel<>();

  Socket socket;
  String data;
  Thread thread;
  OutputStream os;
  BufferedOutputStream bos;
  InputStream is;
  BufferedInputStream bis;

  Map<String, DirectMessage> dmMap = new Hashtable<>();

  String name;
  int whisper = 0;
  int quitPressed = 0;
  int run = 0;

  ChatClient() {
    super("멀티채팅");
    setView();

    setSize(500, 500);
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frame = getSize();
    int x = (int) (screen.getWidth() / 2 - frame.getWidth() / 2);
    int y = (int) (screen.getHeight() / 2 - frame.getHeight() / 2);
    setLocation(x, y);
    setVisible(true);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  private void setView() {
    // 중앙에 붙일 패널 만들기: 닉네임, 대화창, 대화내용
    JPanel centerPanel = new JPanel(new BorderLayout());

    // 닉네임 패널 만들기: 중앙 패널의 위쪽(North)
    nickNamePanel = new JPanel(new BorderLayout());
    nickNamePanel.setBorder(new TitledBorder("닉네임"));
    JTextField nickName_tf = new JTextField(10);
    nickName_btn = new JButton("대화 시작");
    nickName_btn.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (nickName_tf.getText().equals("")) {
            JOptionPane.showMessageDialog(centerPanel, "닉네임을 입력하세요.");
            return;
          }
          if (run == 0) {
            thread =
              new Thread() {
                @Override
                public void run() {
                  try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress("localhost", 5001));
                    os = socket.getOutputStream();
                    bos = new BufferedOutputStream(os);
                    is = socket.getInputStream();
                    bis = new BufferedInputStream(is);
                    name = nickName_tf.getText();
                    send(name);
                    quitPressed = 0;
                    chat_ta.append("=== 대화방에 입장하셨습니다. ===\n");
                    chat_ta.setCaretPosition(chat_ta.getDocument().getLength());
                    receive();
                  } catch (IOException e) {
                    nickName_btn.setText("대화 시작");
                    e.printStackTrace();
                    try {
                      socket.close();
                    } catch (IOException e1) {
                      e1.printStackTrace();
                    }
                  }
                }
              };
            thread.start();
            run = 1;
            nickName_btn.setText("닉네임 변경");
          } else if (run == 1) {
            if (!nickName_tf.getText().equals(name)) {
              name = nickName_tf.getText();
              String changedNickname = "changeNickName:!:";
              StringBuffer sb = new StringBuffer(changedNickname);
              sb.append(name);
              send(sb.toString());
            }
          }
        }
      }
    );
    nickNamePanel.add("Center", nickName_tf);
    nickNamePanel.add("East", nickName_btn);
    centerPanel.add("North", nickNamePanel);

    // 대화창 패널 만들기: 중앙 패널의 가운데(Center)
    JPanel chatPanel = new JPanel(new BorderLayout());
    chatPanel.setBorder(new TitledBorder("대화창"));
    chat_ta = new JTextArea();
    chat_ta.setEditable(false);
    JScrollPane chat_jsp = new JScrollPane(chat_ta);
    chatPanel.add("Center", chat_jsp);
    centerPanel.add("Center", chatPanel);

    // 대화내용 패널 만들기: 중앙 패널의 아래쪽(South)
    JPanel inputPanel = new JPanel(new BorderLayout());
    inputPanel.setBorder(new TitledBorder("대화내용"));
    input_tf = new JTextField(10);
    input_tf.addKeyListener(
      new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyReleased(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            data = input_tf.getText();
            send(data);
            input_tf.setText("");
          }
        }

        @Override
        public void keyPressed(KeyEvent e) {}
      }
    );
    JButton input_btn = new JButton("보내기");
    input_btn.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          data = input_tf.getText();
          send(data);
          input_tf.setText("");
        }
      }
    );
    inputPanel.add("Center", input_tf);
    inputPanel.add("East", input_btn);
    centerPanel.add("South", inputPanel);

    add("Center", centerPanel);

    // 오른쪽 패널 만들기
    JPanel rightPanel = new JPanel(new BorderLayout());
    add("East", rightPanel);

    // 인원표시 패널 만들기
    JPanel memberPanel = new JPanel(new BorderLayout());
    memberPanel.setBorder(new TitledBorder("인원표시"));
    rightPanel.add("Center", memberPanel);

    // 총인원 패널 만들기
    JPanel totalMemberPanel = new JPanel(new FlowLayout());
    totalMemberPanel.setBorder(new TitledBorder("총인원"));
    memberNo_tf = new JTextField(5);
    memberNo_tf.setHorizontalAlignment(JTextField.CENTER);
    memberNo_tf.setText("0");
    memberNo_tf.setEditable(false);
    JLabel memberNo_lb = new JLabel("명");
    totalMemberPanel.add(memberNo_tf);
    totalMemberPanel.add(memberNo_lb);
    memberPanel.add("North", totalMemberPanel);

    member_li = new JList<>(model);
    model.addElement("** Room Member **");

    JScrollPane member_jsp = new JScrollPane(member_li);
    member_jsp.setBorder(new TitledBorder("채팅멤버"));
    memberPanel.add("Center", member_jsp);

    // 옵션 패널 만들기
    JPanel optionPanel = new JPanel(new GridLayout(3, 1));
    optionPanel.setBorder(new TitledBorder("옵션"));
    rightPanel.add("South", optionPanel);

    // 일대일 대화 버튼 만들기
    JButton dmButton = new JButton("1:1 대화");
    dmButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          send("socketRequest:!:" + String.valueOf(member_li.getSelectedIndex()));
        }
      }
    );

    // 종료 버튼 만들기
    JButton quitButton = new JButton("종료");
    quitButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (socket != null) {
            if (!socket.isClosed()) {
              try {
                socket.close();
                quitPressed = 1;
                chat_ta.append("=== 대화방에서 나가셨습니다. ===\n");
              } catch (IOException e1) {
                e1.printStackTrace();
              }
            }
          }
          run = 0;
          nickName_btn.setText("대화 시작");
        }
      }
    );

    optionPanel.add(dmButton);
    optionPanel.add(new JLabel(""));
    optionPanel.add(quitButton);
  }

  public static void main(String[] args) {
    new ChatClient();
  }

  void receive() throws IOException {
    while (true) {
      try {
        byte[] readBytes = new byte[200];

        int readByteNo = bis.read(readBytes);
        if (readByteNo == -1) {
          new IOException();
        }
        data = new String(readBytes, 0, readByteNo, "UTF-8");

        if (data.startsWith("clientNames:!:")) {
          data = data.substring(14);
          String[] clientNames = data.split(":!:");
          setMemberList(clientNames);
          continue;
        }

        if (data.startsWith("socketData:!:")) {
          data = data.substring(13);
          String[] socketData = data.split(":!:");
          if (dmMap.containsKey(socketData[0])) {
            dmMap.get(socketData[0]).setVisible(true);
            continue;
          }
          dmMap.put(socketData[0], new DirectMessage(socketData[0], socketData[1], getLocation()));
          continue;
        }

        if (data.startsWith("whisper:!:")) {
          data = data.substring(10);
          String[] whisperData = data.split(":!:");
          if (dmMap.containsKey(whisperData[0])) {
            dmMap.get(whisperData[0]).setVisible(true);
          } else {
            dmMap.put(
              whisperData[0],
              new DirectMessage(whisperData[0], whisperData[1], getLocation())
            );
          }
          DirectMessage dm = dmMap.get(whisperData[0]);
          if (whisperData[0].equals(dm.socketData) && !(whisperData[1].equals(dm.name))) { // 닉네임이 바뀔 경우를 대비 타이틀 변경
            dm.setTitle(whisperData[1] + "님과 1:1 대화");
          }
          dm.chatTa.append(whisperData[2] + ": " + whisperData[3] + "\n");
        	  dm.chatTa.setCaretPosition(dm.chatTa.getDocument().getLength());
          continue;
        }

        chat_ta.append(data + "\n");
        chat_ta.setCaretPosition(chat_ta.getDocument().getLength());
      } catch (IOException e) {
        if (quitPressed == 1) {
          quitPressed = 0;
          break;
        }
        chat_ta.append("receive: 서버 통신 안 됨\n");
        chat_ta.setCaretPosition(chat_ta.getDocument().getLength());
        socket.close();
        run = 0;
        nickName_btn.setText("대화 시작");
        break;
      }
    }
  }

  void send(String data) {
    Thread sendThread = new Thread() {
      @Override
      public void run() {
        try {
          if (!data.equals("")) {
            byte[] dataBytes = new byte[200];
            dataBytes = data.getBytes("utf-8");
            bos.write(dataBytes);
            bos.flush();
          }
        } catch (IOException e) {
          chat_ta.append("서버 통신 안 됨\n");
          chat_ta.setCaretPosition(chat_ta.getDocument().getLength());
          try {
            socket.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          run = 0;
          nickName_btn.setText("대화 시작");
        }
      }
    };
    sendThread.start();
  }

  void setMemberList(String[] clientNames) {
    model.removeAllElements();
    model.addElement("** Room Member **");
    for (String name : clientNames) {
      model.addElement(name);
    }
    memberNo_tf.setText(String.valueOf(clientNames.length));
  }

  /**
   * DirectMessage
   */
  public class DirectMessage extends JFrame {

    String socketData;
    String name;

    JTextField whisperInputTf;
    JTextArea chatTa;

    DirectMessage(String socketData, String name, Point location) {
      super(name + "님과 1:1 대화");
      this.socketData = socketData;
      this.name = name;

      setDMView();

      setSize(400, 500);
      setLocation(location);
      setVisible(true);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void setDMView() {
      setLayout(new BorderLayout());

      JPanel chatPanel = new JPanel();
      chatPanel.setLayout(new BorderLayout());
      chatPanel.setBorder(new TitledBorder("대화창"));
      chatTa = new JTextArea();
      chatTa.setEditable(false);
      JScrollPane chatJsp = new JScrollPane(chatTa);
      chatPanel.add("Center", chatJsp);
      add("Center", chatPanel);

      JPanel inputPanel = new JPanel(new BorderLayout());
      inputPanel.setBorder(new TitledBorder("대화내용"));
      whisperInputTf = new JTextField();
      whisperInputTf.addKeyListener(
        new KeyListener() {
          @Override
          public void keyTyped(KeyEvent e) {}

          @Override
          public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              data = whisperInputTf.getText();
              send("whisper:!:" + socketData + ":!:" + data);
              whisperInputTf.setText("");
            }
          }

          @Override
          public void keyPressed(KeyEvent e) {}
        }
      );
      JButton inputBtn = new JButton("보내기");
      inputBtn.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            data = whisperInputTf.getText();
            if (!data.equals("")) {
              send("whisper:!:" + socketData + ":!:" + data);
              whisperInputTf.setText("");
            }
          }
        }
      );
      inputPanel.add("Center", whisperInputTf);
      inputPanel.add("East", inputBtn);
      add("South", inputPanel);

      setVisible(true);
    }
  }
}
