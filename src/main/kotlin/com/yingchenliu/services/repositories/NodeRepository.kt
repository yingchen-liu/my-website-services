package com.yingchenliu.services.repositories

import com.yingchenliu.services.domains.NodeOrder
import com.yingchenliu.services.domains.TreeNode
import org.neo4j.driver.Session
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

interface NodeRepository : Neo4jRepository<TreeNode, UUID> {
    /**
     * Finds a TreeNode along with all its descendant children nodes until a node with the "isCollapsed"
     * property set to true is encountered or until the end of the descendant chain.
     *
     * This query navigates the tree structure starting from the node with the specified UUID
     * until it reaches either a node where "isCollapsed" is true or the end of the descendant chain.
     * It collects all valid paths and filters out nodes that are marked as deleted or collapsed.
     * Finally, it returns the start node along with its non-deleted descendant nodes and relationships.
     *
     * @param uuid the UUID of the start TreeNode from which to find descendants
     * @return the start TreeNode along with its non-deleted descendant nodes and relationships
     */
    @Query(
        "MATCH path = (startNode:TreeNode)-[:PARENT_OF*]->(endNode:TreeNode) " +
                "WHERE startNode.uuid = \$uuid AND endNode.isDeleted = false " + """
        AND (
            endNode.isCollapsed = true OR 
            startNode.isCollapsed = true OR 
            NONE(node IN nodes(path) WHERE node.isCollapsed = true)
        ) 
        AND all(node in nodes(path) WHERE node = startNode OR node = endNode OR (NOT node.isCollapsed))
        WITH collect(path) as paths, startNode
        WITH startNode,
        reduce(a=[], node in reduce(b=[], c in [aa in paths | nodes(aa)] | b + c) | case when node in a then a else a + node end) as nodes,
        reduce(d=[], relationship in reduce(e=[], f in [dd in paths | relationships(dd)] | e + f) | case when relationship in d then d else d + relationship end) as relationships
        RETURN startNode, relationships, nodes;
        """
    )
    fun findTreeNodeAndNonDeletedChildren(uuid: UUID): TreeNode

    @Query(
        "MATCH (p:TreeNode)-[:PARENT_OF]->(c:TreeNode) WHERE c.uuid = \$childUUID " +
                "RETURN p LIMIT 1"
    )
    fun findParentNode(childUUID: UUID): TreeNode

    /**
     * Creates a parent-child relationship between two TreeNodes with the specified UUIDs.
     *
     * This operation establishes a "PARENT_OF" relationship between the TreeNode represented by parentUuid
     * and the TreeNode represented by childUuid.
     *
     * @param parentUUID the UUID of the parent TreeNode
     * @param childUUID the UUID of the child TreeNode
     */
    @Query(
        "MATCH (parent:TreeNode {uuid: \$parentUUID}), (child:TreeNode {uuid: \$childUUID}) " +
                "CREATE (parent)-[:PARENT_OF]->(child)"
    )
    fun createParentRelationship(parentUUID: UUID, childUUID: UUID)

    /**
     * Removes the "before" relationship of the TreeNode with the given UUID among its neighboring nodes.
     *
     * This operation adjusts the ordering of nodes when removing a TreeNode with a specific position.
     * Specifically, it ensures that the TreeNode (b) is no longer before any TreeNode (c)
     * while maintaining the existing order among other nodes.
     *
     * @param uuid the UUID of the TreeNode for which the "before" relationship will be removed
     */
    @Query(
        "MATCH (b:TreeNode) WHERE b.uuid = \$uuid " + """
        OPTIONAL MATCH (a:TreeNode)-[r1:BEFORE]->(b:TreeNode)
        OPTIONAL MATCH (b:TreeNode)-[r2:BEFORE]->(c:TreeNode)
        FOREACH (_ IN CASE WHEN a IS NOT NULL AND c IS NOT NULL THEN [1] ELSE [] END |
          CREATE (a)-[:BEFORE]->(c)
        )
        FOREACH (_ IN CASE WHEN r1 IS NOT NULL THEN [1] ELSE [] END |
          DELETE r1
        )
        FOREACH (_ IN CASE WHEN r2 IS NOT NULL THEN [1] ELSE [] END |
          DELETE r2
        )
        """
    )
    fun removeBeforeRelationship(uuid: UUID)

