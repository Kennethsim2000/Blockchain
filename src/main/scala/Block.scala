import java.security.MessageDigest

case class Transaction(sender: String, receiver: String, amount: Int)

case class Block(index: Int, hash:String, prevHash: String, transaction: List[Transaction])
//hash for a block is used to check if the block has been tampered with

object Block { // companion object for block with default methods

    def computeHash(index: Int, prevHash:String, transactions: List[Transaction]):String = {
        val transactionsStr = transactions.map(transaction =>
            s"${transaction.sender} -> ${transaction.receiver} amount: ${transaction.amount}").mkString("|")
        val hashStr = s"$index$prevHash$transactionsStr"
        val bytes = hashStr.getBytes
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hashString = hash.map("%02x".format(_)).mkString
        hashString
    }

    def createGenesisBlock(): Block = {
        //TODO: Complete generation of first Block in the blockchain
    }

    def createNextBlock(prevBlock:Block, transactions: List[Transaction]) = {
        //TODO: Complete creation of first block
    }

}