package com.yingchenliu.services.skilltree.repositories

import com.yingchenliu.services.skilltree.domains.TreeNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query

interface TreeNodeRepository : Neo4jRepository<TreeNode, String> {
    @Query(
        "MATCH path = (startNode:TreeNode)-[:PARENT_OF*]->(endNode:TreeNode) " +
        "WHERE startNode.uuid = \$uuid AND endNode.isDeleted = false " + """
        AND (endNode.isCollapsed = true OR 
        NONE(node IN nodes(path) WHERE node.isCollapsed = true))
        WITH collect(path) as paths, startNode
        WITH startNode,
        reduce(a=[], node in reduce(b=[], c in [aa in paths | nodes(aa)] | b + c) | case when node in a then a else a + node end) as nodes,
        reduce(d=[], relationship in reduce(e=[], f in [dd in paths | relationships(dd)] | e + f) | case when relationship in d then d else d + relationship end) as relationships
        RETURN startNode, relationships, nodes;
        """
    )
    fun findTreeNodeAndNonDeletedChildren(uuid: String): TreeNode
}