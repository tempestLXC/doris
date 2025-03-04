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

suite("test_clickhouse_jdbc_catalog", "p0,external,clickhouse,external_docker,external_docker_clickhouse") {
    String enabled = context.config.otherConfigs.get("enableJdbcTest")
    if (enabled != null && enabled.equalsIgnoreCase("true")) {
        String catalog_name = "clickhouse_catalog";
        String internal_db_name = "regression_test_jdbc_catalog_p0";
        String ex_db_name = "doris_test";
        String clickhouse_port = context.config.otherConfigs.get("clickhouse_22_port");
        String externalEnvIp = context.config.otherConfigs.get("externalEnvIp")
        String s3_endpoint = getS3Endpoint()
        String bucket = getS3BucketName()
        String driver_url = "https://${bucket}.${s3_endpoint}/regression/jdbc_driver/clickhouse-jdbc-0.4.2-all.jar"

        String inDorisTable = "test_clickhouse_jdbc_doris_in_tb";

        sql """create database if not exists ${internal_db_name}; """

        sql """ drop catalog if exists ${catalog_name} """

        sql """ create catalog if not exists ${catalog_name} properties(
                    "type"="jdbc",
                    "user"="default",
                    "password"="123456",
                    "jdbc_url" = "jdbc:clickhouse://${externalEnvIp}:${clickhouse_port}/doris_test",
                    "driver_url" = "${driver_url}",
                    "driver_class" = "com.clickhouse.jdbc.ClickHouseDriver"
        );"""
        sql """use ${internal_db_name}"""
        sql  """ drop table if exists ${internal_db_name}.${inDorisTable} """
        sql  """
            CREATE TABLE ${internal_db_name}.${inDorisTable} (
                `id` INT NULL COMMENT "主键id",
                `name` string NULL COMMENT "名字",
                `age` INT NULL COMMENT "年龄"
                ) DISTRIBUTED BY HASH(id) BUCKETS 10
                PROPERTIES("replication_num" = "1");
        """

        sql """ switch ${catalog_name} """
        def res_dbs_log = sql "show databases;"
		for(int i = 0;i < res_dbs_log.size();i++) {
			def tbs = sql "show tables from  `${res_dbs_log[i][0]}`"
			log.info( "database = ${res_dbs_log[i][0]} => tables = "+tbs.toString())
		}
        try {
            sql """ use ${ex_db_name} """

            order_qt_type  """ select * from type order by k1; """
            order_qt_type_null  """ select * from type_null order by id; """
            sql """drop table if exists internal.${internal_db_name}.ck_type_null """
            order_qt_ctas_type_null """create table internal.${internal_db_name}.ck_type_null PROPERTIES("replication_num" = "1") as select * from type_null """;
            order_qt_query_ctas_type_null """ select * from internal.${internal_db_name}.ck_type_null order by id; """
            order_qt_number  """ select * from number order by k6; """
            order_qt_arr  """ select * from arr order by id; """
            order_qt_arr_null  """ select * from arr_null order by id; """
            sql """ drop table if exists internal.${internal_db_name}.ck_arr_null"""
            order_qt_ctas_arr_null """create table internal.${internal_db_name}.ck_arr_null PROPERTIES("replication_num" = "1") as select * from arr_null """;
            order_qt_query_ctas_arr_null """ select * from internal.${internal_db_name}.ck_arr_null order by id; """
            sql  """ insert into internal.${internal_db_name}.${inDorisTable} select * from student; """
            order_qt_in_tb  """ select id, name, age from internal.${internal_db_name}.${inDorisTable} order by id; """
            order_qt_system  """ show tables from `system`; """
            order_qt_filter  """ select k1,k2 from type where 1 = 1 order by 1 ; """
            order_qt_filter2  """ select k1,k2 from type where 1 = 1 and  k1 = true order by 1 ; """
            order_qt_filter3  """ select k1,k2 from type where k1 = true order by 1 ; """
            sql "set jdbc_clickhouse_query_final = true;"
            order_qt_final1 """select * from final_test"""
            sql "set jdbc_clickhouse_query_final = false;"
            order_qt_final2 """select * from final_test"""
            order_qt_func_push """select * from ts where from_unixtime(ts,'yyyyMMdd') >= '2022-01-01';"""
            explain {
                sql("select * from ts where from_unixtime(ts,'yyyyMMdd') >= '2022-01-01';")
                contains """QUERY: SELECT "id", "ts" FROM "doris_test"."ts" WHERE (FROM_UNIXTIME(ts, '%Y%m%d') >= '2022-01-01')"""
            }
            explain {
                sql("select * from ts where nvl(ts,null) >= '2022-01-01';")
                contains """QUERY: SELECT "id", "ts" FROM "doris_test"."ts"""
            }
            order_qt_func_push2 """select * from ts where ts <= unix_timestamp(from_unixtime(ts,'yyyyMMdd'));"""
            explain {
                sql("select * from ts where ts <= unix_timestamp(from_unixtime(ts,'yyyy-MM-dd'));")
                contains """QUERY: SELECT "id", "ts" FROM "doris_test"."ts" WHERE ("ts" <= toUnixTimestamp(FROM_UNIXTIME(ts, '%Y-%m-%d')))"""
            }

            order_qt_dt_with_tz """ select * from dt_with_tz order by id; """

        }finally {
			res_dbs_log = sql "show databases;"
			for(int i = 0;i < res_dbs_log.size();i++) {
				def tbs = sql "show tables from  `${res_dbs_log[i][0]}`"
				log.info( "database = ${res_dbs_log[i][0]} => tables = "+tbs.toString())
			}
		}
        
        sql """ drop catalog if exists ${catalog_name} """
    }
}
