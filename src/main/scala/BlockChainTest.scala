object BlockchainTest extends App {
    val genesis = Block.createGenesisBlock()
    println(s"Genesis Block:\n$genesis\n")

    val txs = List(
        Transaction("Alice", "Bob", 20),
        Transaction("Bob", "Charlie", 10)
    )
    val next = Block.createNextBlock(genesis, txs)
    println(s"Next Block:\n$next")
}
