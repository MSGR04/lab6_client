package se.ifmo.core.socket.client;

import se.ifmo.core.collection.ObjectUtil;
import se.ifmo.core.collection.objects.HumanBeing;
import se.ifmo.core.transfer.Request;
import se.ifmo.core.transfer.Response;
import se.ifmo.core.io.IOWorker;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class Client implements AutoCloseable {
    private final static int BUFFER_SIZE = 10000;
    private final ClientConfiguration configuration;

    private final DatagramSocket socket;
    private final IOWorker console;

    private boolean handle = true;

    public static int recursionDepth = 0;
    public static final int MAX_RECURSION_DEPTH = 10;

    private Deque<String> requests = new ArrayDeque<>();

    private Map<String, Integer> requiredElementsForCommand = new HashMap<>();

    public Client(ClientConfiguration configuration, IOWorker console) throws SocketException {
        this.console = console;
        this.socket = new DatagramSocket();
        this.configuration = configuration;
    }

    public void start() throws IOException, ClassNotFoundException {
        console.write("### LAB 6 ###");
        console.write("Для выхода введите 'exit'");
        console.write("Для помощи по командам введите 'help'");
        console.write("Для отчистки консоли введите 'clear'");

        String input = "";
        main: while (handle && (!requests.isEmpty() || (input = console.writeAndRead("~ ")) != null && !input.equals("exit"))) {
            recursionDepth = 0;

            if (!requests.isEmpty()) input = requests.pollLast();

            if (input == null) break;
            if (input.isEmpty() || input.isBlank()) continue;

            if ("clear".equals(input)) {
                console.clear();
                continue;
            }

            if ("exit".equals(input)) {
                handle = false;
                continue;
            }

            StringTokenizer tokenizer = new StringTokenizer(input, " ");
            Request request = new Request();

            String command = tokenizer.nextToken();
            int requiredResources = requiredElementsForCommand.getOrDefault(command, 0);

            while (tokenizer.hasMoreTokens()) request.setText(request.getText() + tokenizer.nextToken() + " ");
            request.setText(request.getText().trim());

            while (requiredResources-- > 0) {
                HumanBeing objectIn;
                if (requests.isEmpty()) objectIn = ObjectUtil.collect(console);
                else objectIn = ObjectUtil.collect(requests);
                if (objectIn == null) continue main;
                request.getResource().put(objectIn);
            }

            request.setCommand(command);

            send(request);
            Response result = receive();

            if (result == null) {
                console.write("Команда вернула пустой ответ");
                continue;
            }

            if (!result.getInboundRequests().isEmpty()) requests.addAll(result.getInboundRequests());
            if (!result.getCommands().isEmpty()) requiredElementsForCommand = result.getCommands();

            console.write(result.getText());
            result.getResource().forEach((id, element) -> console.write("#%d %s",id,element.toString()));
        }

        console.write("Выход из программы...");
    }

    private void send(Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        byte[] sendData = bos.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, configuration.getHost(), configuration.getPort());
        socket.send(sendPacket);
    }

    private Response receive() throws IOException, ClassNotFoundException {
        byte[] recvBuf = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
        socket.receive(receivePacket);

        ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Response) ois.readObject();
    }

    @Override
    public void close() throws Exception {
        if (!socket.isClosed()) socket.close();
    }
}
