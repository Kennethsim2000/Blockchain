package durable

import java.security.MessageDigest
import scala.annotation.tailrec

case class Transaction(sender: String, receiver: String, amount: Int)

case class Block(index: Int, hash:String, prevHash: String, proof:Long, timestamp: Long, transactions: List[Transaction])
//hash for a block is used to check if the block has been tampered with

object BlockDurable { // companion object for block with default methods

    val difficulty = 4

    def computeHash(index: Int, prevHash:String, transactions: List[Transaction], proof:Long):String = {
        val transactionsStr = transactions.map(transaction =>
            s"${transaction.sender} -> ${transaction.receiver} amount: ${transaction.amount}").mkString("|")
        val hashStr = s"$index$prevHash$transactionsStr$proof"
        val bytes = hashStr.getBytes
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hashString = hash.map("%02x".format(_)).mkString
        hashString
    }

    def mineBlock(index: Int, prevHash:String, transactions: List[Transaction]) = {
        val timeStamp = System.currentTimeMillis()
        val prefix = "0" * difficulty

        @tailrec
        def findProof(proof:Int):(String, Long) = {
            val hash = computeHash(index, prevHash, transactions = transactions, proof = proof)
            if(hash.startsWith(prefix)) (hash, proof)
            else findProof(proof + 1)
        }

        val (hash,proof) = findProof(0)
        (hash, proof, timeStamp)
    }

    //create the genesis block, the first block
    def createGenesisBlock(): Block = {
        val initialTransaction = Transaction("Genesis", "miner", 50)
        val transactions = List(initialTransaction)
        val (hash, proof, timestamp) = mineBlock(0, "0", transactions)
        Block(0, hash, "0", proof, timestamp, transactions)
    }

    def createNextBlock(prevBlock:Block, transactions: List[Transaction]) = {
        val currIndex = prevBlock.index + 1
        val (hash, proof, timestamp) = mineBlock(currIndex, prevBlock.hash, transactions)
        Block(currIndex, hash, prevBlock.hash, proof, timestamp, transactions)
    }

    //when verifying the proof, we check not just the prevHash but also the transactions and index
    def isValidProof(block: Block): Boolean = {
        val prefix = "0" * difficulty
        val hashComputed = computeHash(block.index, block.prevHash, block.transactions, block.proof)
        hashComputed == block.hash && hashComputed.startsWith(prefix)
    }

}

//The proof in a blockchain serves as a mechanism to secure the network and ensure that adding a block requires
// significant computational effort.
