import BlockChain.AddBlockEvent
import Broker.{AddTransactionEvent, ClearTransactionEvent, GetTransactionEvent, MineCurrentBlockBrokerEvent}
import akka.actor.{Actor, ActorRef, Props}

// Broker is in charge of the current transactions
object Broker {
    sealed trait BrokerEvent
    case class AddTransactionEvent(transaction: Transaction) extends BrokerEvent
    case object GetTransactionEvent extends BrokerEvent
    case object ClearTransactionEvent extends BrokerEvent
    case object MineCurrentBlockBrokerEvent extends BrokerEvent

    def props(brokerActor: ActorRef) : Props = Props(new Broker(brokerActor))
}

class Broker(blockchainActor: ActorRef) extends Actor {

    var pendingTransactions:List[Transaction] = List()

    override def receive: Receive = {
        case AddTransactionEvent(transaction) => pendingTransactions :+ transaction
        case GetTransactionEvent => sender() ! pendingTransactions
        case ClearTransactionEvent =>
            pendingTransactions = List()
        case MineCurrentBlockBrokerEvent =>
            blockchainActor ! AddBlockEvent(pendingTransactions)
            pendingTransactions = List()
    }
}