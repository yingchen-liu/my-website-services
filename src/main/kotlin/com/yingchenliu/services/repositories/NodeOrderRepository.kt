package com.yingchenliu.services.repositories

import com.yingchenliu.services.domains.NodeOrder
import org.neo4j.driver.Driver
import org.neo4j.driver.Session
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class NodeOrderRepository(private val driver: Driver) {

    fun findTreeNodeNonDeletedChildrenOrder(uuid: UUID): List<NodeOrder> {
        val query = "MATCH path = (startNode:TreeNode)-[:PARENT_OF*]->(endNode:TreeNode)" +
                "WHERE startNode.uuid = \$uuid AND endNode.isDeleted = false" +
                """
            AND (
                endNode.isCollapsed = true OR 
                startNode.isCollapsed = true OR 
                NONE(node IN nodes(path) WHERE node.isCollapsed = true)
            )
        WITH startNode, [node IN nodes(path) | node] AS treeNodes
        UNWIND treeNodes AS tn
        MATCH (tn)-[:BEFORE]->(endNode:TreeNode)
        RETURN DISTINCT tn.uuid AS previousNodeUuid, endNode.uuid AS nextNodeUuid
        """
        val session: Session = driver.session()
        val result = session.run(query, mapOf("uuid" to uuid.toString()))

        val nodeOrders = mutableListOf<NodeOrder>()
        result.list().forEach { record ->
            val previousNodeUuid = UUID.fromString(record["previousNodeUuid"].asString())
            val nextNodeUuid = UUID.fromString(record["nextNodeUuid"].asString())
            nodeOrders.add(NodeOrder(previousNodeUuid, nextNodeUuid))
        }

        session.close()
        return nodeOrders
    }
}