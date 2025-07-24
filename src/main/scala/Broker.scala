import Broker.{AddTransactionEvent, ClearTransactionEvent, GetTransactionEvent}
import akka.actor.{Actor, ActorRef}

// Broker is in charge of the current transactions
object Broker {
    sealed trait BrokerEvent
    case class AddTransactionEvent(transaction: Transaction) extends BrokerEvent
    case object GetTransactionEvent extends BrokerEvent
    case object ClearTransactionEvent extends BrokerEvent
}

class Broker(blockchainActor: ActorRef) extends Actor {

    var pendingTransactions:List[Transaction] = List()

    override def receive: Receive = {
        case AddTransactionEvent(transaction) => pendingTransactions :+ transaction
        case GetTransactionEvent => sender() ! pendingTransactions
        case ClearTransactionEvent =>
            pendingTransactions = List()
    }
}