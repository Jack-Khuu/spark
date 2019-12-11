// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Create a new Session with the proper settings
val sparkCbo = spark.newSession
import org.apache.spark.sql.internal.SQLConf.CBO_ENABLED
sparkCbo.conf.set(CBO_ENABLED.key, true)
import org.apache.spark.sql.internal.SQLConf.JOIN_REORDER_ENABLED
sparkCbo.conf.set(JOIN_REORDER_ENABLED.key, true)


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Create the tables
val tableNameS = "t1"
val tableNameS2 = "t2"
val tableNameM = "t3"

Seq((0, 0, "zero"), (1, 1, "one")).
  toDF("id", "p1", "p2").
  write.
  saveAsTable(tableNameS)

Seq((0, 0, "A"), (1, 1, "B")).
  toDF("id", "p1", "p2").
  write.
  saveAsTable(tableNameS2)

Seq((0, 0, "Red"), (1, 10, "Blue"), (1, 20, "Green"), (0, 30, "Yellow")).
  toDF("id", "p1", "p2").
  write.
  saveAsTable(tableNameM)

// Insert Nulls
sparkCbo.sql(s"INSERT INTO t1 VALUES(null, null, null)")


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Generate the statistics for all the tables
sparkCbo.sql(s"ANALYZE TABLE $tableNameS COMPUTE STATISTICS FOR ALL COLUMNS")
sparkCbo.sql(s"ANALYZE TABLE $tableNameS2 COMPUTE STATISTICS FOR ALL COLUMNS")
sparkCbo.sql(s"ANALYZE TABLE $tableNameM COMPUTE STATISTICS FOR ALL COLUMNS")

// Example of JoinReordering (The former uses CBO, the latter returns the plan verbatim)
sparkCbo.sql("SELECT * FROM ((t3 AS A JOIN t2 AS B ON A.p1=B.p1) JOIN (t3 AS C JOIN t2 AS D ON C.p1=D.p1) ON A.p1=C.p1) JOIN T1 AS E ON A.p1=E.p1 WHERE E.p1=0").show()
sparkCbo.conf.set(CBO_ENABLED.key, false)
sparkCbo.sql("SELECT * FROM ((t3 AS A JOIN t2 AS B ON A.p1=B.p1) JOIN (t3 AS C JOIN t2 AS D ON C.p1=D.p1) ON A.p1=C.p1) JOIN T1 AS E ON A.p1=E.p1 WHERE E.p1=0").show()
sparkCbo.conf.set(CBO_ENABLED.key, true)


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Show explicit stats on tables
sparkCbo.sql("SELECT * FROM t1").show()
val catalogStats = sparkCbo.sharedState.externalCatalog.getTable("default", "t1").stats.get
val colStatsMap = catalogStats.colStats
val t1_idStats = colStatsMap("id").toMap("id")
val t1_p2Stats = colStatsMap("p2").toMap("p2")

sparkCbo.sql("SELECT * FROM t3").show()
val catalogStats = sparkCbo.sharedState.externalCatalog.getTable("default", "t3").stats.get
val colStatsMap = catalogStats.colStats
val t3_idStats = colStatsMap("id").toMap("id")
val t3_p2Stats = colStatsMap("p2").toMap("p2")


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// MISC test ideas

// val descExtSQL = s"DESC EXTENDED $tableNameS id"
// sql(descExtSQL).show(truncate = false)


// Invalidate cache
// sparkCbo.sql(s"REFRESH TABLE $tableNameS")

// Use ANALYZE TABLE...FOR COLUMNS to compute column statistics
// that saves them in a metastore (aka an external catalog)
// val df = sparkCbo.table(tableNameS)


// import org.apache.spark.sql.catalyst.TableIdentifier
// val sessionCatalog = sparkCbo.sessionState.catalog
// val tid = TableIdentifier(tableNameS)
// sessionCatalog.dropTable(tid, ignoreIfNotExists = true, purge = true)
