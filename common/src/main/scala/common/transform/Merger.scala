package ma.fin.monitor.common.transform

import org.apache.flink.streaming.api.scala._

/**
 * DataStream[T1] and DataStream[T2] to DataStream[T]
 * 双流join
 * @param source
 * @tparam T1
 * @tparam T2
 * @tparam T
 */
abstract class Merger[T1, T2, T](source: DataStream[T1]) extends Serializable {

  def joinStream(input: DataStream[T2]): DataStream[T] = {
    merge(source, input).name(s"merge_${getName}").uid(s"merger_${getUid}")
  }

  def getName: String

  def getUid: String

  protected def merge(input1: DataStream[T1], input2: DataStream[T2]): DataStream[T]

}
