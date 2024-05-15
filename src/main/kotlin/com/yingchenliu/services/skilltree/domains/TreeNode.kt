package com.yingchenliu.services.skilltree.domains

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

@Node
data class TreeNode(
    @Id
    @GeneratedValue
    val id: Long?,
    val name: String,
    val subtitle: String?,
    val content: String?,

    @Relationship(type = "PARENT_OF")
    val children: Set<TreeNode>?
)