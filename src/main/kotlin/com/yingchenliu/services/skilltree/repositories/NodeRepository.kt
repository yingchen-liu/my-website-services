package com.yingchenliu.services.skilltree.repositories

import com.yingchenliu.services.skilltree.domains.TreeNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query

interface TreeNodeRepository: Neo4jRepository<TreeNode, Long> {
    fun findByName(name: String): TreeNode
}