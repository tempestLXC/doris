// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.plans.algebra;

import org.apache.doris.nereids.trees.expressions.EqualTo;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.MarkJoinSlotReference;
import org.apache.doris.nereids.trees.plans.JoinHint;
import org.apache.doris.nereids.trees.plans.JoinHint.JoinHintType;
import org.apache.doris.nereids.trees.plans.JoinType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Common interface for logical/physical join.
 */
public interface Join {
    JoinType getJoinType();

    List<Expression> getHashJoinConjuncts();

    default List<EqualTo> getEqualToConjuncts() {
        return getHashJoinConjuncts().stream().filter(EqualTo.class::isInstance).map(EqualTo.class::cast)
                .collect(Collectors.toList());
    }

    List<Expression> getOtherJoinConjuncts();

    Optional<Expression> getOnClauseCondition();

    JoinHint getHint();

    boolean isMarkJoin();

    default boolean hasJoinHint() {
        return getHint() != JoinHint.NONE;
    }

    /**
     * The join plan has join condition or not.
     */
    default boolean hasJoinCondition() {
        return !getHashJoinConjuncts().isEmpty() || !getOtherJoinConjuncts().isEmpty();
    }

    default JoinHintType getLeftHint() {
        return JoinHintType.NONE;
    }

    /**
     * Get the hint type of join's right child.
     */
    default JoinHintType getRightHint() {
        switch (getHint()) {
            case SHUFFLE_RIGHT:
                return JoinHintType.SHUFFLE;
            case BROADCAST_RIGHT:
                return JoinHintType.BROADCAST;
            default:
                return JoinHintType.NONE;
        }
    }

    default Optional<MarkJoinSlotReference> getLeftMarkJoinSlotReference() {
        return Optional.empty();
    }
}
