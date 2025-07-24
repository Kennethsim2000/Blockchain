
object BlockTest extends App {
    val genesis = Block.createGenesisBlock()
    println(s"Genesis Block:\n$genesis")
    println(Block.isValidProof(genesis))

    val txs = List(
        Transaction("Alice", "Bob", 20),
        Transaction("Bob", "Charlie", 10)
    )
    val next = Block.createNextBlock(genesis, txs)
    println()
    println(s"Next Block:\n$next")
    println(Block.isValidProof(next))

}
