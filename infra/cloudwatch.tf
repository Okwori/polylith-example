# ---------------------------------------------------------------------------
# Metric Filters
# mulog publishes JSON events. Fields use forward-slash names, so CloudWatch
# filter patterns must use JSON bracket notation: $["http/status"]
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_log_metric_filter" "http_5xx" {
  name           = "pringwa-service-${var.environment}-5xx"
  log_group_name = var.mulog_log_group
  pattern        = "{ $[\"http/status\"] >= 500 }"

  metric_transformation {
    name          = "Http5xxCount"
    namespace     = "PringwaService/${var.environment}"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http_latency" {
  name           = "pringwa-service-${var.environment}-latency"
  log_group_name = var.mulog_log_group
  pattern        = "{ $[\"mulog/event-name\"] = \"com.pringwa.server.request-log/http-request\" }"

  metric_transformation {
    name          = "HttpDurationMs"
    namespace     = "PringwaService/${var.environment}"
    value         = "$[\"http/duration-ms\"]"
    default_value = "0"
    unit          = "Milliseconds"
  }
}

# ---------------------------------------------------------------------------
# Alarms
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "error_rate" {
  alarm_name          = "pringwa-service-${var.environment}-5xx-rate"
  alarm_description   = "More than ${var.alarm_error_rate_threshold} 5xx responses in 5 minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 300
  statistic           = "Sum"
  threshold           = var.alarm_error_rate_threshold
  treat_missing_data  = "notBreaching"

  namespace   = "PringwaService/${var.environment}"
  metric_name = "Http5xxCount"

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "latency_p99" {
  alarm_name          = "pringwa-service-${var.environment}-latency-p99"
  alarm_description   = "p99 latency exceeded ${var.alarm_latency_threshold_ms}ms over 5 minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 300
  extended_statistic  = "p99"
  threshold           = var.alarm_latency_threshold_ms
  treat_missing_data  = "notBreaching"

  namespace   = "PringwaService/${var.environment}"
  metric_name = "HttpDurationMs"

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]
}
