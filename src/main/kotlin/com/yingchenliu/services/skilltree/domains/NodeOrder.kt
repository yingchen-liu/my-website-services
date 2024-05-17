package com.yingchenliu.services.skilltree.domains

import org.springframework.data.neo4j.core.schema.Node
import java.util.*

data class NodeOrder(
    val previousNodeUuid: UUID,
    val nextNodeUuid: UUID
)