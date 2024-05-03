import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket[] playerSockets = new Socket[2];
    private PrintWriter[] writers = new PrintWriter[2];
    private BufferedReader[] readers = new BufferedReader[2];
    private int[] scores = {0, 0};
    private int currentPlayerIndex = 0;
    private boolean gameRunning = true;
    private int targetScore = 5;
    Random random = new Random();
    private String[] operators = {"+","-","/","x"};

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started. Waiting for players...");

            for (int i = 0; i < 2; i++) {
                playerSockets[i] = serverSocket.accept();
                System.out.println("Player " + (i + 1) + " connected.");
                writers[i] = new PrintWriter(playerSockets[i].getOutputStream(), true);
                readers[i] = new BufferedReader(new InputStreamReader(playerSockets[i].getInputStream()));
            }

            playGame();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playGame() throws IOException {
        while (gameRunning) {
            String question = generateQuestion();
            broadcast("Question: " + question);
    
            AtomicReference<AnswerRecord> firstCorrectAnswer = new AtomicReference<>(new AnswerRecord(-1, Long.MAX_VALUE));
            CountDownLatch latch = new CountDownLatch(2); // For waiting both threads
            int[] localScores = {0, 0}; // Local scores to handle score adjustments
    
            // Create a thread for each player to handle answers
            for (int i = 0; i < 2; i++) {
                final int playerIndex = i;
                new Thread(() -> {
                    try {
                        long timestamp = System.currentTimeMillis();
                        String answerStr = readers[playerIndex].readLine();
                        int answer = Integer.parseInt(answerStr);
                        boolean isCorrect = evaluateSingleAnswer(question, answer);
    
                        if (isCorrect) {
                            // Update if this answer is earlier than the current earliest
                            firstCorrectAnswer.getAndUpdate(prev -> {
                                if (timestamp < prev.timestamp) {
                                    localScores[playerIndex] = 1; // Correct answer, potential point
                                    return new AnswerRecord(playerIndex, timestamp);
                                } else {
                                    return prev;
                                }
                            });
                        } else {
                            localScores[playerIndex] = -1; // Incorrect answer, deduct point
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    latch.countDown(); // Signal that this player has answered
                }).start();
            }
    
            try {
                latch.await(); // Wait for both answers
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
    
            AnswerRecord result = firstCorrectAnswer.get();
            // Apply score changes
            for (int i = 0; i < 2; i++) {
                scores[i] += localScores[i];
                // if (localScores[i] != 0) { // Only broadcast if there was a change
                //     broadcast("Player " + (i + 1) + " Score: " + scores[i]);
                // }
                broadcast("Player " + (i + 1) + " Score: " + scores[i]);
            }
    
            if (result.playerIndex != -1 && scores[result.playerIndex] == targetScore) {
                broadcast("Player " + (result.playerIndex + 1) + " wins!");
                gameRunning = false;
            }
    
            // Switch to the other player
            currentPlayerIndex = (currentPlayerIndex + 1) % 2;
        }
    }
    
    
    private boolean evaluateSingleAnswer(String question, int answer) {
        // Implement the logic to check if the provided answer is correct for the given question
        return calculateAnswer(question) == answer;
    }
    
    private int calculateAnswer(String question) {
        // Calculate the correct answer based on the question
        // This method should be the logic extracted from your existing evaluateAnswers method
        String[] args = question.split(" ");
        int number1 = Integer.parseInt(args[0]);
        int number2 = Integer.parseInt(args[2]);
        int answer = 0;
            switch(args[1]){
                case "+":
                    answer = number1 + number2;
                    break;
                case "-":
                    answer = number1 - number2;
                    break;
                case "/":
                    answer = number1 / number2;
                    break;
                case "x":
                    answer = number1 * number2;
                    break;
                default:
                    answer = 0;
                    break;
            }
        return answer;
    }
    
    private static class AnswerRecord {
        public int playerIndex;
        public long timestamp;
    
        public AnswerRecord(int playerIndex, long timestamp) {
            this.playerIndex = playerIndex;
            this.timestamp = timestamp;
        }
    }
    

    private String generateQuestion() {
        // Your code to generate a random question
        int number1 = random.nextInt(90)+10;
        int number2 = random.nextInt(90)+10;
        String operator = operators[random.nextInt(operators.length)];
        if(operator.equals("/")){
            number1 = number2 * random.nextInt(90);
        }
        return String.format("%d %s %d",number1,operator,number2);
    }

    private int evaluateAnswers(String question, int[] answers) {
        // Your code to evaluate the answers and return the index of the correct answer player
        // Implement your logic here
        // For example, if the second player's answer is correct, return 1
        String[] args = question.split(" ");
        int number1 = Integer.parseInt(args[0]);
        int number2 = Integer.parseInt(args[2]);
        int answer = 0;
        switch(args[1]){
            case "+":
				answer = number1 + number2;
				break;
			case "-":
				answer = number1 - number2;
				break;
			case "/":
				answer = number1 / number2;
				break;
			case "x":
				answer = number1 * number2;
				break;
			default:
				answer = 0;
				break;
        }
        int tmp=-1;
        if(answers[0]==answer) tmp = 0;
        else if(answers[1]==answer) tmp = 1;
        return tmp;
    }
    

    private void broadcast(String message) {
        for (PrintWriter writer : writers) {
            writer.println(message);
        }
    }

    public static void main(String[] args) {
        int port = 12345; // Change this to the desired port number
        new GameServer(port);
    }
}
