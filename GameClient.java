import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Scanner scanner;

    public GameClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            scanner = new Scanner(System.in);

            new Thread(this::receiveMessages).start();
            playGame();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playGame() {
        try {
            while (true) {
                String message = reader.readLine();
                if (message != null) {
                    System.out.println(message);
                    if (message.contains("wins!")) {
                        break;
                    }
                }

                // System.out.print("Your answer: ");
                int answer = scanner.nextInt();
                writer.println(answer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "192.168.1.117"; // Change this to the server's IP address or hostname
        int serverPort = 12345; // Change this to the server's port number
        new GameClient(serverAddress, serverPort);
    }
}
