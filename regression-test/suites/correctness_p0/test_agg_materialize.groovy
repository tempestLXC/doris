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

// The cases is copied from https://github.com/trinodb/trino/tree/master
// /testing/trino-product-tests/src/main/resources/sql-tests/testcases/aggregate
// and modified by Doris.

suite("test_agg_materialize") {
    sql "set enable_nereids_planner=false"
    qt_select """with tb1 as (select * from (select * from (select 1 k1) as t lateral view explode([1,2,3]) tmp1 as e1)t)
                    select count(*) from (select 1, count(*)
                        from tb1
                        where e1 in (1, 2)
                        group by e1
                        union all
                        select 1, count(*)
                        from tb1
                        where e1 = 1)tttt
                    ; """
}
