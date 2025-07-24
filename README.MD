# 🧱 Simplified Blockchain with Akka (Scala)

This project implements a **simplified blockchain system** using Scala and Akka actors. It demonstrates the core components of a blockchain in a minimal, educational setup:

- **Block**: Represents a single block in the chain.
- **Blockchain**: Manages the chain of blocks and validates additions.
- **Broker**: Manages the transactions and communicates with BlockChain to mine block
- **Miner**: Handles proof and validation of block, mines new blocks.
- **Node**: Exposes REST API to public.

### Components

- **Block**  
  Holds data such as index, timestamp, transactions, previous hash, and a proof (e.g., for proof-of-work).

- **Blockchain**  
  A list of blocks. Validates new blocks and maintains immutability.

- **Miner**  
  Gathers pending transactions, performs mining (proof-of-work), and creates new blocks.

- **Node**  
  The gateway for external requests. Sends transactions to the miner and retrieves data from the blockchain.
