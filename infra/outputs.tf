output "sns_topic_arn" {
  description = "ARN of the alerts SNS topic"
  value       = aws_sns_topic.alerts.arn
}

output "waf_web_acl_arn" {
  description = "ARN of the WAF Web ACL (pass this to API Gateway association)"
  value       = aws_wafv2_web_acl.api.arn
}

output "cloudwatch_namespace" {
  description = "CloudWatch namespace where mulog metrics are published"
  value       = "PringwaService/${var.environment}"
}

output "api_access_log_group" {
  description = "CloudWatch log group for API Gateway access logs"
  value       = aws_cloudwatch_log_group.api_access_logs.name
}
