package com.yingchenliu.services.skilltree.services

import com.sun.source.tree.Tree
import com.yingchenliu.services.skilltree.domains.NodeOrder
import com.yingchenliu.services.skilltree.domains.TreeNode
import com.yingchenliu.services.skilltree.repositories.NodeOrderRepository
import com.yingchenliu.services.skilltree.repositories.NodeRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class NodeService(
    private val nodeRepository: NodeRepository,
    private val nodeOrderRepository: NodeOrderRepository,
    @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager
) {
    fun findFromNode(uuid: UUID): TreeNode {
        val orders = nodeOrderRepository.findTreeNodeNonDeletedChildrenOrder(uuid)
        println("orders")
        println(orders)
        val node = nodeRepository.findTreeNodeAndNonDeletedChildren(uuid)
        return sortChildrenAtCurrentLevel(node, orders)
    }

    private fun sortChildrenAtCurrentLevel(node: TreeNode, orders: List<NodeOrder>): TreeNode {
        return node.children?.let { children ->
            val nodesByUuid = children.associateBy { it.uuid }
            val orderForThisLevel =
                orders.filter { order -> nodesByUuid.contains(order.previousNodeUuid) || nodesByUuid.contains((order.nextNodeUuid)) }
                    .toList()

            val relationships =
                orderForThisLevel.associate { it.previousNodeUuid.toString() to listOf(it.nextNodeUuid.toString()) }
            println(TopologicalSort(relationships).topologicalSort())
            val sortedOrderedNodes = TopologicalSort(relationships).topologicalSort()
                .mapNotNull { uuid -> nodesByUuid[UUID.fromString(uuid)] }.toList()

            println("sortedOrderedNodes")
            println(sortedOrderedNodes)

            val unorderedNodes = children.filter { node -> !sortedOrderedNodes.contains(node) }.toList()

            println("unorderedNodes")
            println(unorderedNodes)

            return node.copy(children = (sortedOrderedNodes + unorderedNodes).map { node -> sortChildrenAtCurrentLevel(node, orders) })
        } ?: node
    }

    fun findFromRoot(): TreeNode {
        return findFromNode(UUID.fromString("b1747c9f-3818-4edd-b7c6-7384b2cb5e41"))
    }

    fun findById(uuid: UUID): Optional<TreeNode> {
        return nodeRepository.findById(uuid)
    }

    @Transactional("transactionManager")
    fun create(node: TreeNode, parentUuid: UUID): TreeNode {
        val newNode = node.copy(createdAt = LocalDateTime.now(), lastUpdatedAt = LocalDateTime.now())
        val createdNode = nodeRepository.save(newNode)
        nodeRepository.createRelationship(parentUuid, newNode.uuid)
        return createdNode;
    }

    fun update(node: TreeNode): TreeNode {
        val newNode = node.copy(lastUpdatedAt = LocalDateTime.now())
        return nodeRepository.save(newNode)
    }

    fun delete(node: TreeNode): TreeNode {
        val newNode = node.copy(isDeleted = true)
        return update(newNode)
    }

    fun refresh() {
        val nodes = nodeRepository.findAll()
        nodeRepository.saveAll(nodes)
    }
}