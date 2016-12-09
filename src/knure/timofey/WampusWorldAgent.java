package knure.timofey;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

/**
 * Created by timofey on 27.11.16.
 */
public class WampusWorldAgent extends Agent {

    public static String SERVICE_DESCRIPTION = "WAMPUS-WORLD";

    //Запишем в виде чисел кодовые значения на карте
    private static int START = -1; //сколько строк
//    private static int EMPTY = 0; //пустая команат
    private static int WAMPUS = 1; //комната Вампуса
    private static int PIT = 2; //комната с ямой
    private static int BREEZE = 3; //комната с бризом
    private static int STENCH = 4; //комната со зловонием
    private static int SCREAM = 5; //крик Вампуса
    private static int GOLD = 6; //комната с золотом
    private static int BUMP = 7; //стена
    public static HashMap<Integer, String> roomCodes = new HashMap<Integer, String>() {{
            put(START, NavigatorAgent.START);
//        put(EMPTY, NavigatorAgent.EMPTY);
        put(WAMPUS, NavigatorAgent.WAMPUS);
        put(PIT, NavigatorAgent.PIT);
        put(BREEZE, NavigatorAgent.BREEZE);
        put(STENCH, NavigatorAgent.STENCH);
        put(SCREAM, NavigatorAgent.SCREAM);
        put(GOLD, NavigatorAgent.GOLD);
        put(BUMP, NavigatorAgent.BUMP);
    }};

    // Будем тренироваться на карте из примера
    private static int NUM_OF_ROWS = 4; //сколько строк
    private static int NUM_OF_COLUMNS = 4; //сколько столбцов

    private Room[][] wampusMap;
    private HashMap<AID, Coords> diggers;

    String nickname = "WampusWorld"; // Имя агента
    AID id = new AID(nickname, AID.ISLOCALNAME); // Идентификатор агента

    @Override
    protected void setup() { // метод, который вызывается при инициализации агента и настраивает его основные параметры
        System.out.println("Hello! WampusWorld-agent " + getAID().getName() + " is ready."); // чтобы мониторить, что агент создался
        diggers = new HashMap<>();
        generateMap();
        showMap();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(DiggerAgent.WAMPUS_WORLD_TYPE);
        sd.setName(SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // добавим модели моведения
        addBehaviour(new DiggerConnectPerformer());
        addBehaviour(new DiggerArrowPerformer());
        addBehaviour(new DiggerGoldPerformer());
        addBehaviour(new DiggerMovePerformer());
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        try {
//            String s = br.readLine();
//            System.out.println("Your string is: " + s);
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void generateMap() {
        this.wampusMap = new Room[NUM_OF_ROWS][NUM_OF_COLUMNS];
        this.wampusMap[0][0] = new Room();
        this.wampusMap[0][1] = new Room(BREEZE);
        this.wampusMap[0][2] = new Room(PIT);
        this.wampusMap[0][3] = new Room(BREEZE);
        this.wampusMap[1][0] = new Room(STENCH);
        this.wampusMap[1][3] = new Room(BREEZE);
        this.wampusMap[2][0] = new Room(WAMPUS, STENCH);
        this.wampusMap[2][1] = new Room(BREEZE, STENCH, GOLD);
        this.wampusMap[2][2] = new Room(PIT);
        this.wampusMap[2][3] = new Room(BREEZE);
        this.wampusMap[3][0] = new Room(STENCH);
        this.wampusMap[3][2] = new Room(BREEZE);
        this.wampusMap[3][3] = new Room(PIT);
        for (int i=0; i < this.wampusMap.length; i++){
            for (int j= 0; j < this.wampusMap[i].length; j++){
                if (this.wampusMap[i][j] == null) {
                    this.wampusMap[i][j] = new Room();
                }
            }

        }

    }
    private void showMap(){
        for (int i=0; i < this.wampusMap.length; i++){
            for (int j= 0; j < this.wampusMap[i].length; j++){
                System.out.println("POSITION: " + i + ", " + j + "; MARKERS: " + wampusMap[i][j].events);
            }

        }
    }
    private class DiggerConnectPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение CFP получено. Теперь его нужно обработать.
                String message = msg.getContent();
                if (Objects.equals(message, DiggerAgent.GO_INSIDE)){
                    AID current_digger = msg.getSender();
                    Coords digger_coords = diggers.get(current_digger);
                    if (digger_coords == null){
                        diggers.put(current_digger, new Coords(0, 0));
                    }
                    else {
                        diggers.put(current_digger, new Coords(0, 0));
                    }
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(wampusMap[0][0].events.toString());
                    myAgent.send(reply);
                }
//
            }
            else {
                block();
            }
        }
    }
    private class DiggerArrowPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(DiggerAgent.SHOOT_ARROW);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение CFP получено. Теперь его нужно обработать.

