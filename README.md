# multichat
## 개요
Java 공부 도중에 제작했던 멀티 채팅 서버와 클라이언트이다.<br>
Swing을 사용한 GUI로 제작하였다.
<br><br>

## 구현 상세
**ServerSocket**과 **Socket** 클래스를 이용해 실시간 메시지가 가능하도록 구현하였다.<br>
소켓의 연결 대기 동작과 각 클라이언트의 메시지 전송 요청 대기가 스레드 풀 상에서 동시에 진행되도록 했다.
<br><br>

### 서버 구현
<img src="https://user-images.githubusercontent.com/46345154/173520547-69c74c3e-c45f-4f78-9150-cf0bc8e5fed7.png" width="50%">

서버는 GUI가 필요 없기 때문에 콘솔 창에 로그를 출력하는 정도로 구현하였다.

새로운 연결 요청이 들어오는 경우 Client 객체를 생성해 List에 추가하여 관리하도록 했다.
```java
List<Client> clients = new Vector<Client>();

...

Socket socket = serverSocket.accept();
System.out.println("[연결 수락: " + socket.getRemoteSocketAddress() + "]");
Client client = new Client(socket, clients, es);
clients.add(client);
```
Client의 연결이 끊어지게 되면 관리하고 있던 Client 객체를 제거하고, 안전하게 소켓을 close 시키도록 했다.
```java
...

  } catch (IOException e) {
    try {
      System.out.println(
        "[receive: 클라이언트 통신 안 됨: " + socket.getRemoteSocketAddress() + "]"
      );
      clients.remove(Client.this);
      quit();
    
...

void quit() {
  for (Client client : clients) {
    client.send("*** " + name + "님이 나가셨습니다. ***");
  }
}

```
<br><br>
### 클라이언트 구현
Swing을 사용해 GUI로 제작하였다.

<img src="https://user-images.githubusercontent.com/46345154/173728885-096a2d7e-28ae-421c-97b0-4b81f97875fb.png" style="max-width: 50%">

<br><br>

### 구현 상세
- 닉네임을 입력하고 **대화 시작** 버튼을 누르면 서버에 연결된다.
- **대화 시작** 버튼은 **닉네임 변경** 버튼으로 바뀌고, 닉네임을 변경하면 모두에게 알림 메시지가 출력된다.
- 입장과 퇴장시 모두에게 알림 메시지가 출력된다.
- 입장과 퇴장시 총 인원과 멤버 목록을 전송해 갱신하도록 구현하였다.
- 목록에서 멤버를 선택하고 1:1 대화를 누르면 1:1 대화 창이 열리고, 1:1 대화에서 대화한 내용은 해당 창에만 출력된다.
- 다른 멤버에게서 1:1 대화가 걸려온 경우 자동으로 1:1 대화 창이 팝업되어 대화가 가능하다.

<img src="https://user-images.githubusercontent.com/46345154/173732867-51195e7f-6ef5-418e-9ce3-2eedb59e0f5a.gif">