    /**
     * Removes the parent-child relationship between two TreeNodes, where the specified child node
     * has the given UUID.
     *
     * @param childUuid the UUID of the child TreeNode whose incoming "PARENT_OF" relationship will be removed
     */
    @Query(
        "MATCH (parent:TreeNode)-[r:PARENT_OF]->(child:TreeNode {uuid: \$childUuid}) " +
                "DELETE r"
    )
    fun removeParentRelationship(childUuid: UUID)

    /**
     * Inserts the node with uuidOfNewNode before the node with uuidOfExistingNode, while maintaining
     * the existing BEFORE relationships among other nodes.
     *
     * This is achieved by:
     * 1. Matching the target node (b) with the given uuidOfExistingNode.
     * 2. Optionally matching any node (a) that has a BEFORE relationship with (b).
     * 3. Optionally matching any node (c) that (b) has a BEFORE relationship with.
     * 4. Matching the node (n) with the given uuidOfNewNode.
     * 5. If there is a node (a) that is before (b), creating a new BEFORE relationship
     *    between (a) and (n) and deleting the existing relationship (a)-[:BEFORE]->(b).
     * 6. Creating a BEFORE relationship between (n) and (b).
     *
     * @param uuidOfNewNode the UUID of the new TreeNode to be inserted before the existing node
     * @param uuidOfExistingNode the UUID of the existing TreeNode before which the new node will be inserted
     */
    @Query(
        "MATCH (b:TreeNode) WHERE b.uuid = \$uuidOfExistingNode " + """
        OPTIONAL MATCH (a:TreeNode)-[r1:BEFORE]->(b:TreeNode)
        OPTIONAL MATCH (b:TreeNode)-[r2:BEFORE]->(c:TreeNode) """ +
                "MATCH (n:TreeNode {uuid: \$uuidOfNewNode}) " + """
        FOREACH (_ IN CASE WHEN a IS NOT NULL THEN [1] ELSE [] END |
          CREATE (a)-[:BEFORE]->(n)
          DELETE r1
        )
        CREATE (n)-[:BEFORE]->(b)
        """
    )
    fun createBeforeRelationship(uuidOfNewNode: UUID, uuidOfExistingNode: UUID)

    /**
     * Creates an "after" relationship between two TreeNode entities.
     * This is achieved by adding a relationship indicating that the node with the given uuidOfNodeB
     * comes before the node with the specified uuidOfNodeA.
     *
     * Specifically, the relationship (b)-[:BEFORE]->(a) is created.
     *
     * @param uuidOfNodeA the UUID of the new TreeNode that is to be placed after the existing node
     * @param uuidOfNodeB the UUID of the existing TreeNode that will be before the new node
     */
    @Query(
        "MATCH (b:TreeNode) WHERE b.uuid = \$uuidOfNodeB " +
                "MATCH (a:TreeNode {uuid: \$uuidOfNodeA}) " +
                "CREATE (b)-[:BEFORE]->(a)"
    )
    fun createAfterRelationship(uuidOfNodeA: UUID, uuidOfNodeB: UUID)

    /**
     * Finds the last child of a parent node, if any exists.
     * The last child is defined as the node that does not have any outgoing "BEFORE" relationships
     * with other children of the same parent node.
     *
     * @param parentUUID the UUID of the parent TreeNode
     * @return the last child TreeNode or null if no such child exists
     */
    @Query(
        "MATCH (p:TreeNode {uuid: \$parentUUID})-[:PARENT_OF]->(n:TreeNode) " +
                "WHERE NOT (n)-[:BEFORE]-() OR (()-[:BEFORE]->(n) AND NOT (n)-[:BEFORE]->()) " +
                "RETURN n " +
                "LIMIT 1"
    )
    fun findLastChild(parentUUID: UUID): TreeNode?
}