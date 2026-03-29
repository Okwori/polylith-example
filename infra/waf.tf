# ---------------------------------------------------------------------------
# WAFv2 — REGIONAL (API Gateway, not CloudFront)
# ---------------------------------------------------------------------------

resource "aws_wafv2_web_acl" "api" {
  name  = "pringwa-service-${var.environment}"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  # Rule 1 — AWS managed common rules (SQLi, XSS, etc.)
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 10

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "pringwa-${var.environment}-common-rules"
      sampled_requests_enabled   = true
    }
  }

  # Rule 2 — AWS managed known bad inputs (log4j, SSRF probes, etc.)
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 20

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "pringwa-${var.environment}-known-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  # Rule 3 — IP-based rate limit
  rule {
    name     = "RateLimitPerIP"
    priority = 30

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.waf_rate_limit
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "pringwa-${var.environment}-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "pringwa-${var.environment}-waf"
    sampled_requests_enabled   = true
  }
}

# Associate WAF with the API Gateway stage
resource "aws_wafv2_web_acl_association" "api_gateway" {
  resource_arn = "arn:aws:apigateway:${var.aws_region}::/apis/${var.api_gateway_id}/stages/${var.api_gateway_stage}"
  web_acl_arn  = aws_wafv2_web_acl.api.arn
}

# ---------------------------------------------------------------------------
# API Gateway stage — throttling + access logging
# X-Ray is not available on HTTP API (v2). Observability is handled by mulog
# CloudWatch publisher. Import the existing stage before applying:
#   terraform import aws_api_gatewayv2_stage.api <api-id>/<stage-name>
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "api_access_logs" {
  name              = "/aws/apigateway/pringwa-service-${var.environment}"
  retention_in_days = 30
}

resource "aws_api_gatewayv2_stage" "api" {
  api_id = var.api_gateway_id
  name   = var.api_gateway_stage

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_access_logs.arn
  }

  default_route_settings {
    detailed_metrics_enabled = true
    throttling_burst_limit   = 500
    throttling_rate_limit    = 1000
  }

  lifecycle {
    ignore_changes = [deployment_id, description, tags]
  }
}
