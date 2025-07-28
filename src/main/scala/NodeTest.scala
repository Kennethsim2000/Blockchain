import Node.{AddNewTransactionEvent, GetChainRequestEvent, GetTransactionsEvent, MineEvent}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object NodeTest extends App {
    val system : ActorSystem[Node.NodeEvent] = ActorSystem(Node(1), "Node1")
    system ! AddNewTransactionEvent(Transaction("Alice", "Bob", 50))
    system ! AddNewTransactionEvent(Transaction("Bob", "Charlie", 50))
    system ! MineEvent
    Thread.sleep(1000) // give time for node to mine
    implicit val timeout:Timeout = 3.seconds
    import system.executionContext
    implicit val scheduler = system.scheduler
    val futureTransactions = system.ask(replyTo => GetTransactionsEvent(replyTo))
    futureTransactions.onComplete { // expect no transactions to be printed
        case Success(transactions) =>
            println("Transactions in the broker")
            transactions.foreach(x=> println(x))
            system.terminate()
        case Failure(ex) =>
            println(s"Failed to get transactions, ${ex.getMessage}")
            system.terminate()
    }
    val futureChain = system.ask(replyTo => GetChainRequestEvent(replyTo))
    futureChain.onComplete {
        case Success(chain) =>
            println("Blocks in the blockchain")
            chain.foreach(x=> println(x))
            system.terminate()
        case Failure(ex) =>
            println(s"Failed to get blockchain, ${ex.getMessage}")
            system.terminate()
    }
}
