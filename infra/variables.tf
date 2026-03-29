variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (staging or prod)"
  type        = string
  validation {
    condition     = contains(["staging", "prod"], var.environment)
    error_message = "environment must be staging or prod"
  }
}

variable "api_gateway_id" {
  description = "ID of the HTTP API Gateway fronting the Ion handler"
  type        = string
}

variable "api_gateway_stage" {
  description = "API Gateway stage name (e.g. $default)"
  type        = string
  default     = "$default"
}

variable "mulog_log_group" {
  description = "CloudWatch Logs log group name where mulog publishes events"
  type        = string
  default     = "/pringwa/service"
}

variable "alert_email" {
  description = "Email address to receive CloudWatch alarm notifications"
  type        = string
}

variable "waf_rate_limit" {
  description = "Maximum requests per 5-minute window per IP before WAF blocks"
  type        = number
  default     = 2000
}

variable "alarm_error_rate_threshold" {
  description = "Number of 5xx responses in 5 minutes that triggers the error-rate alarm"
  type        = number
  default     = 10
}

variable "alarm_latency_threshold_ms" {
  description = "p99 latency (ms) in 5 minutes that triggers the latency alarm"
  type        = number
  default     = 3000
}
