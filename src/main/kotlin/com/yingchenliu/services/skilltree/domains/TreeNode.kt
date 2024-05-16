package com.yingchenliu.services.skilltree.domains

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

@Node
data class TreeNode(
    @Id
    val uuid: String,
    val name: String,
    val subtitle: String?,
    val content: String?,
    val isDeleted: Boolean = false,

    @Relationship(type = "PARENT_OF")
    val children: Set<TreeNode>
)