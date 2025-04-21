package ma.fin.monitor.entity2dim

import ma.fin.monitor.common.entity.financial.FinancialEntity
import ma.fin.monitor.common.entity.monitor.EntityRelation
import ma.fin.monitor.entity2dim.sink._
import org.apache.flink.streaming.api.scala._

/**
 * 隐式转换函数定义
 */
object Step {

// Change these methods to:
implicit def financialInstitutionSink(input: DataStream[FinancialEntity]): Unit = {
  new FinancialInstitutionSink().doSink(input)
}

implicit def individualSink(input: DataStream[FinancialEntity]): Unit = {
  new IndividualSink().doSink(input)
}

implicit def sanctionedEntitySink(input: DataStream[FinancialEntity]): Unit = {
  new SanctionedEntitySink().doSink(input)
}

implicit def entityRelationSink(input: DataStream[EntityRelation]): Unit = {
  new EntityRelationSink().doSink(input)
}
}