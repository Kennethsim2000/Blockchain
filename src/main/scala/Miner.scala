import BlockChain.{AddBlockEvent, GetLastHashEvent, getIndexEvent}
import Broker.{ClearTransactionEvent, GetTransactionEvent}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

//Miner is in charge of validating blocks, and mining the current block
object Miner {
    sealed trait MinerEvent
    case class ValidateBlock(block: Block, replyTo: ActorRef[Boolean]) extends MinerEvent

    case object MineCurrentBlockMinerEvent extends MinerEvent
    sealed trait MiningWorkflowEvent extends MinerEvent
    case class obtainLastHashEvent(transactions: List[Transaction]) extends MiningWorkflowEvent
    case class obtainLastIndexEvent(transactions: List[Transaction], prevHash: String) extends MiningWorkflowEvent
    case class mineBlock(transactions: List[Transaction], prevHash: String, index: Int) extends MiningWorkflowEvent

    case class MiningFailed(error: String) extends MinerEvent

    def createCoinBaseTransaction(): Transaction = {
        Transaction("Network", "Miner", 50)
    }
    def apply(brokerActor: ActorRef[Broker.BrokerEvent], blockchainActor: ActorRef[BlockChain.BlockChainEvent]): Behavior[MinerEvent] =
        minerBehavior(brokerActor, blockchainActor)

    private def minerBehavior(
                                 brokerActor: ActorRef[Broker.BrokerEvent],
                                 blockchainActor: ActorRef[BlockChain.BlockChainEvent]
                              ): Behavior[MinerEvent] = {
        Behaviors.setup { context =>
            implicit val timeout:Timeout = 3.seconds
            Behaviors.receive { (context, message) =>
                message match {
                    case ValidateBlock(block, replyTo) =>
                        val isValid = Block.isValidProof(block)
                        replyTo ! isValid
                        Behaviors.same
                    case MineCurrentBlockMinerEvent =>
                        context.log.info("Received mine block event")
                        context.ask(brokerActor, ref => GetTransactionEvent(ref)) {
                            case Success(transactions) =>
                                val coinbaseTransaction = createCoinBaseTransaction()
                                obtainLastHashEvent(transactions :+ coinbaseTransaction) // add coinbase transaction for the miner
                            case Failure(ex) => MiningFailed(ex.getMessage)
                        }
                        Behaviors.same
                    case obtainLastHashEvent(transactions) =>
                        context.log.info(s"Mining block with ${transactions.size} transactions")
                        context.ask(blockchainActor, ref => GetLastHashEvent(ref)) {
                            case Success(prevHash) => obtainLastIndexEvent(transactions, prevHash)
                            case Failure(ex) => MiningFailed(ex.getMessage)
                        }
                        Behaviors.same
                    case obtainLastIndexEvent(transactions, prevHash) =>
                        context.log.info(s"Mining block with ${transactions.size} transactions and prevHash ${prevHash}")
                        context.ask(blockchainActor, ref => getIndexEvent(ref)) {
                        case Success(lastIndex) => mineBlock(transactions, prevHash, lastIndex)
                        case Failure(ex) => MiningFailed(ex.getMessage)
                    }
                        Behaviors.same
                    case mineBlock(transactions, prevHash, index) =>
                        context.log.info(s"Mining block with ${transactions.size} transactions and prevHash ${prevHash}" +
                            s"with index ${index}")
                        val (hash, proof, timestamp) = Block.mineBlock(index, prevHash, transactions)
                        val newBlock = Block(index, hash, prevHash, proof, timestamp, transactions)
                        blockchainActor ! AddBlockEvent(newBlock)
                        brokerActor ! ClearTransactionEvent
                        Behaviors.same
                    case MiningFailed(error) =>
                        context.log.error(error)
                        Behaviors.same
                }
            }
        }
    }
}
//In context.ask(...), the ref is an auto-generated temporary ActorRef[ReplyType] that you use to receive the response
// from another actor.
