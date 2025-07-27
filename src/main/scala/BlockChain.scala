import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

//This is the actor that contains all the blocks
object BlockChain {
    sealed trait Chain
    case class ValidChain(blocks: Seq[Block]) extends Chain
    case class EmptyChain() extends Chain

    sealed trait BlockChainEvent
    case class GetChainEvent(replyTo: ActorRef[List[Block]]) extends BlockChainEvent
    case class AddBlockEvent(block: Block) extends BlockChainEvent
    case class GetLastHashEvent(replyTo: ActorRef[String]) extends BlockChainEvent
    case class getIndexEvent(replyTo: ActorRef[Int]) extends BlockChainEvent

    def apply(): Behavior[BlockChainEvent] = {
        val genesisBlock = Block.createGenesisBlock()
        blockchainBehavior(List(genesisBlock))
    }

    private def blockchainBehavior(blocks: List[Block]): Behavior[BlockChainEvent] = {

        Behaviors.receive { (context, message) =>
            message match {
                case GetChainEvent(replyTo) =>
                    replyTo ! blocks
                    Behaviors.same
                case AddBlockEvent(block) =>
                    val valid = Block.isValidProof(block)
                    if(valid) {
                        context.log.info("Block is valid, added")
                        blockchainBehavior(blocks :+ block)
                    }
                    else {
                        context.log.info("Block is invalid, rejected")
                        Behaviors.same
                    }
                case getIndexEvent(replyTo) =>
                    val lastBlock = blocks.last
                    replyTo ! lastBlock.index + 1
                    Behaviors.same
                case GetLastHashEvent(replyTo) =>
                    val lastBlock = blocks.last
                    replyTo ! lastBlock.hash
                    Behaviors.same


            }
        }
    }
}

//Other nodes can mine blocks, so we may receive a request to add a block that we didn’t mine. The proof is enough to
// add the new block, since we assume that all the nodes share the same list of pending transactions.