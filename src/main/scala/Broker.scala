import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Broker {

    sealed trait BrokerEvent
    case class AddTransactionEvent(transaction: Transaction) extends BrokerEvent
    case class GetTransactionEvent(replyTo: ActorRef[List[Transaction]]) extends BrokerEvent
    case object ClearTransactionEvent extends BrokerEvent

    // Broker contains all the transactions, and communicates with blockchain actor to add block
    def apply(): Behavior[BrokerEvent] =
        brokerBehavior(List.empty)

    private def brokerBehavior(
                                  pendingTransactions: List[Transaction]
                              ): Behavior[BrokerEvent] = {

        Behaviors.receive { (context, message) =>
            message match {
                case AddTransactionEvent(transaction) =>
                    brokerBehavior(pendingTransactions :+ transaction)

                case GetTransactionEvent(replyTo) =>
                    replyTo ! pendingTransactions
                    Behaviors.same

                case ClearTransactionEvent =>
                    brokerBehavior(List.empty)
            }
        }
    }
}
