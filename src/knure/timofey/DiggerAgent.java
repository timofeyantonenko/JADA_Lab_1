package knure.timofey;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Objects;

/**
 * Created by timofey on 27.11.16.
 */
public class DiggerAgent extends Agent {

    private static int WORLD_SEARCH_PAUSE = 2000;
    public static int LOOK_RIGHT = 0;
    public static int LOOK_LEFT = 1;
    public static int LOOK_UP = 2;
    public static int LOOK_DOWN = 3;
    public static int MOVE = 4;
    public static int SHOOT_ARROW = 5;
    public static int TAKE_GOLD = 6;
    public static java.util.HashMap<Integer, String> actionCodes = new java.util.HashMap<Integer, String>() {{
        put(LOOK_RIGHT, "right");
        put(LOOK_LEFT, "left");
        put(LOOK_UP, "up");
        put(LOOK_DOWN, "down");
        put(MOVE, "move");
        put(SHOOT_ARROW, "shoot");
        put(TAKE_GOLD, "take");
    }};

    public static String GO_INSIDE = "go_inside";
    public static String WAMPUS_WORLD_TYPE = "wampus-world";
    public static String NAVIGATOR_AGENT_TYPE = "navigator-agent";

    public static String WORLD_DIGGER_CONVERSATION_ID = "digger-world";
    public static String NAVIGATOR_DIGGER_CONVERSATION_ID = "digger-navigator";

    private int arrowCount = 1;
    private AID wampusWorld; //Мир, который нашли
    private AID navigationAgent; //Навигационный агент
    private String currentWorldState = "";

    @Override
    protected void setup() {
        addBehaviour(new WampusWorldFinder());
    }

    private class WampusWorldFinder extends Behaviour {
        private int step = 0;
        @Override
        public void action() {
            if (step == 0){
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WAMPUS_WORLD_TYPE);
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result != null && result.length > 0) {
                        wampusWorld = result[0].getName(); // Возьмем первый попавшийся
                        System.out.println("I found the world!!! Name is: " + wampusWorld);
                        myAgent.addBehaviour(new WampusWorldPerformer());
                        ++step;
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public boolean done() {
            return step == 1;
        }
    }
    private class WampusWorldPerformer extends Behaviour {
        private MessageTemplate mt; //Шаблон для получения ответов

        private int step = 0;
        @Override
        public void action() {
            switch (step) {
                case 0:
                    // Отправляем запросы на поиск Вампус мира
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(wampusWorld);
                    cfp.setContent(GO_INSIDE);
                    cfp.setConversationId(WORLD_DIGGER_CONVERSATION_ID);
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Уникальное значение
                    myAgent.send(cfp);
                    // Подготавливаем шаблон для получения предложения
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(WORLD_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Получаем ответ от мира
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Ответ получен
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            // Это предложение (так как не null)
                            String answer = reply.getContent();
                            System.out.println("Answer from world is: " + answer);
                            currentWorldState = answer;
                            myAgent.addBehaviour(new NavigatorAgentPerformer());
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        @Override
        public boolean done() {
            return step == 2;
        }
    }
    private class NavigatorAgentPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt; // Шаблон для получения ответов
        @Override
        public void action() {
            switch (step) {
                case 0: {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(NAVIGATOR_AGENT_TYPE);
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result != null && result.length > 0) {
                            navigationAgent = result[0].getName(); // Возьмем первый попавшийся
                            System.out.println("I found the navigator!!! Name is: " + navigationAgent);
                            ++step;
                        } else {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
                case 1: {
                    System.out.println("IN STEP 1");
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                    order.addReceiver(navigationAgent);
                    order.setContent(currentWorldState);
                    order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Подготавливаем шаблон чтобы получить ответ на информацию о нашем положении
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 2;
                }
                case 2: {
                    System.out.println("IN step 2");
                    // Получаем руководство к действиям
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        System.out.println("REPLY NOT NULL");
                        // Ответ получен
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String actions = reply.getContent();
                            actions = actions.substring(1, actions.length()-1);
                            String[] instructions = actions.split(", ");
                            if (instructions.length == 1){
                                //takeGold();
                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(SHOOT_ARROW))){

                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(MOVE))){

                            }
                            else {
                                System.out.println("ERROR ACTIONS");
                            }
                            ++step;
                        }
                    }
                    else {
                        block();
                    }
                    break;

                }
            }
        }
        @Override
        public boolean done() {
            return step == 3;
        }
    }


}
