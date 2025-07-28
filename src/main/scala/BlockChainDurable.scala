
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}

//This is the actor that contains all the blocks
object BlockChainDurable {

    sealed trait BlockChainEvent

    case class GetChainEvent(replyTo: ActorRef[List[Block]]) extends BlockChainEvent
    case class AddBlockEvent(block: Block) extends BlockChainEvent
    case class GetLastHashEvent(replyTo: ActorRef[String]) extends BlockChainEvent
    case class getIndexEvent(replyTo: ActorRef[Int]) extends BlockChainEvent

    case class BlockChainState(blocks: List[Block])

    def apply(): Behavior[BlockChainEvent] = {
        val genesisBlock = Block.createGenesisBlock()
        blockchainDurableBehavior(List(genesisBlock))
    }

    private def blockchainDurableBehavior(blocks: List[Block]): Behavior[BlockChainEvent] = {
        Behaviors.setup { context:ActorContext[BlockChainEvent] =>
                DurableStateBehavior[BlockChainEvent, BlockChainState](
                    persistenceId = PersistenceId.ofUniqueId("abc"),
                    emptyState = BlockChainState(blocks),
                    commandHandler = (state, command) =>
                        command match {
                            case GetChainEvent(replyTo) =>
                                Effect.reply(replyTo)(state match {
                                    case BlockChainState(blocks) => blocks
                                })
                            case AddBlockEvent(block)   =>
                                val valid = Block.isValidProof(block)
                                if(valid) {
                                    context.log.info("Block is valid, added")
                                    Effect.persist(state.copy(blocks = state.blocks :+ block))
                                }
                                else {
                                    context.log.info("Block is invalid, rejected")
                                    Effect.none
                                }
                            case getIndexEvent(replyTo) =>
                                val lastBlock = state.blocks.last
                                Effect.reply(replyTo)(lastBlock.index + 1)
                            case GetLastHashEvent(replyTo) =>
                                val lastBlock = blocks.last
                                Effect.reply(replyTo)(lastBlock.hash)
                        }
                )
        }

    }
}