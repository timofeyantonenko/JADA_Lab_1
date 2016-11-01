package knure.timofey;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Created by timofey on 09.10.16.
 */
public class BookBuyerAgent extends Agent {
    private static int REQUEST_REPEAT_TIME = 60000; //каждую минуту делаем запрос

    private String targetBookTitle; // Название необходимой кники

    // Массив агентов, к которым будет делаться запрос на наличие книги.
    private AID[] sellerAgents;

    String nickname = "Peter"; // Имя агента
    AID id = new AID(nickname, AID.ISLOCALNAME); // Идентификатор агента
        @Override
    protected void setup() { // метод, который вызывается при инициализации агента и настраивает его основные параметры

        System.out.println("Hello! Buyer-agent " + getAID().getName() + " is ready."); // чтобы мониторить, что агент создался
        Object [] args = getArguments(); // аргементы, которые были поданы на вход в коммандной строке
        if (args != null && args.length > 0) {
            targetBookTitle = (String)args[0];
            System.out.println("Trying to buy book: " + targetBookTitle);
            addBehaviour(new TickerBehaviour(this, REQUEST_REPEAT_TIME) {
                @Override
                protected void onTick() {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("book-selling");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        }
        else { // если на вход не было подано никаких аргументов (в нашем случае - название книги)
            System.out.println("No book title specified");
            doDelete(); // удаляем агента
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
    }
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // Агент, который предложил лучшее решение
        private int bestPrice;  // Лучшая предложенная цена
        private int repliesCnt = 0; // Счетчик ответов от агентов-продавцов
        private MessageTemplate mt; // Шаблон для получения ответов
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Отправляем запросы ко всем продавцам
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Уникальное значение
                    myAgent.send(cfp);
                    // Подготавливаем шаблон для получения предложения
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Получаем все прдложения/отказы от агентов-продавцов
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Ответ получен
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // Это предложение (так как не null)
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                // Это лучшее предложение (ну или первое в списке:)
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            // Мы получили все все ответы
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Отправляем запрос на покупку к продавцу от которого было лучшее предложение
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Подготавливаем шаблон чтобы получить ответ на заказ покупки
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Получить ответ с запроса на покупку
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Ответ на запрос покупки получен
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+bestPrice);
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested book already sold.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: "+targetBookTitle+" not available for sale");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }
}