                ACLMessage reply = msg.createReply();
                reply.setPerformative(DiggerAgent.SHOOT_ARROW);

                String message = msg.getContent();
                AID current_digger = msg.getSender();
                Coords digger_coords = diggers.get(current_digger);

                int row = digger_coords.row;
                int column = digger_coords.column;
                String answer = "";
                if (message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_DOWN))){
                    for (int i = 0; i < row; ++i){
                        if (wampusMap[i][column].events.contains(WampusWorldAgent.roomCodes.get(WAMPUS))){
                            answer = NavigatorAgent.SCREAM;
                        }
                    }
                }
                else if(message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_UP))){
                    for (int i = row+1; i < NUM_OF_ROWS; ++i){
                        if (wampusMap[i][column].events.contains(WampusWorldAgent.roomCodes.get(WAMPUS))){
                            answer = NavigatorAgent.SCREAM;
                        }
                    }
                }
                else if(message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_LEFT))){
                    for (int i = 0; i < column; ++i){
                        if (wampusMap[row][i].events.contains(WampusWorldAgent.roomCodes.get(WAMPUS))){
                            answer = NavigatorAgent.SCREAM;
                        }
                    }
                }
                else if (message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_RIGHT))){
                    for (int i = column+1; i < NUM_OF_COLUMNS; ++i){
                        if (wampusMap[row][i].events.contains(WampusWorldAgent.roomCodes.get(WAMPUS))){
                            answer = NavigatorAgent.SCREAM;
                        }
                    }
                }

                reply.setContent(answer);

                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
    private class DiggerMovePerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(DiggerAgent.MOVE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение CFP получено. Теперь его нужно обработать.

                ACLMessage reply = msg.createReply();
                reply.setPerformative(DiggerAgent.MOVE);

                String message = msg.getContent();
                AID current_digger = msg.getSender();
                Coords digger_coords = diggers.get(current_digger);
                System.out.println("World say: Current agent coords: " + digger_coords.row + " | " + digger_coords.column);
                if (digger_coords == null){
                    diggers.put(current_digger, new Coords(0, 0));
                    digger_coords = diggers.get(current_digger);
                }
                int row = digger_coords.row;
                int column = digger_coords.column;
                if (message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_DOWN))){
                    row -= 1;
                }
                else if(message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_UP))){
                    row += 1;
                }
                else if(message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_LEFT))){
                    column -=1;
                }
                else if (message.equals(DiggerAgent.actionCodes.get(DiggerAgent.LOOK_RIGHT))){
                    column += 1;
                }
                if (row > -1 && column > -1 && row < NUM_OF_ROWS && column < NUM_OF_COLUMNS){
                    digger_coords.column = column;
                    digger_coords.row = row;
                    reply.setContent(wampusMap[row][column].events.toString());
                }
                else {
                    reply.setContent(String.valueOf(new ArrayList<String>(){{
                        add(NavigatorAgent.BUMP);
                    }}));
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
    private class DiggerGoldPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(DiggerAgent.TAKE_GOLD);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение CFP получено. Теперь его нужно обработать.
                String message = msg.getContent();
                AID current_digger = msg.getSender();
                Coords digger_coords = diggers.get(current_digger);
                if (digger_coords == null){
                    diggers.put(current_digger, new Coords(0, 0));
                }
                else {
                    if (wampusMap[digger_coords.row][digger_coords.column].events.contains(WampusWorldAgent.roomCodes.get(GOLD))){
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(DiggerAgent.TAKE_GOLD);
                        reply.setContent("GOLD");
                        myAgent.send(reply);
                    }
                }
            }
            else {
                block();
            }
        }
    }
}
class Room {
    ArrayList<String> events = new ArrayList<>();
    Room (int... args){
        for (int i: args){
            events.add(WampusWorldAgent.roomCodes.get(i));
        }
    }
}
class Coords {
    int row = 0;
    int column = 0;
    Coords(int row, int column){
        this.row = row;
        this.column = column;
    }
}
