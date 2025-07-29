import BlockChain.{AddBlockEvent, GetChainEvent, GetLastHashEvent, getIndexEvent}
import Broker.{AddTransactionEvent, GetTransactionEvent}
import Miner.{MineCurrentBlockMinerEvent, MinerEvent, MiningFailed, ValidateBlock, mineBlock, obtainLastHashEvent, obtainLastIndexEvent}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Node {
    sealed trait NodeEvent
    case class AddNewTransactionEvent(transaction: Transaction) extends NodeEvent
    case class GetTransactionsEvent(replyTo: ActorRef[List[Transaction]]) extends NodeEvent
    case class WrappedTransactionsResponseEvent(transactions: List[Transaction], replyTo: ActorRef[List[Transaction]]) extends NodeEvent
    case class ReceiveNewBlockEvent(block:Block) extends NodeEvent
    case class AppendBlockEvent(block:Block) extends NodeEvent
    case object MineEvent extends NodeEvent

    case class GetChainRequestEvent(replyTo: ActorRef[List[Block]]) extends NodeEvent
    case class GetChainResponseEvent(chain: List[Block], replyTo:ActorRef[List[Block]]) extends NodeEvent

    case class NodeError(ex:String) extends NodeEvent


    def apply(nodeId: Int): Behavior[NodeEvent] =
        nodeBehavior(nodeId)

    private def nodeBehavior(
                                 nodeId:Int
                             ): Behavior[NodeEvent] = {
        Behaviors.setup { context =>
            implicit val timeout:Timeout = 3.seconds
            val blockchainActor = context.spawn(BlockChain(), "blockchain")
            val brokerActor = context.spawn(Broker(), "broker")
            val minerActor = context.spawn(Miner(brokerActor, blockchainActor), "miner")

            Behaviors.receive { (context, message) =>
                message match {
                    case AddNewTransactionEvent(transaction) =>
                        brokerActor ! AddTransactionEvent(transaction)
                        Behaviors.same
                    case GetTransactionsEvent(replyTo) =>
                        context.ask(brokerActor, ref => GetTransactionEvent(ref)) {
                            case Success(transactions) => WrappedTransactionsResponseEvent(transactions, replyTo)
                            case Failure(ex) => NodeError(ex.getMessage)
                        }
                        Behaviors.same
                    case WrappedTransactionsResponseEvent(transactions, replyTo) =>
                        replyTo ! transactions
                        Behaviors.same
                    case ReceiveNewBlockEvent(block) =>
                        context.ask(minerActor, ref => ValidateBlock(block, ref)) {
                            case Success(isValid) =>
                                if(isValid) {
                                    AppendBlockEvent(block)
                                } else {
                                    NodeError("Block received is invalid")
                                }
                            case Failure(ex) => NodeError(ex.getMessage)
                        }
                        Behaviors.same
                    case AppendBlockEvent(block) =>
                        blockchainActor ! AddBlockEvent(block)
                        Behaviors.same
                    case MineEvent =>
                        minerActor ! MineCurrentBlockMinerEvent
                        Behaviors.same
                    case GetChainRequestEvent(replyTo) =>
                        context.ask(blockchainActor, ref => GetChainEvent(ref)) {
                            case Success(blocks) => GetChainResponseEvent(blocks, replyTo)
                            case Failure(ex) => NodeError(ex.getMessage)
                        }
                        Behaviors.same
                    case GetChainResponseEvent(blocks, replyTo) =>
                        replyTo ! blocks
                        Behaviors.same
                    case NodeError(error) =>
                        context.log.error(error)
                        Behaviors.same

                }
            }
        }
    }
}

// sealed trait NodeMessage
//
//  case class AddTransaction(transaction: Transaction) extends NodeMessage
//
//  case class CheckPowSolution(solution: Long) extends NodeMessage
//
//  case class AddBlock(proof: Long) extends NodeMessage
//
//  case object GetTransactions extends NodeMessage
//
//  case object Mine extends NodeMessage
//
//  case object StopMining extends NodeMessage
//
//  case object GetStatus extends NodeMessage
//

//}