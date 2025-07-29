package durable

import BlockChainDurable.{AddBlockEvent, GetChainEvent}
import BrokerDurable.{AddTransactionEvent, GetTransactionEvent}
import MinerDurable.{MineCurrentBlockMinerEvent, ValidateBlock}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object NodeDurable {
    sealed trait NodeEvent
    case class AddNewTransactionEvent(transaction: Transaction) extends NodeEvent
    case class GetTransactionsEvent(replyTo: ActorRef[List[Transaction]]) extends NodeEvent
    case class WrappedTransactionsResponseEvent(transactions: List[Transaction], replyTo: ActorRef[List[Transaction]]) extends NodeEvent
    case class ReceiveNewBlockEvent(block:Block) extends NodeEvent
    case class AppendBlockEvent(block:Block) extends NodeEvent
    case object MineEvent extends NodeEvent

    case class GetChainRequestEvent(replyTo: ActorRef[List[Block]]) extends NodeEvent
    case class GetChainResponseEvent(chain: List[Block], replyTo:ActorRef[List[Block]]) extends NodeEvent
    case class BlockchainReady() extends NodeEvent

    case class NodeError(ex:String) extends NodeEvent


    def apply(nodeId: Int): Behavior[NodeEvent] =
        nodeBehavior(nodeId)

    private def nodeBehavior(
                                nodeId:Int
                            ): Behavior[NodeEvent] = {
        Behaviors.setup { context =>
            implicit val timeout:Timeout = 3.seconds
            val blockchainActor = context.spawn(BlockChainDurable(), "BlockChainDurable")
            val brokerActor = context.spawn(BrokerDurable(blockchainActor), "brokerDurable")
            val minerActor = context.spawn(MinerDurable(brokerActor, blockchainActor), "minerDurable")

            Behaviors.receive { (context, message) =>
                message match {
                    case AddNewTransactionEvent(transaction) =>
                        brokerActor ! AddTransactionEvent(transaction)
                        Behaviors.same
                    case GetTransactionsEvent(replyTo) =>
                        context.ask(brokerActor, ref => GetTransactionEvent(ref)) {
                            case Success(transactions) => WrappedTransactionsResponseEvent(transactions, replyTo)
                            case Failure(ex) => NodeError("get transactions" + ex.getMessage)
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
                        println(s"node error $error")
                        context.log.error(error)
                        Behaviors.same

                }
            }
        }
    }
}