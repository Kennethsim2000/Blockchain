import Broker.MineCurrentBlockBrokerEvent
import Miner.{MineCurrentBlockMinerEvent, Ready, ValidateBlock}
import akka.actor.{Actor, ActorRef, Props}

object Miner {
    sealed trait MinerEvent
    case class ValidateBlock(block: Block) extends MinerEvent
    case object MineCurrentBlockMinerEvent extends MinerEvent
    case object Ready extends MinerEvent

    def props(brokerActor: ActorRef) : Props = Props(new Miner(brokerActor))
}

class Miner(brokerActor:ActorRef) extends Actor {

    override def receive: Receive = {
        case ValidateBlock(block) =>
            val isValid = Block.isValidProof(block)
            sender() ! isValid
        case MineCurrentBlockMinerEvent => brokerActor ! MineCurrentBlockBrokerEvent
        case Ready => ??? // TODO: To be implemented in case the miner is not ready
    }
}
