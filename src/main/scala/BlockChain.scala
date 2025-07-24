import BlockChain.{AddBlock, GetChain}
import akka.actor.{Actor, Props}

object BlockChain {
    case object GetChain
    case class AddBlock(block: Block)
    case class Chain(blocks: Seq[Block])
    def props:Props = Props[BlockChain]
}

class BlockChain extends Actor {

    val chain:Seq[Block] = Seq()

    override def receive: Receive = {
        case GetChain => sender() ! chain
        case AddBlock(block) => ???
    }
}