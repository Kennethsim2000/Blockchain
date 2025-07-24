import Miner.{AddTransaction, MinePendingTransactions}
import akka.actor.{Actor, ActorRef}

object Miner {
    case object MinePendingTransactions
    case class AddTransaction(transaction: Transaction)
}

class Miner(blockChain:ActorRef) extends Actor {
    var pendingTransactions = List()

    override def receive: Receive = {
        //TODO: complete method completion
        case MinePendingTransactions => ???
        case AddTransaction(transaction) => ???
    }
}