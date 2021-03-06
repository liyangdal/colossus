package colossus.metrics

import MetricAddress._
import org.scalatest._

import scala.util.Success

class AggregationSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  "MetricFilter" must {
    "filter metrics" in {
      val t1 = Metric(Root / "foo" / "bar", 4L)
      val t2 = Metric(Root / "foo" / "baz", 5L)

      val metrics: MetricMap = MetricMap(t1, t2)
      val expected = MetricMap(t1)
      val filter = MetricFilter(Root / "foo" / "bar")
      metrics.filter(Seq(filter)) must equal (expected)
    }

    "filter out tags from metric" in {
      val v1 = Map("tag1" -> "value1") -> (4L)
      val v2 = Map("tag1" -> "value2") -> (5L)
      val v3 = Map("tag1" -> "value3") -> (6L)
      val metric = Metric(Root / "foo", Map(v1,v2,v3))
      val expected = Metric(Root / "foo", Map(v1))

      val filter = MetricFilter(Root / "foo", TagSelector(Map("tag1" -> List("value1"))))

      metric.filter(filter.valueFilter) must equal (expected)
    }
  }

  "MetricSelection" must {
    "basic exact match" in {
      val s = Root / "foo" / "bar" / "baz"
      s.matches(s) must equal (true)
      s.matches(Root / "foo" / "bar") must equal (false)
      s.matches(Root / "foo" / "bar" / "baz" / "mooo") must equal (false)
    }

    "trailing wildcard match" in {
      val m = Root / "foo" / "bar" / "baz"
      val s = Root / "foo" / "*"
      s.matches(m) must equal (true)
      s.matches(Root / "bar" / "baz") must equal(false)
    }

    "inside wildcard match" in {
      val m = Root / "foo" / "bar" / "baz"
      val s = Root / "foo" / "*" / "baz"
      s.matches(m) must equal (true)
      s.matches(Root / "foo" / "asfasdf" / "baz") must equal(true)
      s.matches(Root / "foo" / "bar") must equal(false)
      s.matches(Root / "bar" / "baz") must equal(false)
    }

  }

  "TagSelector" must {
    "match tags" in {
      val t = TagSelector(
        Map("foo" -> List("bar", "baz"))
      )
      t.matches(Map("foo" -> "bar")) must equal (true)
      t.matches(Map("foo" -> "ggg")) must equal (false)
      t.matches(Map("rrr" -> "baz")) must equal (false)
    }
    "match tags with wildcard" in {
      val t = TagSelector(
        Map("foo" -> List("*"), "bar" -> List("baz"))
      )
      t.matches(Map("foo" -> "bar", "bar" -> "baz")) must equal (true)
      t.matches(Map("foo" -> "adfsdf", "bar" -> "baz")) must equal (true)
      t.matches(Map("foo" -> "adfsdf", "bar" -> "zzz")) must equal (false)
      t.matches(Map("bar" -> "baz")) must equal (false)

    }
  }

  "MetricValueFilter" must {
    "only keep desired values" in {
      val v1 = Map("foo" -> "bar") -> (4L)
      val v2 = Map("foo" -> "bar", "bar" -> "baz") -> (5L)
      val v3 = Map("foo" -> "moo", "bar" -> "noo") -> (6L)
      val v4 = TagMap.Empty -> (8L)
      val values: ValueMap = Map(v1,v2,v3,v4)
      val expected = Map(v2)

      val filter = MetricValueFilter(Some(TagSelector(Map("foo" -> Seq("*"), "bar" -> Seq("baz")))), None)

      filter.process(values) must equal (expected)
    }
  }

  "MetricFilterParser" must {
    "parse just address" in {
      MetricFilterParser.parseFilter("SELECT /foo/bar") must equal (Success(MetricFilter(Root / "foo" / "bar")))
    }

    "case insensitive keywords" in {
      MetricFilterParser.parseFilter("SelEcT /foo/bar") must equal (Success(MetricFilter(Root / "foo" / "bar")))
    }

    "parse address with filter" in {
      val expected = Success(MetricFilter(Root / "foo" / "bar", TagSelector(Map("tag1" -> List("v1","v2")))))
      MetricFilterParser.parseFilter("SELECT /foo/bar WHERE tag1=v1,v2") must equal (expected)
    }
    "parse address with multiple tag filters" in {
      val expected = Success(MetricFilter(Root / "foo" / "bar", TagSelector(Map("tag1" -> List("v1"), "tag2" -> List("v2")))))
      MetricFilterParser.parseFilter("SELECT /foo/bar WHERE tag1=v1;tag2=v2") must equal (expected)
    }

    "parse filter with group by" in {
      val expected = Success(MetricFilter(Root / "foo" / "bar", MetricValueFilter(None, Some(GroupBy(Set("tag1", "tag2"), AggregationType.Sum)))))
      MetricFilterParser.parseFilter("SELECT /foo/bar GROUP BY tag1,tag2 sum") must equal (expected)
    }

    "parse address with alias" in {
      val expected = Success(MetricFilter(Root / "foo" / "bar", MetricValueFilter.Empty, Some(Root / "cool" / "alias")))
      MetricFilterParser.parseFilter("SELECT /foo/bar AS /cool/alias") must equal (expected)
    }
  }
      

}

