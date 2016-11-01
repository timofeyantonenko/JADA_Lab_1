package knure.timofey;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;

/**
 * Created by timofey on 09.10.16.
 */
public class BookSellerAgent extends Agent {
    private Hashtable catalogue; // Каталог книг
    private BookSellerGui myGui; // Интерфейс для аганта продавца книг

    @Override
    protected void setup() {
        //инициализируем необхимые переменные агента
        catalogue = new Hashtable();
        myGui = new BookSellerGui(this);
        myGui.showGui();  // отобразим интерфейс

        // Зарегестрируем книго-продажный сервис в желтой странице
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // добавим модели моведения
        addBehaviour(new OfferRequestsServer());

        addBehaviour(new PurchaseOrdersServer());
    }

    @Override
    protected void takeDown() {
        // снимем регистрация с желтых страниц
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Закрываем интерфейс продавца книг
        myGui.dispose();
        // Выведем освобожденное сообщение
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }

    // Функция добавления книги в каталог
    public void updateCatalogue(final String title, final int price){
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                catalogue.put(title, new Integer(price));
            }
        });
    }

    // Поведение которое описывает сервис ответа на запросы о покупке книг
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение CFP получено. Теперь его нужно обработать.
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
                    // Запрашиваемая книга присутствует. Ответим сообщением с указанием цены.
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                }
                else {
                    // Запрашиваемая книга не доступна для продажи.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    // Описание поведения, при котором покупают книга
    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Сообщение с подтверждением покупки получено.
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Integer price = (Integer) catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(title+" sold to agent "+msg.getSender().getName());
                }
                else {
                    // В промежуток между запросом и ответом книга была продана другому покупателю.
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

}
