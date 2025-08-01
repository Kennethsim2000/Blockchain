# 🧱 Simplified Blockchain with Akka (Scala)

This project implements a **simplified blockchain system** using Scala and Akka Typed actors. It demonstrates the key building blocks of blockchain systems, while showcasing how Akka can be used to simulate distributed, event-driven behavior in a scalable and concurrent manner.

## 🧠 Core Components

- **Block**: Represents a unit in the chain, containing transaction data.
- **Blockchain**: Manages the chain of blocks, supports block validation and querying.
- **Broker**: Maintains a pool of unconfirmed (pending) transactions.
- **Miner**: Handles proof of work and creates new blocks from transactions.
- **Node**: Combines broker, miner, and blockchain; encapsulates full blockchain functionality.
- **Network**: Manages multiple nodes and facilitates communication between them.
- **RestHttp**: Exposes a RESTful HTTP API to interact with the blockchain network.

---

## 🕸️ Simulating a Distributed Network

Each **node** in the network is made up of a unique group of the following actors:

- 🧩 `NodeX`: Manages its internal actors (miner, broker, blockchain).
- 💬 `BrokerX`: Handles unconfirmed transactions.
- 🛠️ `MinerX`: Performs mining and block validation.
- 📚 `BlockchainX`: Stores and updates the block ledger.

### Features of Each Node:

- Maintains its own **independent blockchain**
- Mines blocks with **local transactions**
- Can **receive blocks** mined by other nodes
- **Broadcasts** new blocks to all peers in the network

---

## 🌐 REST API Guide

The system exposes a simple HTTP API through the `RestHttp` server, allowing external systems to interact with the blockchain network.

## REST API Endpoints

### 1. Add Transaction

**POST** `/transaction`  
Adds a new transaction to a specific node.

**Request Body (JSON):**
```json
{
  "nodeId": "node1",
  "sender": "Alice",
  "receiver": "Bob",
  "amount": 50
}
```

### 2. Mine block

**GET** `/mine?nodeId=node1`  
Triggers mining of the current block for the specified node.

### 3. Mine block

**GET** `/chain/{nodeId}`  
Fetches the blockchain from the specified node.

## 🌱 Potential New Features to Add

### 1. Dynamic Addition of New Nodes
Allow the network to dynamically accept new nodes at runtime. This would involve:
- Providing a REST endpoint to register new nodes.
- Broadcasting the current state of the chain to the new node for synchronization.
- Updating peer lists in existing nodes to include the newly added node.

### 2. Chain Consistency and Persistence
Ensure all nodes maintain a consistent view of the blockchain using the **Longest Chain Algorithm**. This includes:
- Periodic broadcasting of chain length and hashes to detect inconsistencies.
- Conflict resolution by adopting the longest valid chain.
- Persisting blocks to disk so nodes can rebuild the longest valid chain based off the hash of the last block in the chain.

