package durable

import NodeDurable.{AddNewTransactionEvent, GetChainRequestEvent, GetTransactionsEvent, MineEvent}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.util.Timeout
import durable.Network.SendToNode

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object NodeDurableTest extends App {
    val system : ActorSystem[Network.NetworkEvent] = ActorSystem(Network(), "network")
    system ! SendToNode("node1",AddNewTransactionEvent(Transaction("Alice", "Bob", 50)))
    system ! SendToNode("node1", AddNewTransactionEvent(Transaction("Bob", "Charlie", 50)))
    system ! SendToNode("node1", MineEvent)
    Thread.sleep(3000)
    implicit val timeout:Timeout = 3.seconds
    import system.executionContext
    implicit val scheduler = system.scheduler

    val futureChain = system.ask[List[Block]] { replyTo: ActorRef[List[Block]] =>
        SendToNode("node1", GetChainRequestEvent(replyTo))
    }

    futureChain.onComplete {
        case Success(chain) =>
            println("Blocks in the blockchain of node1")
            chain.foreach(x=> println("node1 is " + x))
            system.terminate()
        case Failure(ex) =>
            println(s"Failed to get blockchain, ${ex.getMessage}")
            system.terminate()
    }

    val futureChain2 = system.ask[List[Block]] { replyTo: ActorRef[List[Block]] =>
        SendToNode("node2", GetChainRequestEvent(replyTo))
    }

    futureChain2.onComplete {
        case Success(chain) =>
            println("Blocks in the blockchain of node2")
            chain.foreach(x=> println("node2 is " + x))
            system.terminate()
        case Failure(ex) =>
            println(s"Failed to get blockchain, ${ex.getMessage}")
            system.terminate()
    }
}
